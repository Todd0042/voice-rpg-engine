"""FastAPI server & dual-machine ComfyUI runner for voice-rpg-engine Art Studio."""

from __future__ import annotations

import argparse
import io
import json
from pathlib import Path
import shutil
import subprocess
import time
from typing import Any
import urllib.parse
import urllib.request
import uuid

from fastapi import Body, FastAPI, HTTPException, Query
from fastapi.responses import FileResponse, HTMLResponse
import uvicorn

from .planner import generate_game_plan
from .poses import ensure_pose_images
from .worker import ArtQueueManager

COMFY_URL = "http://127.0.0.1:8188"
COMFY_INPUT = Path.home() / "ai/ComfyUI/input"
DESKTOP_SSH = "todd@192.168.1.236"


def _comfy_post(obj: dict[str, Any]) -> dict[str, Any]:
    req = urllib.request.Request(
        f"{COMFY_URL}/prompt",
        data=json.dumps(obj).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def _comfy_wait(prompt_id: str, timeout_sec: float = 240.0) -> bytes:
    t0 = time.time()
    while True:
        with urllib.request.urlopen(f"{COMFY_URL}/history/{prompt_id}", timeout=15) as resp:
            h = json.loads(resp.read().decode())
        if prompt_id in h:
            outs = h[prompt_id]["outputs"]["9"]["images"]
            q = urllib.parse.urlencode(
                {"filename": outs[0]["filename"], "subfolder": outs[0].get("subfolder", ""), "type": "output"}
            )
            with urllib.request.urlopen(f"{COMFY_URL}/view?{q}", timeout=20) as vresp:
                return vresp.read()
        if time.time() - t0 > timeout_sec:
            raise TimeoutError(f"ComfyUI timeout for {prompt_id}")
        time.sleep(1.5)


def _ensure_laptop_comfy() -> None:
    try:
        urllib.request.urlopen(f"{COMFY_URL}/system_stats", timeout=1.5).close()
        return
    except Exception:
        pass
    starter = Path.home() / "graphical-studio/start_comfyui.sh"
    if not starter.is_file():
        raise RuntimeError(f"ComfyUI starter not found: {starter}")
    subprocess.Popen([str(starter)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for _ in range(25):
        try:
            urllib.request.urlopen(f"{COMFY_URL}/system_stats", timeout=1.5).close()
            return
        except Exception:
            time.sleep(2.0)
    raise RuntimeError("Laptop ComfyUI did not start in 50s")


def _build_wan_grid(wan_frames_dir: Path) -> Path:
    """Build a labeled row-per-tag contact sheet (wan_grid.png) from Wan output frames."""
    from PIL import Image, ImageDraw

    by_tag: dict[str, list[Path]] = {}
    for fp in sorted(wan_frames_dir.glob("*.png")):
        tag = fp.stem.rsplit("_", 1)[0]
        by_tag.setdefault(tag, []).append(fp)
    if not by_tag:
        raise FileNotFoundError(f"No Wan frames in {wan_frames_dir}")

    thumb = 128
    label_w = 110
    max_cols = max(len(v) for v in by_tag.values())
    grid = Image.new("RGB", (label_w + max_cols * thumb, len(by_tag) * thumb), (18, 22, 36))
    draw = ImageDraw.Draw(grid)
    for row_idx, (tag, fps) in enumerate(by_tag.items()):
        y = row_idx * thumb
        draw.text((10, y + thumb // 2 - 6), tag.upper(), fill=(220, 230, 245))
        for col_idx, fp in enumerate(fps):
            im = Image.open(fp).convert("RGB").resize((thumb, thumb), Image.Resampling.BILINEAR)
            grid.paste(im, (label_w + col_idx * thumb, y))
    out_p = wan_frames_dir.parent / "wan_grid.png"
    grid.save(out_p, "PNG")
    return out_p


def _rembg_clean(img_bytes: bytes) -> Any:
    import os
    from PIL import Image

    try:
        import rembg

        session = rembg.new_session("u2net")
        nobg = rembg.remove(img_bytes, session=session)
    except ModuleNotFoundError:
        venv_py = "/home/todd/graphical-studio/.venv/bin/python"
        env = os.environ.copy()
        env["CUDA_VISIBLE_DEVICES"] = ""
        proc = subprocess.run(
            [
                venv_py,
                "-c",
                (
                    "import sys, rembg; "
                    "sess = rembg.new_session('u2net'); "
                    "sys.stdout.buffer.write(rembg.remove(sys.stdin.buffer.read(), session=sess))"
                ),
            ],
            input=img_bytes,
            capture_output=True,
            env=env,
            timeout=120,
        )
        if proc.returncode != 0:
            raise RuntimeError(
                f"rembg subprocess failed ({proc.returncode}): {proc.stderr.decode(errors='replace')[:300]}"
            )
        nobg = proc.stdout

    im = Image.open(io.BytesIO(nobg)).convert("RGBA")
    if im.getbbox():
        im = im.crop(im.getbbox())
    return im


def execute_art_item(game_dir: Path, item: dict[str, Any]) -> str:
    """Execute one queue item on Laptop ComfyUI or Desktop Wan 2.2."""
    kind = str(item.get("kind") or "")
    aid = str(item.get("assetId") or "")
    desc = str(item.get("promptDesc") or aid)
    art_dir = game_dir / "art"
    art_dir.mkdir(parents=True, exist_ok=True)

    script_path = Path(__file__).resolve().parents[2] / "scripts/generate_battle_sprites.py"
    laptop_py = "/home/todd/graphical-studio/.venv/bin/python"
    work_root = art_dir / "battle_work"
    role = str(item.get("role") or "party")
    cell = int(item.get("cell") or (128 if role == "boss" else 64))

    if kind == "base_sprite":
        import zlib

        try:
            _http_json(
                f"{LAPTOP_COMFY}/free",
                {"unload_models": True, "free_memory": True},
                timeout=10,
            )
        except Exception:
            pass
        seed = 100 + (zlib.crc32(aid.encode("utf-8")) % 90000)
        cmd = [
            laptop_py,
            str(script_path),
            "--id", aid,
            "--role", role,
            "--cell", str(cell),
            "--desc", desc,
            "--seed", str(seed),
            "--studio-dir", str(Path.home() / "graphical-studio"),
            "--work-dir", str(work_root),
            "--only-poses",
        ]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
        if res.returncode != 0:
            raise RuntimeError(f"Klein poses failed: {(res.stderr or res.stdout)[-400:]}")
        raw_p = work_root / aid / f"{aid}_raw.png"
        if not raw_p.is_file():
            raise FileNotFoundError(f"Missing generated raw pose sheet: {raw_p}")
        return str(raw_p)

    if kind == "sprite_sheet":
        sheets_dir = game_dir / "app/src/main/assets/game/sprites/sheets"
        prev_dir = art_dir / "sprite-previews"
        colors = int(item.get("paletteColors") or 0)
        cmd = [
            laptop_py,
            str(script_path),
            "--id", aid,
            "--role", role,
            "--cell", str(cell),
            "--desc", desc,
            "--colors", str(colors),
            "--out-dir", str(sheets_dir),
            "--preview-dir", str(prev_dir),
            "--work-dir", str(work_root),
            "--only-pack",
        ]
        if item.get("bossExtras"):
            cmd.append("--boss-extras")
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        if res.returncode != 0:
            raise RuntimeError(f"Pack sprite sheet failed: {(res.stderr or res.stdout)[-400:]}")
        prev_p = prev_dir / f"{aid}_preview.png"
        return str(prev_p if prev_p.is_file() else sheets_dir / f"{aid}.png")

    if kind == "combat_backdrop":
        _ensure_laptop_comfy()
        wf = {
            "4": {"class_type": "CheckpointLoaderSimple", "inputs": {"ckpt_name": "sd_xl_base_1.0.safetensors"}},
            "10": {"class_type": "LoraLoader", "inputs": {"lora_name": "pixel-art-xl.safetensors", "model": ["4", 0], "clip": ["4", 1], "strength_model": 0.85, "strength_clip": 0.85}},
            "5": {"class_type": "EmptyLatentImage", "inputs": {"batch_size": 1, "height": 768, "width": 1344}},
            "6": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["10", 1], "text": f"pixel art, 16-bit SNES RPG battle arena backdrop, wide establishing shot, clear flat floor in lower third, painted horizon, {desc}, no characters, no people"}},
            "7": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["10", 1], "text": "characters, people, UI, text, watermark, blurry"}},
            "3": {"class_type": "KSampler", "inputs": {"cfg": 7.0, "denoise": 1.0, "latent_image": ["5", 0], "model": ["10", 0], "negative": ["7", 0], "positive": ["6", 0], "sampler_name": "euler_ancestral", "scheduler": "normal", "seed": int(time.time()) % 999999, "steps": 24}},
            "8": {"class_type": "VAEDecode", "inputs": {"samples": ["3", 0], "vae": ["4", 2]}},
            "9": {"class_type": "SaveImage", "inputs": {"filename_prefix": f"bg_{aid}", "images": ["8", 0]}},
        }
        pid = _comfy_post({"prompt": wf, "client_id": str(uuid.uuid4())})["prompt_id"]
        raw = _comfy_wait(pid)
        out_p = game_dir / f"app/src/main/assets/environments/{aid}.png"
        out_p.write_bytes(raw)
        return str(out_p)

    if kind == "battle_sprite":
        import shlex

        role = str(item.get("role") or "party")
        cell = int(item.get("cell") or (128 if role == "boss" else 64))
        desk_game = f"/home/todd/Documents/GitHub/{game_dir.name}"
        desk_sheets = f"{desk_game}/app/src/main/assets/game/sprites/sheets"
        desk_prev = f"{desk_game}/art/sprite-previews"
        subprocess.run(["ssh", DESKTOP_SSH, f"mkdir -p {desk_sheets} {desk_prev}"], check=True, timeout=15)
        cmd = (
            f"/home/todd/storage/graphical-studio/.venv/bin/python "
            f"/home/todd/Documents/GitHub/voice-rpg-engine/scripts/generate_battle_sprites.py "
            f"--id {shlex.quote(aid)} --role {shlex.quote(role)} --cell {cell} "
            f"--desc {shlex.quote(desc)} --out-dir {shlex.quote(desk_sheets)} --preview-dir {shlex.quote(desk_prev)} "
            f"--work-dir /tmp/battle_sprites_work"
        )
        subprocess.run(["ssh", DESKTOP_SSH, cmd], check=True, timeout=1800)

        local_sheets = game_dir / "app/src/main/assets/game/sprites/sheets"
        local_prev_dir = game_dir / "art/sprite-previews"
        local_sheets.mkdir(parents=True, exist_ok=True)
        local_prev_dir.mkdir(parents=True, exist_ok=True)
        subprocess.run(["scp", "-q", f"{DESKTOP_SSH}:{desk_sheets}/{aid}.png", str(local_sheets / f"{aid}.png")], check=True, timeout=30)
        subprocess.run(["scp", "-q", f"{DESKTOP_SSH}:{desk_sheets}/{aid}.json", str(local_sheets / f"{aid}.json")], check=True, timeout=30)
        local_prev = local_prev_dir / f"{aid}_preview.png"
        subprocess.run(["scp", "-q", f"{DESKTOP_SSH}:{desk_prev}/{aid}_preview.png", str(local_prev)], check=True, timeout=30)
        return str(local_prev)

    if kind == "wan_animation":
        import shlex

        local_char_work = work_root / aid
        if not (local_char_work / "pose_7.png").is_file():
            raise FileNotFoundError(f"Key poses missing for {aid} in {local_char_work}")
        desk_work = f"/tmp/art_studio_wan/{game_dir.name}/{aid}"
        subprocess.run(["ssh", DESKTOP_SSH, f"mkdir -p {desk_work}"], check=True, timeout=15)
        pose_files = [str(local_char_work / f"pose_{i}.png") for i in range(8)]
        subprocess.run(["scp", "-q", *pose_files, f"{DESKTOP_SSH}:{desk_work}/"], check=True, timeout=30)
        extras_flag = " --boss-extras" if item.get("bossExtras") else ""
        cmd = (
            f"/home/todd/storage/graphical-studio/.venv/bin/python "
            f"/home/todd/Documents/GitHub/voice-rpg-engine/scripts/generate_battle_sprites.py "
            f"--id {shlex.quote(aid)} --role {shlex.quote(role)} --cell {cell} "
            f"--desc {shlex.quote(desc)} --work-dir /tmp/art_studio_wan/{shlex.quote(game_dir.name)} --only-wan{extras_flag}"
        )
        subprocess.run(["ssh", DESKTOP_SSH, cmd], check=True, timeout=1800)

        local_wf = local_char_work / "wan_frames"
        local_wf.mkdir(parents=True, exist_ok=True)
        subprocess.run(["scp", "-q", f"{DESKTOP_SSH}:{desk_work}/wan_frames/*.png", str(local_wf) + "/"], check=True, timeout=60)
        return str(_build_wan_grid(local_wf))

    raise ValueError(f"Unknown art kind: {kind}")


def create_app(initial_game_dir: Path | None = None) -> FastAPI:
    gh_root = Path.home() / "GitHub"
    default_game = initial_game_dir or (gh_root / "superstore-saga")
    mgr = ArtQueueManager(default_game)

    app = FastAPI(title="VoiceRPG Art Studio")
    static_dir = Path(__file__).parent / "static"

    @app.get("/", response_class=HTMLResponse)
    async def index() -> str:
        return (static_dir / "index.html").read_text(encoding="utf-8")

    @app.get("/api/games")
    async def api_games() -> dict[str, Any]:
        games = sorted(
            p.name for p in gh_root.iterdir()
            if p.is_dir() and (p / "app/src/main/assets/game").is_dir()
        ) if gh_root.is_dir() else []
        return {"games": games, "current": mgr.game_dir.name}

    @app.post("/api/game/select")
    async def api_game_select(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
        name = Path(str(body.get("game") or "")).name
        target = gh_root / name
        if not (target / "app/src/main/assets/game").is_dir():
            raise HTTPException(status_code=404, detail=f"Game not found: {name}")
        mgr.set_game_dir(target)
        return mgr.state_snapshot()

    @app.get("/api/state")
    async def api_state() -> dict[str, Any]:
        return mgr.state_snapshot()

    @app.post("/api/plan/generate")
    async def api_plan_generate(body: dict[str, Any] = Body(default={})) -> dict[str, Any]:
        name = Path(str(body.get("game") or mgr.game_dir.name)).name
        target = gh_root / name
        if not target.is_dir():
            raise HTTPException(status_code=404, detail=f"Game not found: {name}")
        mgr.set_game_dir(target)
        palette_colors = int(body.get("paletteColors") or 0)
        generate_game_plan(
            target,
            wave_size=int(body.get("waveSize") or 5),
            palette_colors=palette_colors,
        )
        return mgr.state_snapshot()

    @app.post("/api/queue/pause")
    async def api_queue_pause(body: dict[str, Any] = Body(default={})) -> dict[str, Any]:
        return mgr.pause(machine=str(body.get("machine") or "all"))

    @app.post("/api/queue/resume")
    async def api_queue_resume(body: dict[str, Any] = Body(default={})) -> dict[str, Any]:
        return mgr.resume(execute_art_item, machine=str(body.get("machine") or "all"))

    @app.get("/api/preview")
    async def api_preview(path: str = Query(...)) -> FileResponse:
        p = Path(path).resolve()
        if not p.is_file() or not str(p).startswith(str(mgr.game_dir.resolve())):
            raise HTTPException(status_code=404, detail="Preview not found")
        return FileResponse(p)

    return app


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--game", default=str(Path.home() / "GitHub/superstore-saga"))
    ap.add_argument("--port", type=int, default=8792)
    args = ap.parse_args()
    app = create_app(Path(args.game))
    uvicorn.run(app, host="127.0.0.1", port=args.port, log_level="warning")


if __name__ == "__main__":
    main()
