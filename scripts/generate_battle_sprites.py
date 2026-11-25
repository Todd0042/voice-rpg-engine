#!/usr/bin/env python3
"""Generate 64x64 / 96x96 / 128x128 Aseprite battle sprite sheets using FLUX.2 Klein + WanFirstLastFrameToVideo.

Pipeline per character:
  1. Generate a unified 4x2 key-pose sheet via FLUX.2 Klein on flat #FF00FF magenta.
  2. Animate each tag via Wan 2.2-14B WanFirstLastFrameToVideo (start_image + end_image).
  3. Key + despill magenta, quantize to a shared 24-color palette, anchor to `feet`,
     and export Aseprite <spriteId>.png + <spriteId>.json matching docs/SPRITE_PLAN.md.

Example:
  ~/storage/graphical-studio/.venv/bin/python scripts/generate_battle_sprites.py \\
    --cast echoes_party --id aethel --game echoes-of-logos
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw


@dataclass(frozen=True)
class TransitionSpec:
    tag: str
    start_idx: int
    end_idx: int
    pick_frames: tuple[int, ...]
    duration_ms: int
    prompt: str
    negative: str
    event: str = ""  # e.g. "release=4" or "impact=4"


PARTY_TRANSITIONS: list[TransitionSpec] = [
    TransitionSpec(
        "idle", 0, 0, (0, 5, 10, 14), 190,
        "16-bit SNES JRPG pixel art fighter subtle idle breathing stance, gentle chest rise and cloak sway, static feet, flat #FF00FF magenta background, static camera",
        "walking, running, turning, camera movement, blurry, extra limbs",
    ),
    TransitionSpec(
        "ready", 1, 1, (0, 5, 10, 14), 160,
        "16-bit SNES JRPG pixel art fighter alert combat-ready breathing stance, weapon held steady with slight rhythmic bounce, flat #FF00FF magenta background, static camera",
        "walking, turning, camera movement, blurry",
    ),
    TransitionSpec(
        "charge", 2, 2, (0, 4, 8, 12), 120,
        "16-bit SNES JRPG pixel art fighter channeling intense magic energy, swirling cloak and pulsing glowing aura around raised weapon, flat #FF00FF magenta background, static camera",
        "camera movement, blurry, fading out",
    ),
    TransitionSpec(
        "cast", 0, 3, (0, 4, 8, 12, 16), 110,
        "16-bit SNES JRPG pixel art fighter stepping forward into a dramatic spell-release lunge, thrusting weapon forward with bright magic burst, fluid articulation, flat #FF00FF magenta background, static camera",
        "stiff board, camera movement, blurry, distorted limbs",
        event="release=4",
    ),
    TransitionSpec(
        "attack", 0, 4, (0, 4, 8, 12, 16), 100,
        "16-bit SNES JRPG pixel art fighter winding up and leaping forward into a powerful dynamic weapon strike, expressive smear motion, flat #FF00FF magenta background, static camera",
        "stiff board, camera movement, blurry, distorted limbs",
        event="impact=4",
    ),
    TransitionSpec(
        "hurt", 0, 5, (0, 5, 10, 16), 110,
        "16-bit SNES JRPG pixel art fighter recoiling sharply backward from a heavy impact, knees bending and arms flinching, flat #FF00FF magenta background, static camera",
        "stiff board, camera movement, blurry",
    ),
    TransitionSpec(
        "ko", 0, 6, (0, 3, 6, 9, 12, 16), 130,
        "16-bit SNES JRPG pixel art fighter taking a heavy blow, knees buckling and dropping to knees first with cloak billowing before collapsing dramatically onto the ground into KO pose, flat #FF00FF magenta background, static camera",
        "stiff board, rigid tipping, camera movement, blurry, distorted limbs",
    ),
    TransitionSpec(
        "victory", 7, 7, (0, 5, 10, 14), 180,
        "16-bit SNES JRPG pixel art fighter forward-facing triumphant victory pose, gentle celebratory bounce and glowing weapon shimmer, flat #FF00FF magenta background, static camera",
        "walking away, camera movement, blurry",
    ),
]

ENEMY_TRANSITIONS: list[TransitionSpec] = [
    TransitionSpec("idle", 0, 0, (0, 5, 10, 14), 190, "16-bit pixel art monster subtle breathing idle loop, flat #FF00FF magenta background, static camera", "camera movement, blurry"),
    TransitionSpec("windup", 0, 1, (0, 5, 10, 14), 140, "16-bit pixel art monster coiling back in a menacing telegraph windup, flat #FF00FF magenta background, static camera", "camera movement, blurry"),
    TransitionSpec("attack", 1, 2, (0, 4, 8, 12, 16), 100, "16-bit pixel art monster lunging forward in a fierce physical strike, flat #FF00FF magenta background, static camera", "stiff board, camera movement, blurry", event="impact=4"),
    TransitionSpec("cast", 0, 3, (0, 4, 8, 12, 16), 110, "16-bit pixel art monster unleashing a glowing dark magic spell burst forward, flat #FF00FF magenta background, static camera", "stiff board, camera movement, blurry", event="release=4"),
    TransitionSpec("hurt", 0, 4, (0, 5, 10, 16), 110, "16-bit pixel art monster flinching and staggering backward in pain, flat #FF00FF magenta background, static camera", "camera movement, blurry"),
    TransitionSpec("death", 0, 5, (0, 3, 6, 9, 12, 16), 120, "16-bit pixel art monster buckling and collapsing dramatically onto the ground defeated, flat #FF00FF magenta background, static camera", "stiff board, camera movement, blurry"),
]

BOSS_EXTRA_TRANSITIONS: list[TransitionSpec] = [
    TransitionSpec("charge_ultimate", 6, 6, (0, 4, 8, 12), 120, "16-bit pixel art boss monster gathering overwhelming elemental aura, pulsing energy, flat #FF00FF magenta background, static camera", "camera movement, blurry"),
    TransitionSpec("ultimate", 6, 7, (0, 4, 8, 12, 16), 110, "16-bit pixel art boss monster unleashing a massive devastating ultimate attack wave forward, flat #FF00FF magenta background, static camera", "stiff board, camera movement, blurry", event="release=4"),
]

BUILTIN_CAST: dict[str, dict[str, Any]] = {
    "hero": {
        "role": "party",
        "cell": 64,
        "desc": "Aethel, young heroic invocator mage with spiky silver-white hair, blue eyes, royal blue high-collared cloak with gold rune trim, gold-blue chest clasp, ornate staff topped with a glowing cyan crystal",
        "flip_raw": [0, 1, 5],
    },
    "aethel": {
        "role": "party",
        "cell": 64,
        "desc": "Aethel, young heroic invocator mage with spiky silver-white hair, blue eyes, royal blue high-collared cloak with gold rune trim, gold-blue chest clasp, ornate staff topped with a glowing cyan crystal",
        "flip_raw": [0, 1, 5],
    },
    "cedric": {
        "role": "party",
        "cell": 64,
        "desc": "Sir Cedric, noble veteran paladin knight in gleaming silver-and-cobalt plate armor, crimson surcoat, kite shield and glowing broadsword",
        "flip_raw": [],
    },
    "lyra": {
        "role": "party",
        "cell": 64,
        "desc": "Lyra the Grove Warden, agile emerald-cloaked ranger-druid with auburn braided hair, carved oak longbow and verdant spirit-leaf brooch",
        "flip_raw": [],
    },
    "zephyr": {
        "role": "party",
        "cell": 64,
        "desc": "Zephyr, sleek hooded tempest rogue in indigo-and-teal scarf and light leather brigandine, wielding twin curved wind-daggers",
        "flip_raw": [],
    },
    "malakor": {
        "role": "boss",
        "cell": 128,
        "desc": "Malakor the Void Sovereign, towering armored dark sorcerer in obsidian horned mantle and violet-glowing runes, wielding a colossal void halberd",
        "flip_raw": [],
    },
    "template_brute": {
        "role": "enemy",
        "cell": 64,
        "desc": "Heavy ironclad vanguard brute warrior in dark spiked steel armor, broad pauldrons, wielding a massive battleaxe",
        "flip_raw": [],
    },
    "template_sniper": {
        "role": "enemy",
        "cell": 64,
        "desc": "Hooded shadow-cloak marksman stalker in dark slate leather armor, wielding a rune-etched arbalest crossbow",
        "flip_raw": [],
    },
    "template_caster": {
        "role": "enemy",
        "cell": 64,
        "desc": "Sinister crimson-and-obsidian robe warlock acolyte with glowing violet eyes and twisted bone-crystal staff",
        "flip_raw": [],
    },
}


def unload_ollama_models() -> None:
    try:
        ps = json.loads(urllib.request.urlopen("http://127.0.0.1:11434/api/ps", timeout=2).read())
        for m in ps.get("models", []):
            name = m.get("name")
            req = urllib.request.Request(
                "http://127.0.0.1:11434/api/generate",
                data=json.dumps({"model": name, "keep_alive": 0}).encode(),
                headers={"Content-Type": "application/json"},
            )
            urllib.request.urlopen(req, timeout=5).read()
            print(f"[VRAM] Unloaded Ollama model: {name}", flush=True)
    except Exception:
        pass


def key_and_despill_magenta(img: Image.Image) -> Image.Image:
    a = np.asarray(img.convert("RGB")).astype(np.int16)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mag = (r > 110) & (b > 110) & (g < 135) & ((r + b) - 2 * g > 65)
    alpha = np.where(mag, 0, 255).astype(np.uint8)
    rgb = a.copy()
    edge_pink = (~mag) & ((r + b) - 2 * g > 40) & (r > g) & (b > g)
    rgb[edge_pink, 0] = np.minimum(rgb[edge_pink, 0], rgb[edge_pink, 1] + 10)
    rgb[edge_pink, 2] = np.minimum(rgb[edge_pink, 2], rgb[edge_pink, 1] + 10)
    return Image.fromarray(np.dstack([rgb.astype(np.uint8), alpha]), "RGBA")


def build_klein_prompt(desc: str, role: str) -> str:
    if role == "party":
        poses = (
            "Top row left to right: 1 IDLE in 1/4 right-facing 3/4 side view holding weapon upright, "
            "2 READY in 1/4 right-facing combat stance, 3 CHARGE full right-facing profile raising weapon with glowing aura, "
            "4 CAST full right-facing profile lunging right with magic burst. "
            "Bottom row left to right: 5 ATTACK full right-facing leaping strike forward, "
            "6 HURT full right-facing staggering backward, 7 KO fallen collapsed on ground facing right, "
            "8 VICTORY facing forward toward the camera raising weapon in celebration."
        )
    elif role == "boss":
        poses = (
            "Top row left to right: 1 IDLE facing right, 2 WINDUP coiling back, 3 ATTACK fierce strike right, "
            "4 CAST dark spell burst right. Bottom row left to right: 5 HURT recoil, 6 DEATH collapsed on ground, "
            "7 CHARGE_ULTIMATE gathering overwhelming aura, 8 ULTIMATE unleashing massive attack wave."
        )
    else:
        poses = (
            "Top row left to right: 1 IDLE facing right, 2 WINDUP telegraph coil, 3 ATTACK fierce lunge right, "
            "4 CAST spell burst right. Bottom row left to right: 5 HURT staggering recoil, 6 DEATH collapsed on ground, "
            "7 TAUNT menacing roar, 8 GUARD braced defense."
        )
    return (
        f"16-bit SNES JRPG battle sprite sheet like Final Fantasy VI on a completely flat solid #FF00FF magenta background, "
        f"NO grid lines, NO text, NO borders. 4 columns by 2 rows (8 separate well-spaced poses). "
        f"Character: {desc}. {poses} Crisp dark pixel outlines, consistent outfit and proportions."
    )


def extract_cell_to_256(raw: Image.Image, cell_idx: int, flip: bool, floor_y: int = 236) -> Image.Image:
    cw, ch = raw.width // 4, raw.height // 2
    col, row = cell_idx % 4, cell_idx // 4
    cell = raw.crop((col * cw, row * ch, (col + 1) * cw, row * ch + min(ch, 205)))
    if flip:
        cell = cell.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    rgba = key_and_despill_magenta(cell)
    bbox = rgba.getchannel("A").getbbox()
    crop = rgba.crop(bbox) if bbox else rgba
    canvas = Image.new("RGBA", (256, 256), (255, 0, 255, 255))
    x = (256 - crop.width) // 2
    y = floor_y - crop.height
    canvas.alpha_composite(crop, (x, y))
    return canvas.convert("RGB")


def pack_aseprite_sheet(
    sprite_id: str,
    cell_sz: int,
    tagged_images: list[tuple[str, str, int, list[Image.Image]]],
    out_dir: Path,
) -> tuple[Path, Path, Path]:
    all_imgs = [im for _, _, _, frames in tagged_images for im in frames]
    # Sample shared 24-color palette across all frames
    collage = Image.new("RGB", (cell_sz * len(all_imgs), cell_sz), (0, 0, 0))
    for i, im in enumerate(all_imgs):
        collage.paste(im.resize((cell_sz, cell_sz), Image.Resampling.BOX).convert("RGB"), (i * cell_sz, 0))
    palette = collage.quantize(colors=24, method=Image.Quantize.MEDIANCUT)

    total_frames = len(all_imgs)
    sheet = Image.new("RGBA", (cell_sz * total_frames, cell_sz), (0, 0, 0, 0))
    frames_json: list[dict[str, Any]] = []
    tags_json: list[dict[str, Any]] = []

    cursor = 0
    for tag, event, dur, frames in tagged_images:
        start_idx = cursor
        for im in frames:
            rgba = key_and_despill_magenta(im)
            small = rgba.resize((cell_sz, cell_sz), Image.Resampling.BOX)
            a = np.asarray(small)
            opaque = a[..., 3] >= 128
            q = np.asarray(Image.fromarray(a[..., :3].copy()).quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB"))
            out = np.zeros((cell_sz, cell_sz, 4), np.uint8)
            out[..., :3] = q
            out[..., 3] = np.where(opaque, 255, 0)
            sheet.paste(Image.fromarray(out, "RGBA"), (cursor * cell_sz, 0))
            frames_json.append({
                "filename": f"{sprite_id}_{tag}_{cursor - start_idx + 1}.png",
                "frame": {"x": cursor * cell_sz, "y": 0, "w": cell_sz, "h": cell_sz},
                "rotated": False,
                "trimmed": False,
                "spriteSourceSize": {"x": 0, "y": 0, "w": cell_sz, "h": cell_sz},
                "sourceSize": {"w": cell_sz, "h": cell_sz},
                "duration": dur,
            })
            cursor += 1
        tag_entry: dict[str, Any] = {"name": tag, "from": start_idx, "to": cursor - 1, "direction": "forward"}
        if event:
            tag_entry["data"] = event
        tags_json.append(tag_entry)

    out_dir.mkdir(parents=True, exist_ok=True)
    png_path = out_dir / f"{sprite_id}.png"
    json_path = out_dir / f"{sprite_id}.json"
    sheet.save(png_path)
    with json_path.open("w", encoding="utf-8") as fh:
        json.dump(
            {
                "frames": frames_json,
                "meta": {
                    "app": "voice-rpg-engine/generate_battle_sprites",
                    "version": "1.0",
                    "image": f"{sprite_id}.png",
                    "size": {"w": cell_sz * total_frames, "h": cell_sz},
                    "scale": "1",
                    "frameTags": tags_json,
                    "slices": [
                        {"name": "feet", "keys": [{"frame": 0, "bounds": {"x": cell_sz // 4, "y": cell_sz - 4, "w": cell_sz // 2, "h": 4}}]},
                        {"name": "hit", "keys": [{"frame": 0, "bounds": {"x": cell_sz // 4, "y": cell_sz // 3, "w": cell_sz // 2, "h": cell_sz // 3}}]},
                        {"name": "cast", "keys": [{"frame": 0, "bounds": {"x": (cell_sz * 3) // 4, "y": cell_sz // 3, "w": 6, "h": 6}}]},
                        {"name": "head", "keys": [{"frame": 0, "bounds": {"x": cell_sz // 3, "y": cell_sz // 8, "w": cell_sz // 3, "h": 6}}]},
                    ],
                },
            },
            fh,
            indent=2,
        )

    # Preview strip (2x for bosses, 4x for 64x64)
    scale_x = 2 if cell_sz >= 96 else 4
    prev = Image.new("RGBA", (sheet.width * scale_x, cell_sz * scale_x + 20), (18, 22, 36, 255))
    draw = ImageDraw.Draw(prev)
    draw.line([(0, (cell_sz - 4) * scale_x), (prev.width, (cell_sz - 4) * scale_x)], fill=(70, 90, 120, 255), width=2)
    prev.alpha_composite(sheet.resize((sheet.width * scale_x, sheet.height * scale_x), Image.Resampling.NEAREST), (0, 0))
    prev_path = out_dir / f"{sprite_id}_preview.png"
    prev.save(prev_path)
    return png_path, json_path, prev_path


def main() -> None:
    p = argparse.ArgumentParser(description="Generate animated Aseprite battle sprite sheets via Klein + Wan FLF.")
    p.add_argument("--id", required=True, help="Sprite ID (e.g. aethel, cedric, lyra, zephyr, malakor)")
    p.add_argument("--role", choices=["party", "enemy", "boss"], default="")
    p.add_argument("--cell", type=int, default=0, help="Cell size (64, 96, or 128)")
    p.add_argument("--desc", default="", help="Character visual description")
    p.add_argument("--game", default="", help="Sibling game folder name (writes to <game>/app/src/main/assets/game/sprites/sheets)")
    p.add_argument("--out-dir", default="", help="Explicit output directory")
    p.add_argument("--studio-dir", default=os.path.expanduser("~/storage/graphical-studio"))
    p.add_argument("--work-dir", default="/tmp/battle_sprites_work")
    p.add_argument("--raw-sheet", default="", help="Reuse an existing 4x2 raw sheet PNG instead of running Klein")
    p.add_argument("--seed", type=int, default=108)
    p.add_argument("--skip-wan", action="store_true", help="Skip Wan interpolation (pack static key poses only)")
    p.add_argument("--dry-run", action="store_true", help="Print plan without running GPU models")
    args = p.parse_args()

    preset = BUILTIN_CAST.get(args.id, {})
    role = args.role or preset.get("role", "party")
    cell = args.cell or preset.get("cell", 128 if role == "boss" else 64)
    desc = args.desc or preset.get("desc", f"fantasy {role} character {args.id}")
    flip_raw = set(preset.get("flip_raw", []))

    engine_root = Path(__file__).resolve().parent.parent
    out_dir = Path(args.out_dir) if args.out_dir else (
        engine_root.parent / args.game / "app" / "src" / "main" / "assets" / "game" / "sprites" / "sheets"
        if args.game else engine_root / "assets" / "game" / "sprites" / "sheets"
    )
    work_dir = Path(args.work_dir) / args.id
    work_dir.mkdir(parents=True, exist_ok=True)
    raw_path = Path(args.raw_sheet) if args.raw_sheet else work_dir / f"{args.id}_raw.png"

    transitions = list(PARTY_TRANSITIONS if role == "party" else ENEMY_TRANSITIONS)
    if role == "boss":
        transitions.extend(BOSS_EXTRA_TRANSITIONS)

    if args.dry_run:
        print(json.dumps({
            "id": args.id,
            "role": role,
            "cell": cell,
            "outDir": str(out_dir),
            "rawSheet": str(raw_path),
            "skipWan": args.skip_wan,
            "transitions": [t.tag for t in transitions],
        }, indent=2))
        return

    studio_dir = str(Path(args.studio_dir).resolve())
    if studio_dir not in sys.path:
        sys.path.insert(0, studio_dir)

    if not raw_path.is_file():
        import subprocess
        cmd = [
            sys.executable, str(Path(studio_dir) / "generate_asset.py"),
            "--model", "flux2_klein", "--width", "1024", "--height", "512",
            "--steps", "4", "--cfg", "1.0", "--seed", str(args.seed),
            "--output", str(raw_path),
            "--prompt", build_klein_prompt(desc, role),
        ]
        print(f"[Klein] Generating raw 4x2 sheet -> {raw_path}", flush=True)
        subprocess.run(cmd, cwd=studio_dir, check=True)

    raw_img = Image.open(raw_path).convert("RGB")
    pose_256 = [extract_cell_to_256(raw_img, i, i in flip_raw) for i in range(8)]
    pose_paths = []
    for i, im in enumerate(pose_256):
        pp = work_dir / f"pose_{i}.png"
        im.save(pp)
        pose_paths.append(pp)

    tagged_frames: list[tuple[str, str, int, list[Image.Image]]] = []
    if args.skip_wan:
        for t in transitions:
            tagged_frames.append((t.tag, t.event, t.duration_ms, [pose_256[t.end_idx]]))
    else:
        import gc as pygc
        unload_ollama_models()
        import torch
        import generate_cutscene as gc
        sys.argv = [sys.argv[0], "--lowvram", "--reserve-vram", "3.2", *gc.speed_flags("2.2-14b")]
        if gc.COMFY_DIR not in sys.path:
            sys.path.insert(0, gc.COMFY_DIR)
        os.chdir(gc.COMFY_DIR)
        import comfy.options
        comfy.options.enable_args_parsing()
        import nodes
        from comfy_extras.nodes_model_advanced import ModelSamplingSD3
        from comfy_extras.nodes_wan import WanFirstLastFrameToVideo

        clip = nodes.CLIPLoader().load_clip("umt5_xxl_fp8_e4m3fn_scaled.safetensors", type="wan", device="default")[0]
        encoded_conds = [
            (
                nodes.CLIPTextEncode().encode(clip, t.prompt)[0],
                nodes.CLIPTextEncode().encode(clip, t.negative)[0],
            )
            for t in transitions
        ]
        del clip
        pygc.collect()
        gc.free_vram()

        vae = nodes.VAELoader().load_vae("wan_2.1_vae.safetensors")[0]
        loader = gc._gguf_loader()

        for t, (pos, neg) in zip(transitions, encoded_conds):
            print(f"[Wan FLF] {args.id}:{t.tag} ({t.start_idx}->{t.end_idx})...", flush=True)
            s_img = gc.load_start_image(str(pose_paths[t.start_idx]), "2.2-14b")
            e_img = gc.load_start_image(str(pose_paths[t.end_idx]), "2.2-14b")
            pos, neg, latent = gc.unwrap_all(
                WanFirstLastFrameToVideo.execute(
                    positive=pos, negative=neg, vae=vae, width=256, height=256, length=17,
                    batch_size=1, start_image=s_img, end_image=e_img,
                )
            )
            del s_img, e_img
            gc.free_vram()
            for noise, st, en, add_n, left in (("High", 0, 2, "enable", "enable"), ("Low", 2, 10000, "disable", "disable")):
                model = loader.load_unet(f"Wan2.2-I2V-A14B-{noise}Noise-Q4_K_S.gguf")[0]
                model = nodes.LoraLoaderModelOnly().load_lora_model_only(model, f"wan2.2_i2v_lightning_4step_{noise.lower()}_noise.safetensors", 1.0)[0]
                model = ModelSamplingSD3().patch(model, shift=5.0)[0]
                with torch.inference_mode():
                    latent = nodes.KSamplerAdvanced().sample(
                        model=model, add_noise=add_n, noise_seed=args.seed, steps=4, cfg=1.0,
                        sampler_name="euler", scheduler="simple", positive=pos, negative=neg,
                        latent_image=latent, start_at_step=st, end_at_step=en, return_with_leftover_noise=left,
                    )[0]
                del model
                pygc.collect()
                gc.free_vram()
            with torch.inference_mode():
                imgs = nodes.VAEDecodeTiled().decode(vae=vae, samples=latent, tile_size=256, overlap=64, temporal_size=32, temporal_overlap=8)[0]
            arr = (np.clip(imgs.detach().cpu().numpy(), 0.0, 1.0) * 255.0).round().astype(np.uint8)
            tagged_frames.append((t.tag, t.event, t.duration_ms, [Image.fromarray(arr[idx]) for idx in t.pick_frames]))
            del imgs, latent, arr, pos, neg
            pygc.collect()
            gc.free_vram()

    png_path, json_path, prev_path = pack_aseprite_sheet(args.id, cell, tagged_frames, out_dir)
    print(f"[Done] {png_path} | {json_path} | {prev_path}", flush=True)


if __name__ == "__main__":
    main()
