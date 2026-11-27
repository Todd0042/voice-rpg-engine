#!/usr/bin/env python3
"""Lint (and fix) a game's battle sprite sheets before they ship (docs/SPRITE_PLAN.md).

Checks every `game/sprites/sheets/<id>.json` + PNG for what AI-made sheets typically get wrong:
  - labels baked into frames ("IDLE", "WINDUP"...): small one-line marks near the top of a cell
  - pure magenta pixels (a keying leftover, or a deliberate magic glow): reported for a look, never removed
  - a `feet` slice that doesn't sit on the soles (the sprite would float or sink); the soles are the
    lowest opaque row of the idle frames
  - cells that aren't 64 px (bosses 64/96/128) or a PNG that doesn't match the JSON
  - preview or other stray images inside the sheets folder (they would ship in the APK)

  scripts/sprite_lint.py ../echoes-of-logos            # report
  scripts/sprite_lint.py ../echoes-of-logos --fix      # repair in place; strays move to art/sprite-previews/
  scripts/sprite_lint.py ../echoes-of-logos --fix --strip-above-body wisp,archon   # labels the safe rules miss

Exit code 0 when nothing is left to fix.
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from collections import deque

import numpy as np
from PIL import Image

SHEETS = "app/src/main/assets/game/sprites/sheets"
STRAYS = "art/sprite-previews"
LABEL_ROWS = 17      # labels sit in the top rows of a cell
LABEL_HEIGHT = 5     # one line of tiny text
LABEL_MAX_PIXELS = 90
LABEL_MIN_PIXELS = 8   # a line of text has at least this many pixels; fewer is a stray speck


def components(mask: np.ndarray) -> list[list[tuple[int, int]]]:
    h, w = mask.shape
    seen = np.zeros_like(mask)
    out = []
    for y in range(h):
        for x in range(w):
            if mask[y, x] and not seen[y, x]:
                q = deque([(y, x)])
                seen[y, x] = True
                pts = []
                while q:
                    cy, cx = q.popleft()
                    pts.append((cy, cx))
                    for dy in (-1, 0, 1):
                        for dx in (-1, 0, 1):
                            ny, nx = cy + dy, cx + dx
                            if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                                seen[ny, nx] = True
                                q.append((ny, nx))
                out.append(pts)
    return sorted(out, key=len, reverse=True)


def magenta(rgb: np.ndarray) -> np.ndarray:
    return (rgb[..., 0] > 200) & (rgb[..., 2] > 200) & (rgb[..., 1] < 80)


def frame_rects(data: dict) -> list[dict]:
    frames = data["frames"]
    return list(frames.values()) if isinstance(frames, dict) else frames


def letter_shaped(pts, k: float = 1.0) -> bool:
    """A letter has a few pixels and is no wider than twice its height; the stroke of a motion arc or a streak is
    long and flat, and an aura's flecks are single pixels."""
    ys = [p[0] for p in pts]; xs = [p[1] for p in pts]
    h, w = max(ys) - min(ys) + 1, max(xs) - min(xs) + 1
    return len(pts) >= 4 * k and w <= 2 * h + 1


