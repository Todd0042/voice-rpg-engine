#!/usr/bin/env python3
"""Generate consistent Square (_s) -> Landscape + Portrait (_p) story backgrounds via Desktop ComfyUI (FLUX.2 Klein 4B).

For every story background missing any orientation variant (.jpg, _p.jpg, _s.jpg),
this script regenerates all 3 variants using Desktop's ComfyUI server (192.168.1.236:8188):
  1. Generates Square (_s.jpg, 1024x1024) first.
  2. Feeds Square's sampled latent into ReferenceLatent to generate Landscape (.jpg, 1376x768)
     and Portrait (_p.jpg, 720x1280) with matching room layout, lighting, and palette.
  3. Applies 2x pixelation (cell=2, colors=48) to superstore-saga, commander-america-rpg,
     echoes-of-logos, and voice-rpg-engine; keeps wallys-world un-pixelated (cell=1).
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.parse
import urllib.request
import uuid

from PIL import Image

GITHUB_ROOT = Path("/home/todd/GitHub")
DESKTOP_SSH = "todd@192.168.1.236"
KLEIN_UNET = "flux-2-klein-4b-Q8_0.gguf"
KLEIN_TE = "qwen_3_4b.safetensors"
KLEIN_VAE = "flux2-vae.safetensors"


@dataclass
class StoryBgJob:
    game: str
    stem: str
    prompt_s: str
    prompt_l: str
    prompt_p: str
    pixelate: int
    colors: int

    @property
    def story_dir(self) -> Path:
        return GITHUB_ROOT / self.game / "app" / "src" / "main" / "assets" / "story"

    @property
    def path_s(self) -> Path:
        return self.story_dir / f"{self.stem}_s.jpg"

    @property
    def path_l(self) -> Path:
        return self.story_dir / f"{self.stem}.jpg"

    @property
    def path_p(self) -> Path:
        return self.story_dir / f"{self.stem}_p.jpg"

    def seed(self, base: int = 72026) -> int:
        h = int(hashlib.sha256(f"{self.game}/{self.stem}".encode()).hexdigest()[:8], 16)
        return base + (h % 1_000_000)


def _strip_neg_and_flags(text: str) -> str:
    text = re.sub(r"\s*Negative\s*/\s*Exclude:.*$", "", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"\s*--ar\s+\d+:\d+\.?", "", text)
    text = re.sub(r"\s*--style\s+\S+", "", text)
    text = text.replace("**", "")
    return " ".join(text.split()).strip(" ,.")


def _adapt_for_aspect(prompt: str, aspect: str) -> str:
    p = _strip_neg_and_flags(prompt)
    if aspect == "square":
        p = re.sub(r"\bpanoramic 16:9 view\b", "balanced 1:1 square view", p, flags=re.IGNORECASE)
        p = re.sub(r"\bwide[- ]angle\b", "balanced square", p, flags=re.IGNORECASE)
        p = re.sub(r"\bwide establishing shot\b", "balanced square establishing shot", p, flags=re.IGNORECASE)
        return f"{p}, balanced 1:1 square composition matching the exact same location, lighting, and palette"
    if aspect == "portrait":
        p = re.sub(r"\bpanoramic 16:9 view\b", "vertical 9:16 portrait view", p, flags=re.IGNORECASE)
        p = re.sub(r"\bwide[- ]angle\b", "vertical portrait", p, flags=re.IGNORECASE)
        p = re.sub(r"\bwide establishing shot\b", "vertical portrait establishing shot", p, flags=re.IGNORECASE)
        return f"{p}, vertical 9:16 portrait framing, matching the exact same location, lighting, and palette as the reference"
    return f"{p}, wide 16:9 landscape framing, matching the exact same location, lighting, and palette as the reference"


def _load_superstore_jobs() -> list[StoryBgJob]:
    md = (GITHUB_ROOT / "superstore-saga" / "IMAGE_GENERATION_PROMPTS.md").read_text(encoding="utf-8")
    sections = re.split(r"^###\s+", md, flags=re.MULTILINE)
    by_stem: dict[str, dict[str, str]] = {}
    for sec in sections[1:]:
        m_fn = re.search(r"Target Filename:\*\*\s*`app/src/main/assets/story/([^`]+)\.jpg`", sec)
        m_ar = re.search(r"Aspect Ratio:\*\*\s*`([^`]+)`", sec)
        m_txt = re.search(r"```text\s*\n(.*?)\n```", sec, flags=re.DOTALL)
        if not (m_fn and m_ar and m_txt):
            continue
        raw_name = m_fn.group(1)
        stem = raw_name[:-2] if raw_name.endswith("_p") else raw_name
        ar = m_ar.group(1)
        prompt = _strip_neg_and_flags(m_txt.group(1))
        bucket = by_stem.setdefault(stem, {})
        if "1:1" in ar:
            bucket["s"] = prompt
        elif "9:16" in ar:
            bucket["p"] = prompt
        else:
            bucket["l"] = prompt

    by_stem["bg_security_office"] = {
        "s": (
            "16-bit retro JRPG environment background of a cramped, windowless big-box store Loss Prevention "
            "security office and CCTV interrogation room, centered square 1:1 composition. A steel desk with a "
            "metal folding chair faces a glowing bank of grainy CRT and LCD security camera monitors showing store "
            "aisles, gray cinderblock walls, a corkboard with shoplifter photos, scuffed linoleum floor, harsh "
            "cold overhead fluorescent tube light"
        ),
    }
    by_stem["bg_manager_office"] = {
        "s": (
            "16-bit retro JRPG environment background of a big-box store General Manager's elevated executive "
            "office overlooking the retail sales floor through a wide glass window, centered square 1:1 composition. "
            "Polished laminate desk with aluminum clipboards, stacked KPI binders, leather swivel chair, corporate "
            "motivational plaques on beige drywall, harsh commercial fluorescent lighting"
        ),
    }

    order = [
        "bg_title_storefront",
        "bg_hiring_kiosk",
        "bg_locker_room",
        "bg_breakroom",
        "bg_action_alley",
        "bg_checkout_lanes",
        "bg_loading_dock",
        "bg_security_office",
        "bg_manager_office",
    ]
    jobs: list[StoryBgJob] = []
    for stem in order:
        b = by_stem[stem]
        base_s = b.get("s") or _adapt_for_aspect(b["l"], "square")
        base_l = b.get("l") or _adapt_for_aspect(base_s, "landscape")
        base_p = b.get("p") or _adapt_for_aspect(base_s, "portrait")
        jobs.append(
            StoryBgJob(
                game="superstore-saga",
                stem=stem,
                prompt_s=_adapt_for_aspect(base_s, "square"),
                prompt_l=_adapt_for_aspect(base_l, "landscape"),
                prompt_p=_adapt_for_aspect(base_p, "portrait"),
                pixelate=2,
                colors=48,
            )
        )
    return jobs


def _load_wallys_world_jobs() -> list[StoryBgJob]:
    md = (GITHUB_ROOT / "wallys-world" / "project-guide" / "image_prompts.md").read_text(encoding="utf-8")
    style = (
        "Gritty but warm editorial photography, muted early-2000s American retail toning, slightly "
        "desaturated mustard-and-steel palette, soft fluorescent overhead light, subtle film grain, "
        "photorealistic 35mm, no real logos, no barcodes, no characters in foreground focus unless named, "
    )
    pattern = re.compile(
        r"\*\*File:\*\*\s*`app/src/main/assets/story/([^`]+)\.jpg`[^\n]*\n(?:[^\n>]*\n)*?\n((?:>\s*.*\n?)+)",
        re.MULTILINE,
    )
    landscapes: dict[str, str] = {}
    portraits: dict[str, str] = {}
    for m in pattern.finditer(md):
        fname = m.group(1)
        body = " ".join(line.lstrip("> ").strip() for line in m.group(2).splitlines())
        body = _strip_neg_and_flags(body)
        if fname.endswith("_p"):
            portraits[fname[:-2]] = body
        else:
            landscapes[fname] = body

    jobs: list[StoryBgJob] = []
    for stem, l_prompt in landscapes.items():
        p_prompt = portraits.get(stem, l_prompt)
        jobs.append(
            StoryBgJob(
                game="wallys-world",
                stem=stem,
                prompt_s=_adapt_for_aspect(style + l_prompt, "square"),
                prompt_l=_adapt_for_aspect(style + l_prompt, "landscape"),
                prompt_p=_adapt_for_aspect(style + p_prompt, "portrait"),
                pixelate=1,
                colors=0,
            )
        )
    return jobs


def _load_echoes_jobs() -> list[StoryBgJob]:
    md = (GITHUB_ROOT / "echoes-of-logos" / "STORY_BACKGROUND_PROMPTS.md").read_text(encoding="utf-8")
    scenes = re.split(r"^####\s+Scene\s+\d+:", md, flags=re.MULTILINE)[1:]
    jobs: list[StoryBgJob] = []
    for sec in scenes:
        m_l = re.search(r"Landscape\s*\(`story/([^`]+)\.jpg`\)\*\*:\s*\n\s*>\s*\*\*Prompt\*\*:\s*`([^`]+)`", sec)
        m_p = re.search(r"Portrait\s*\(`story/([^`]+)_p\.jpg`\)\*\*:\s*\n\s*>\s*\*\*Prompt\*\*:\s*`([^`]+)`", sec)
        if not m_l:
            continue
        stem = m_l.group(1)
        l_prompt = _strip_neg_and_flags(m_l.group(2))
        p_prompt = _strip_neg_and_flags(m_p.group(2)) if m_p else l_prompt
        jobs.append(
            StoryBgJob(
                game="echoes-of-logos",
                stem=stem,
                prompt_s=_adapt_for_aspect(l_prompt, "square"),
                prompt_l=_adapt_for_aspect(l_prompt, "landscape"),
                prompt_p=_adapt_for_aspect(p_prompt, "portrait"),
                pixelate=2,
                colors=48,
            )
        )
    return jobs


def _load_commander_america_jobs() -> list[StoryBgJob]:
    script_path = GITHUB_ROOT / "commander-america-rpg" / "scripts" / "generate_gemini_backgrounds.py"
    spec = importlib.util.spec_from_file_location("ca_gemini_bg", str(script_path))
    mod = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(mod)
    scene_prompts: dict[str, dict[str, str]] = mod.SCENE_PROMPTS

    story_dir = GITHUB_ROOT / "commander-america-rpg" / "app" / "src" / "main" / "assets" / "story"
    files = set(p.name for p in story_dir.iterdir() if p.is_file())
    stems = sorted({
        f.rsplit(".", 1)[0][:-2] if f.rsplit(".", 1)[0].endswith(("_p", "_s")) else f.rsplit(".", 1)[0]
        for f in files if f.endswith((".jpg", ".png", ".webp"))
    })

    jobs: list[StoryBgJob] = []
    for stem in stems:
        has_all = all((story_dir / f"{stem}{sfx}.jpg").is_file() for sfx in ("", "_p", "_s"))
        if has_all:
            continue
        info = scene_prompts.get(f"{stem}.jpg")
        if not info:
            raise RuntimeError(f"Missing prompt for commander-america-rpg/story/{stem}.jpg")
        raw = _strip_neg_and_flags(info["prompt"])
        jobs.append(
            StoryBgJob(
                game="commander-america-rpg",
                stem=stem,
                prompt_s=_adapt_for_aspect(raw, "square"),
                prompt_l=_adapt_for_aspect(raw, "landscape"),
                prompt_p=_adapt_for_aspect(raw, "portrait"),
                pixelate=2,
                colors=48,
            )
        )
    return jobs


def _load_engine_template_jobs() -> list[StoryBgJob]:
    return [
        StoryBgJob(
            game="voice-rpg-engine",
            stem="template_scene",
            prompt_s=_adapt_for_aspect(
                "16-bit retro JRPG environment background of an ancient sun-dappled stone proving grounds courtyard, "
                "weathered granite pillars, carved resonance runes, mossy flagstones, blue sky above",
                "square",
            ),
            prompt_l=_adapt_for_aspect(
                "16-bit retro JRPG environment background of an ancient sun-dappled stone proving grounds courtyard, "
                "weathered granite pillars, carved resonance runes, mossy flagstones, blue sky above",
                "landscape",
            ),
            prompt_p=_adapt_for_aspect(
                "16-bit retro JRPG environment background of an ancient sun-dappled stone proving grounds courtyard, "
                "tall weathered granite pillars rising into a bright sky, mossy flagstones below",
                "portrait",
            ),
            pixelate=2,
            colors=48,
        ),
        StoryBgJob(
            game="voice-rpg-engine",
            stem="template_scene_alt",
            prompt_s=_adapt_for_aspect(
                "16-bit retro JRPG environment background of a quiet subterranean Echo Hall sanctuary, vaulted stone "
                "arches, glowing cyan crystal braziers, polished dark marble floor, atmospheric mist",
                "square",
            ),
            prompt_l=_adapt_for_aspect(
                "16-bit retro JRPG environment background of a quiet subterranean Echo Hall sanctuary, vaulted stone "
                "arches, glowing cyan crystal braziers, polished dark marble floor, atmospheric mist",
                "landscape",
            ),
            prompt_p=_adapt_for_aspect(
                "16-bit retro JRPG environment background of a quiet subterranean Echo Hall sanctuary, soaring vaulted "
                "stone arches, glowing cyan crystal braziers, polished dark marble floor below",
                "portrait",
            ),
            pixelate=2,
            colors=48,
        ),
    ]


def collect_all_jobs(game_filter: str | None = None) -> list[StoryBgJob]:
    all_jobs = (
        _load_superstore_jobs()
        + _load_wallys_world_jobs()
        + _load_commander_america_jobs()
        + _load_echoes_jobs()
        + _load_engine_template_jobs()
    )
    if game_filter:
        return [j for j in all_jobs if j.game == game_filter]
    return all_jobs


def _pick_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return int(s.getsockname()[1])


def _comfy_reachable(url: str) -> bool:
    try:
        with urllib.request.urlopen(f"{url}/system_stats", timeout=3) as r:
            return r.status == 200
    except Exception:
        return False


def _comfy_get(url: str, path: str) -> dict:
    with urllib.request.urlopen(f"{url}{path}", timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _comfy_post(url: str, path: str, payload: dict) -> dict:
    req = urllib.request.Request(
        f"{url}{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _comfy_view(url: str, img_info: dict) -> bytes:
    fn = urllib.parse.quote(img_info["filename"])
    sf = urllib.parse.quote(img_info.get("subfolder", ""))
    tp = urllib.parse.quote(img_info.get("type", "output"))
    with urllib.request.urlopen(f"{url}/view?filename={fn}&subfolder={sf}&type={tp}", timeout=60) as r:
        return r.read()


def build_triplet_workflow(job: StoryBgJob, seed: int, steps: int = 4) -> dict:
    prefix = f"storybg_{job.game}_{job.stem}"
    return {
        "1": {"class_type": "UnetLoaderGGUF", "inputs": {"unet_name": KLEIN_UNET}},
        "2": {"class_type": "CLIPLoader", "inputs": {"clip_name": KLEIN_TE, "type": "flux2"}},
        "3": {"class_type": "VAELoader", "inputs": {"vae_name": KLEIN_VAE}},
        "4": {"class_type": "KSamplerSelect", "inputs": {"sampler_name": "euler"}},
        "5": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["2", 0], "text": ""}},
        "6": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["2", 0], "text": job.prompt_s}},
        "7": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["2", 0], "text": job.prompt_l}},
        "8": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["2", 0], "text": job.prompt_p}},
        # 1. Square (1024x1024)
        "10": {"class_type": "EmptyFlux2LatentImage", "inputs": {"width": 1024, "height": 1024, "batch_size": 1}},
        "11": {"class_type": "Flux2Scheduler", "inputs": {"steps": steps, "width": 1024, "height": 1024}},
        "12": {
            "class_type": "SamplerCustom",
            "inputs": {
                "model": ["1", 0],
                "add_noise": True,
                "noise_seed": seed,
                "cfg": 1.0,
                "positive": ["6", 0],
                "negative": ["5", 0],
                "sampler": ["4", 0],
                "sigmas": ["11", 0],
                "latent_image": ["10", 0],
            },
        },
        "13": {"class_type": "VAEDecode", "inputs": {"samples": ["12", 0], "vae": ["3", 0]}},
        "14": {"class_type": "SaveImage", "inputs": {"filename_prefix": f"{prefix}_s", "images": ["13", 0]}},
        # 2. Landscape (1376x768) conditioned on Square latent
        "20": {"class_type": "ReferenceLatent", "inputs": {"conditioning": ["7", 0], "latent": ["12", 0]}},
        "21": {"class_type": "EmptyFlux2LatentImage", "inputs": {"width": 1376, "height": 768, "batch_size": 1}},
        "22": {"class_type": "Flux2Scheduler", "inputs": {"steps": steps, "width": 1376, "height": 768}},
        "23": {
            "class_type": "SamplerCustom",
            "inputs": {
                "model": ["1", 0],
                "add_noise": True,
                "noise_seed": seed + 1,
                "cfg": 1.0,
                "positive": ["20", 0],
                "negative": ["5", 0],
                "sampler": ["4", 0],
                "sigmas": ["22", 0],
                "latent_image": ["21", 0],
            },
        },
        "24": {"class_type": "VAEDecode", "inputs": {"samples": ["23", 0], "vae": ["3", 0]}},
        "25": {"class_type": "SaveImage", "inputs": {"filename_prefix": f"{prefix}_l", "images": ["24", 0]}},
        # 3. Portrait (720x1280) conditioned on Square latent
        "30": {"class_type": "ReferenceLatent", "inputs": {"conditioning": ["8", 0], "latent": ["12", 0]}},
        "31": {"class_type": "EmptyFlux2LatentImage", "inputs": {"width": 720, "height": 1280, "batch_size": 1}},
        "32": {"class_type": "Flux2Scheduler", "inputs": {"steps": steps, "width": 720, "height": 1280}},
        "33": {
            "class_type": "SamplerCustom",
            "inputs": {
                "model": ["1", 0],
                "add_noise": True,
                "noise_seed": seed + 2,
                "cfg": 1.0,
                "positive": ["30", 0],
                "negative": ["5", 0],
                "sampler": ["4", 0],
                "sigmas": ["32", 0],
                "latent_image": ["31", 0],
            },
        },
        "34": {"class_type": "VAEDecode", "inputs": {"samples": ["33", 0], "vae": ["3", 0]}},
        "35": {"class_type": "SaveImage", "inputs": {"filename_prefix": f"{prefix}_p", "images": ["34", 0]}},
    }


def _pixelate_2x(img: Image.Image, cell: int, colors: int) -> Image.Image:
    if cell <= 1:
        return img
    sw = max(1, img.width // cell)
    sh = max(1, img.height // cell)
    small = img.resize((sw, sh), Image.Resampling.BOX)
    if colors > 0:
        small = small.quantize(colors=colors, method=Image.Quantize.MEDIANCUT).convert("RGB")
    out = small.resize((sw * cell, sh * cell), Image.Resampling.NEAREST)
    if out.size != img.size:
        out = out.resize(img.size, Image.Resampling.NEAREST)
    return out


def _save_atomic_jpeg(raw_png: bytes, dest: Path, pixelate: int, colors: int, quality: int = 94) -> None:
    img = Image.open(io.BytesIO(raw_png)).convert("RGB")
    img = _pixelate_2x(img, cell=pixelate, colors=colors)
    dest.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(prefix=".storybg_", suffix=".jpg", dir=str(dest.parent))
    try:
        import os
        os.close(fd)
        tmp_path = Path(tmp_name)
        img.save(tmp_path, "JPEG", quality=quality, optimize=True)
        tmp_path.replace(dest)
    except Exception:
        Path(tmp_name).unlink(missing_ok=True)
        raise


def run_job(url: str, job: StoryBgJob, steps: int = 4) -> None:
    wf = build_triplet_workflow(job, seed=job.seed(), steps=steps)
    pid = _comfy_post(url, "/prompt", {"prompt": wf, "client_id": str(uuid.uuid4())})["prompt_id"]
    while True:
        hist = _comfy_get(url, f"/history/{pid}")
        if pid in hist:
            entry = hist[pid]
            status = entry.get("status") or {}
            if status.get("status_str") == "error":
                raise RuntimeError(f"ComfyUI error on {job.game}/{job.stem}: {status}")
            outputs = entry.get("outputs") or {}
            png_s = _comfy_view(url, outputs["14"]["images"][0])
            png_l = _comfy_view(url, outputs["25"]["images"][0])
            png_p = _comfy_view(url, outputs["35"]["images"][0])
            _save_atomic_jpeg(png_s, job.path_s, job.pixelate, job.colors)
            _save_atomic_jpeg(png_l, job.path_l, job.pixelate, job.colors)
            _save_atomic_jpeg(png_p, job.path_p, job.pixelate, job.colors)
            return
        time.sleep(0.3)


def sync_templates_to_superstore() -> None:
    src_dir = GITHUB_ROOT / "voice-rpg-engine" / "app" / "src" / "main" / "assets" / "story"
    dst_dir = GITHUB_ROOT / "superstore-saga" / "app" / "src" / "main" / "assets" / "story"
    for stem in ("template_scene", "template_scene_alt"):
        for sfx in ("", "_p", "_s"):
            src = src_dir / f"{stem}{sfx}.jpg"
            dst = dst_dir / f"{stem}{sfx}.jpg"
            if src.is_file():
                shutil.copy2(src, dst)


def main() -> int:
    ap = argparse.ArgumentParser(description="Generate Square->Landscape+Portrait story backgrounds on Desktop ComfyUI.")
    ap.add_argument("--game", default="", help="Only run one game slug (e.g. superstore-saga)")
    ap.add_argument("--limit", type=int, default=0, help="Stop after N scenes")
    ap.add_argument("--skip-existing", action="store_true", help="Skip stems that already have L, P, and S")
    ap.add_argument("--steps", type=int, default=4)
    ap.add_argument("--comfy-url", default="", help="Override ComfyUI URL (default: SSH tunnel to desktop :8188)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    jobs = collect_all_jobs(args.game or None)
    if args.skip_existing:
        jobs = [j for j in jobs if not (j.path_l.is_file() and j.path_p.is_file() and j.path_s.is_file())]
    if args.limit > 0:
        jobs = jobs[: args.limit]

    print(f"Prepared {len(jobs)} story background triplet(s) ({len(jobs) * 3} JPEGs).", flush=True)
    if args.dry_run:
        for i, j in enumerate(jobs, 1):
            print(f"[{i:02d}] {j.game}/{j.stem} (pixelate={j.pixelate}x, colors={j.colors})")
        return 0

    tunnel_proc = None
    url = args.comfy_url
    if not url:
        local_port = _pick_free_port()
        url = f"http://127.0.0.1:{local_port}"
        tunnel_proc = subprocess.Popen(
            ["ssh", "-N", "-L", f"{local_port}:127.0.0.1:8188", DESKTOP_SSH],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        for _ in range(30):
            if _comfy_reachable(url):
                break
            time.sleep(0.25)
        if not _comfy_reachable(url):
            tunnel_proc.terminate()
            raise SystemExit(f"Could not reach Desktop ComfyUI via SSH tunnel at {url}")

    t_start = time.monotonic()
    try:
        for idx, job in enumerate(jobs, 1):
            t0 = time.monotonic()
            print(f"[{idx}/{len(jobs)}] {job.game}/{job.stem} (pixelate={job.pixelate}x)...", flush=True)
            run_job(url, job, steps=args.steps)
            dt = time.monotonic() - t0
            print(f"  -> saved _s, base, _p in {dt:.1f}s (elapsed {(time.monotonic() - t_start)/60:.1f}m)", flush=True)
        if not args.game or args.game in ("voice-rpg-engine", "superstore-saga"):
            sync_templates_to_superstore()
    finally:
        if tunnel_proc is not None:
            tunnel_proc.terminate()
            tunnel_proc.wait(timeout=5)

    print(f"Done in {(time.monotonic() - t_start)/60:.1f} min.", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
