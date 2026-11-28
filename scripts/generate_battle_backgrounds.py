#!/usr/bin/env python3
"""Generate all 64x3 = 192 battle backgrounds from docs/BATTLE_BACKGROUND_PROMPTS.md via ComfyUI."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import io
import json
from pathlib import Path
import re
import time
import urllib.request
import uuid

from PIL import Image

COMFY_URL = "http://127.0.0.1:8188"
CKPT_NAME = "sd_xl_base_1.0.safetensors"
LORA_NAME = "pixel-art-xl.safetensors"

GAME_DIR_MAP = {
    "Wally's World": "wallys-world",
    "Superstore Saga": "superstore-saga",
    "Commander America": "commander-america-rpg",
    "Echoes of the Logos": "echoes-of-logos",
}

SHAPES = (
    (
        "",
        1376,
        768,
        344,
        192,
        "Wide 16:9 battle arena backdrop. The horizon, where the back wall or scenery meets the floor, "
        "is a straight level line at 55% of the image height. The floor fills the bottom 45%: flat, open "
        "and clear from the left edge to the right edge, one even surface. Landmarks sit above the horizon "
        "in the centre and back; props only at the far left and far right edges, behind the floor line.",
    ),
    (
        "_p",
        768,
        1376,
        192,
        344,
        "Tall 9:16 battle arena backdrop of the same place, recomposed vertically. The horizon is a straight "
        "level line at 55% of the image height. The floor fills the bottom 45%: flat, open and clear across "
        "the full width. Use the extra height above the horizon for the place's tall features (ceiling, racks, "
        "towers, sky). Nothing important at the far left or right edges, which may be cropped.",
    ),
    (
        "_s",
        1024,
        1024,
        256,
        256,
        "Square 1:1 battle arena backdrop of the same place. The horizon is a straight level line at 55% of "
        "the image height. The floor fills the bottom 45%: flat, open and clear across the full width. The "
        "place's main landmark sits centred above the horizon, with balanced scenery to either side.",
    ),
)


@dataclass
class BgSpec:
    game_repo: str
    bg_id: str
    scene: str
    portrait_line: str
    style: str
    negative: str


def _clean_blockquote(lines: list[str], start_idx: int) -> str:
    parts: list[str] = []
    i = start_idx
    while i < len(lines) and lines[i].startswith(">"):
        parts.append(lines[i][1:].strip())
        i += 1
    return " ".join(p for p in parts if p)


def parse_prompts_md(md_path: Path) -> list[BgSpec]:
    lines = md_path.read_text(encoding="utf-8").splitlines()
    specs: list[BgSpec] = []
    cur_repo = ""
    cur_style = ""
    cur_neg = ""
    in_bg_table = False

    i = 0
    while i < len(lines):
        line = lines[i]
        m_sec = re.match(r"^##\s+\d+\.\s+(.+)$", line)
        if m_sec:
            heading = m_sec.group(1)
            cur_repo = ""
            for k, v in GAME_DIR_MAP.items():
                if heading.startswith(k):
                    cur_repo = v
                    break
            cur_style = ""
            cur_neg = ""
            in_bg_table = False
            i += 1
            continue

        if cur_repo:
            if line.strip() == "**GAME STYLE:**":
                cur_style = _clean_blockquote(lines, i + 1)
            elif line.strip() == "**GAME NEGATIVE:**":
                cur_neg = _clean_blockquote(lines, i + 1)
            elif line.strip() == "### Backgrounds":
                in_bg_table = True
            elif line.strip().startswith("### ") and line.strip() != "### Backgrounds":
                in_bg_table = False
            elif in_bg_table and line.startswith("| `"):
                cols = [c.strip() for c in line.strip().strip("|").split("|")]
                bg_id = cols[0].strip("`").strip()
                # Echoes has 6 columns (id, Name, Overlay, Theme, SCENE, Portrait); others have 5
                if len(cols) >= 6:
                    scene, portrait = cols[4], cols[5]
                elif len(cols) >= 5:
                    scene, portrait = cols[3], cols[4]
                else:
                    i += 1
                    continue
                specs.append(
                    BgSpec(
                        game_repo=cur_repo,
                        bg_id=bg_id,
                        scene=scene,
                        portrait_line=portrait,
                        style=cur_style,
                        negative=cur_neg,
                    )
                )
        i += 1
    return specs


def _comfy_get(path: str) -> Any:
    with urllib.request.urlopen(f"{COMFY_URL}{path}", timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _comfy_post(path: str, payload: dict[str, Any]) -> Any:
    req = urllib.request.Request(
        f"{COMFY_URL}{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _comfy_generate(pos: str, neg: str, width: int, height: int, seed: int, prefix: str) -> bytes:
    wf = {
        "4": {"class_type": "CheckpointLoaderSimple", "inputs": {"ckpt_name": CKPT_NAME}},
        "10": {
            "class_type": "LoraLoader",
            "inputs": {
                "lora_name": LORA_NAME,
                "model": ["4", 0],
                "clip": ["4", 1],
                "strength_model": 0.85,
                "strength_clip": 0.85,
            },
        },
        "5": {"class_type": "EmptyLatentImage", "inputs": {"batch_size": 1, "width": width, "height": height}},
        "6": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["10", 1], "text": pos}},
        "7": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["10", 1], "text": neg}},
        "3": {
            "class_type": "KSampler",
            "inputs": {
                "cfg": 7.0,
                "denoise": 1.0,
                "latent_image": ["5", 0],
                "model": ["10", 0],
                "negative": ["7", 0],
                "positive": ["6", 0],
                "sampler_name": "euler_ancestral",
                "scheduler": "normal",
                "seed": seed,
                "steps": 24,
            },
        },
        "8": {"class_type": "VAEDecode", "inputs": {"samples": ["3", 0], "vae": ["4", 2]}},
        "9": {"class_type": "SaveImage", "inputs": {"filename_prefix": prefix, "images": ["8", 0]}},
    }
    pid = _comfy_post("/prompt", {"prompt": wf, "client_id": str(uuid.uuid4())})["prompt_id"]
    while True:
        hist = _comfy_get(f"/history/{pid}")
        if pid in hist:
            outputs = hist[pid].get("outputs") or {}
            for node_out in outputs.values():
                for img in node_out.get("images") or []:
                    fn = urllib.parse.quote(img["filename"])
                    sf = urllib.parse.quote(img.get("subfolder", ""))
                    tp = urllib.parse.quote(img.get("type", "output"))
                    with urllib.request.urlopen(f"{COMFY_URL}/view?filename={fn}&subfolder={sf}&type={tp}", timeout=30) as r:
                        return r.read()
            raise RuntimeError(f"ComfyUI finished {pid} with no image output")
        time.sleep(0.4)


def _pixelize_4x(raw_png: bytes, grid_w: int, grid_h: int, out_w: int, out_h: int) -> bytes:
    im = Image.open(io.BytesIO(raw_png)).convert("RGB")
    small = im.resize((grid_w, grid_h), resample=Image.Resampling.NEAREST)
    quant = small.quantize(colors=48, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    final = quant.resize((out_w, out_h), resample=Image.Resampling.NEAREST)
    buf = io.BytesIO()
    final.save(buf, format="PNG", optimize=True)
    return buf.getvalue()


def main() -> int:
    import urllib.parse  # noqa: F401 - used in _comfy_generate

    ap = argparse.ArgumentParser()
    ap.add_argument("--prompts", default="/home/todd/GitHub/voice-rpg-engine/docs/BATTLE_BACKGROUND_PROMPTS.md")
    ap.add_argument("--games-root", default="/home/todd/GitHub")
    ap.add_argument("--half", choices=("all", "laptop", "desktop"), default="all")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--overwrite", action="store_true")
    args = ap.parse_args()

    specs = parse_prompts_md(Path(args.prompts))
    mid = len(specs) // 2
    if args.half == "laptop":
        selected = specs[:mid]
    elif args.half == "desktop":
        selected = specs[mid:]
    else:
        selected = specs

    print(f"Total specs: {len(specs)} | Selected ({args.half}): {len(selected)} backgrounds ({len(selected)*3} PNGs)")
    if args.dry_run:
        for s in selected[:5]:
            print(f"  {s.game_repo}/{s.bg_id}")
        print("  ...")
        for s in selected[-3:]:
            print(f"  {s.game_repo}/{s.bg_id}")
        return 0

    games_root = Path(args.games_root)
    t_start = time.monotonic()
    done = 0
    total_imgs = len(selected) * len(SHAPES)
    for spec in selected:
        out_dir = games_root / spec.game_repo / "app/src/main/assets/environments"
        out_dir.mkdir(parents=True, exist_ok=True)
        base_seed = abs(hash(f"{spec.game_repo}:{spec.bg_id}")) % 900_000_000 + 1000
        for suffix, out_w, out_h, grid_w, grid_h, shape_block in SHAPES:
            done += 1
            out_path = out_dir / f"{spec.bg_id}{suffix}.png"
            if out_path.is_file() and not args.overwrite:
                print(f"[{done}/{total_imgs}] SKIP {spec.game_repo}/{out_path.name}", flush=True)
                continue
            scene_part = f"{spec.scene} {spec.portrait_line}" if suffix == "_p" else spec.scene
            pos = f"{spec.style} {scene_part} {shape_block}"
            neg = spec.negative
            t0 = time.monotonic()
            raw = _comfy_generate(pos, neg, out_w, out_h, base_seed, f"bg_{spec.game_repo}_{spec.bg_id}{suffix}")
            pix = _pixelize_4x(raw, grid_w, grid_h, out_w, out_h)
            out_path.write_bytes(pix)
            dt = time.monotonic() - t0
            print(f"[{done}/{total_imgs}] OK {spec.game_repo}/{out_path.name} ({dt:.1f}s)", flush=True)

    elapsed = time.monotonic() - t_start
    print(f"FINISHED {args.half}: {total_imgs} PNGs in {elapsed:.1f}s ({elapsed/60:.1f}m)", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