def lint_sheet(json_path: str, fix: bool, strip_above: bool = False):
    problems = []
    data = json.load(open(json_path))
    meta = data.get("meta", {})
    folder = os.path.dirname(json_path)
    png = os.path.join(folder, os.path.basename(meta.get("image", "")))
    if not os.path.isfile(png):
        return [f"image {meta.get('image')} not found"], []
    img = np.asarray(Image.open(png).convert("RGBA")).copy()
    rects = frame_rects(data)
    cell = rects[0]["sourceSize"]["w"], rects[0]["sourceSize"]["h"]
    if cell[0] != cell[1] or cell[0] not in (64, 96, 128):
        problems.append(f"cells are {cell[0]}x{cell[1]} (64, or 96/128 for a boss)")

    labelled, maybe, keyed = set(), set(), 0
    for i, r in enumerate(rects):
        f = r["frame"]
        c = img[f["y"]:f["y"] + f["h"], f["x"]:f["x"] + f["w"]]
        keyed += int((magenta(c[..., :3]) & (c[..., 3] > 0)).sum())
        comps = components(c[..., 3] > 0)
        k = f["h"] / 64                      # sizes scale with the cell (96 and 128 px bosses)
        body = comps[0] if comps else []
        body_top = min(p[0] for p in body) if body else 0
        small = []
        for pts in comps[1:]:
            ys = [p[0] for p in pts]
            if len(pts) <= LABEL_MAX_PIXELS * k * k and max(ys) - min(ys) <= LABEL_HEIGHT * k:
                small.append((min(ys), max(ys), pts))
        for top, bottom, pts in small:
            # A label is a line of text: near the top of the cell, or a row of 3+ letter-sized marks side by
            # side above the body (scattered debris and spell sparks don't line up like that).
            line = [s for s in small if s[0] <= bottom and s[1] >= top]
            above = bottom < body_top
            if above and not strip_above and bottom > LABEL_ROWS:
                if len(line) >= 3:
                    maybe.add(i)             # looks like a line of text, or aligned debris: a look decides
                continue
            # Text has substance: a line of letters adds up to a few pixels at least; stray specks don't. And it is
            # letters: at least two compact marks, not the thin stroke of a motion arc or an aura's rim.
            letters = [m for m in line if m[1] < body_top and letter_shaped(m[2], k)]
            if above and len(letters) >= 2 and sum(len(m[2]) for m in line) >= LABEL_MIN_PIXELS * k:
                labelled.add(i)
                if fix:
                    for y, x in pts:
                        c[y, x, 3] = 0
    if labelled:
        problems.append(f"text-like marks above the sprite in {len(labelled)} frame(s): {sorted(labelled)[:12]}")
    notes = [f"note: {keyed} pure magenta pixel(s); fine if it's a glow, a keying leftover if not"] if keyed else []
    if maybe:
        notes.append(f"note: a row of small marks above the body in frame(s) {sorted(maybe)[:12]}: labels? "
                     f"If so, rerun with --strip-above-body {os.path.basename(json_path)[:-5]}")

    # Feet: the slice's bottom edge must be the sole line of the idle frames.
    tags = {t["name"]: t for t in meta.get("frameTags", [])}
    idle = tags.get("idle")
    feet_slice = next((s for s in meta.get("slices", []) if s.get("name") == "feet"), None)
    if idle is not None:
        soles = []
        for i in range(idle["from"], idle["to"] + 1):
            f = rects[i]["frame"]
            rows = np.where((img[f["y"]:f["y"] + f["h"], f["x"]:f["x"] + f["w"], 3] > 0).any(axis=1))[0]
            if len(rows):
                soles.append(int(rows.max()) + 1 + rects[i].get("spriteSourceSize", {}).get("y", 0))
        sole = max(set(soles), key=soles.count) if soles else None
        if feet_slice is None:
            problems.append("no feet slice")
            if fix and sole is not None:
                meta.setdefault("slices", []).append({"name": "feet", "color": "#0000ffff",
                    "keys": [{"frame": 0, "bounds": {"x": cell[0] // 2 - 2, "y": sole - 2, "w": 4, "h": 2}}]})
        elif sole is not None:
            key = feet_slice["keys"][0]
            b = key["bounds"]
            bottom = b["y"] + (key["pivot"]["y"] if "pivot" in key else b["h"])
            if bottom != sole:
                problems.append(f"feet slice ends at row {bottom}, the soles are at {sole} (the sprite would {'float' if bottom > sole else 'sink'} {abs(bottom - sole)} px)")
                if fix:
                    key.pop("pivot", None)
                    b["h"] = min(b["h"], sole)
                    b["y"] = sole - b["h"]

    if fix and problems:
        Image.fromarray(img, "RGBA").save(png)
        with open(json_path, "w") as fh:
            json.dump(data, fh, indent=1)
    return problems, notes


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("game", help="the game's folder (or its sprites/sheets folder)")
    ap.add_argument("--fix", action="store_true")
    ap.add_argument("--strip-above-body", default="", metavar="SHEETS",
                    help="comma-separated sheet ids whose every small mark above the body is a label (check by eye first: "
                         "spell sparks and debris above the body would go too)")
    args = ap.parse_args()
    sheets = args.game if os.path.basename(os.path.normpath(args.game)) == "sheets" else os.path.join(args.game, SHEETS)
    if not os.path.isdir(sheets):
        print(f"No sheets folder: {sheets}")
        return 2
    game_root = None if os.path.basename(os.path.normpath(args.game)) == "sheets" else os.path.abspath(args.game)
    remaining = 0

    jsons = sorted(f for f in os.listdir(sheets) if f.endswith(".json"))
    images = {os.path.basename(json.load(open(os.path.join(sheets, j))).get("meta", {}).get("image", "")) for j in jsons}
    strays = sorted(f for f in os.listdir(sheets) if not f.endswith(".json") and f not in images)
    if strays:
        print(f"[stray] {len(strays)} file(s) that aren't sheets would ship in the APK: {', '.join(strays[:6])}")
        if args.fix and game_root:
            dest = os.path.join(game_root, STRAYS)
            os.makedirs(dest, exist_ok=True)
            for f in strays:
                shutil.move(os.path.join(sheets, f), os.path.join(dest, f))
            print(f"        moved to {os.path.relpath(dest, game_root)}/")
        else:
            remaining += len(strays)

    for j in jsons:
        aggressive = j[:-5] in {x.strip() for x in args.strip_above_body.split(",") if x.strip()}
        problems, notes = lint_sheet(os.path.join(sheets, j), args.fix, aggressive)
        name = j[:-5]
        if not problems:
            print(f"[ok]    {name}")
            for n in notes:
                print(f"        {n}")
            continue
        print(f"[{'fixed' if args.fix else 'fix'}] {name}")
        for p in problems:
            print(f"        - {p}")
        for n in notes:
            print(f"        {n}")
        if args.fix:
            after, _ = lint_sheet(os.path.join(sheets, j), fix=False, strip_above=aggressive)
            for p in after:
                print(f"        ! still: {p}")
            remaining += len(after)
        else:
            remaining += len(problems)
    return 0 if remaining == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
