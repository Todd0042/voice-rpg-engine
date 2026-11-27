"""Generation plan generator for any voice-rpg-engine game repository."""

from __future__ import annotations

import json
from pathlib import Path
import time
from typing import Any

SKIP_CHAR_IDS = {"narrator", "hiring_portal", "voice_mote"}
GENERIC_FANTASY_ENVS = {"castle", "cave", "dungeon", "forest", "swamp"}


def _load_json(path: Path, default: Any) -> Any:
    if not path.is_file():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return default


def _action_prompts_for_moves(moves: list[dict[str, Any]], role: str) -> list[dict[str, str]]:
    """Build concrete pose + weapon/prop + end-result prompts from actual character/enemy moves."""
    actions: list[dict[str, str]] = [
        {
            "poseKey": "idle_right",
            "tag": "idle",
            "desc": "combat ready stance, weight balanced, facing right",
        },
        {
            "poseKey": "hurt_right",
            "tag": "hurt",
            "desc": "staggering backward from hit impact, both arms thrown up defensively, shocked expression, facing right",
        },
    ]
    seen_tags = {"idle", "hurt"}
    for m in moves[:3]:
        mname = str(m.get("name") or "").strip()
        kind = str(m.get("kind") or "BASIC").upper()
        eff = str((m.get("graphics") or {}).get("effectType") or "SLASH").upper()
        if kind in ("BUFF", "HEAL", "DEBUFF") or eff in ("AURA", "BOLT", "BEAM"):
            if "cast" not in seen_tags:
                seen_tags.add("cast")
                actions.append(
                    {
                        "poseKey": "cast_right",
                        "tag": "cast",
                        "desc": f"executing '{mname}': wide power stance, pointing or gesturing forward, mouth open shouting/chanting, facing right",
                    }
                )
        else:
            if "windup" not in seen_tags:
                seen_tags.add("windup")
                actions.append(
                    {
                        "poseKey": "windup_right",
                        "tag": "windup",
                        "desc": f"winding up '{mname}': drawing signature item/weapon from pocket or shoulder with both hands, torso twisted back, facing right",
                    }
                )
            if "attack" not in seen_tags:
                seen_tags.add("attack")
                actions.append(
                    {
                        "poseKey": "attack_right",
                        "tag": "attack",
                        "desc": f"striking with '{mname}': lunging forward on front foot, swinging weapon/item horizontally forward, dynamic follow-through, facing right",
                    }
                )
    if "attack" not in seen_tags:
        actions.append(
            {
                "poseKey": "attack_right",
                "tag": "attack",
                "desc": "lunging forward with signature strike, dynamic follow-through, facing right",
            }
        )
    if role == "party":
        actions.append(
            {
                "poseKey": "victory_front",
                "tag": "victory",
                "desc": "celebrating victory facing forward toward camera, both arms raised triumphantly, smiling",
            }
        )
    return actions


def _existing_status(game_dir: Path, kind: str, asset_id: str) -> tuple[str, str]:
    """Check if an asset was already generated in art/ or sprites/sheets/."""
    sheets = game_dir / "app/src/main/assets/game/sprites/sheets"
    art_dir = game_dir / "art"
    if kind == "base_sprite":
        raw = art_dir / "battle_work" / asset_id / f"{asset_id}_raw.png"
        if raw.is_file() and (art_dir / "battle_work" / asset_id / "pose_7.png").is_file():
            return ("completed", str(raw))
    if kind == "wan_animation":
        wg = art_dir / "battle_work" / asset_id / "wan_grid.png"
        if wg.is_file():
            return ("completed", str(wg))
        wf = art_dir / "battle_work" / asset_id / "wan_frames"
        if wf.is_dir() and any(wf.glob("*.png")):
            first = sorted(wf.glob("*.png"))[0]
            return ("completed", str(first))
    if kind == "sprite_sheet":
        sh_png = sheets / f"{asset_id}.png"
        sh_json = sheets / f"{asset_id}.json"
        if sh_png.is_file() and sh_json.is_file():
            try:
                meta = json.loads(sh_json.read_text(encoding="utf-8")).get("meta", {})
                if meta.get("app") == "voice-rpg-engine/generate_battle_sprites":
                    prev = game_dir / f"art/sprite-previews/{asset_id}_preview.png"
                    return ("completed", str(prev if prev.is_file() else sh_png))
            except Exception:
                pass
    if kind == "combat_backdrop":
        bg = game_dir / f"app/src/main/assets/game/environments/{asset_id}.jpg"
        bg_png = game_dir / f"app/src/main/assets/game/environments/{asset_id}.png"
        if bg.is_file():
            return ("completed", str(bg))
        if bg_png.is_file():
            return ("completed", str(bg_png))
    return ("pending", "")


def _parse_deliver_briefs(game_dir: Path) -> dict[str, dict[str, Any]]:
    """Parse exact role, cell size, and description from art/SPRITE_BRIEFS.md Deliver: lines."""
    import re

    briefs_path = game_dir / "art/SPRITE_BRIEFS.md"
    if not briefs_path.is_file():
        return {}
    out: dict[str, dict[str, Any]] = {}
    cur_id = ""
    desc_lines: list[str] = []

    def _flush() -> None:
        if cur_id and cur_id in out and desc_lines:
            out[cur_id]["desc"] = " ".join(desc_lines).strip()

    for raw in briefs_path.read_text(encoding="utf-8").splitlines():
        s = raw.strip()
        m_hdr = re.match(r"^###\s+`([^`]+)`:\s*([^(\u2705\u26a0]+)", s)
        if m_hdr:
            _flush()
            cur_id = m_hdr.group(1).strip()
            clean_name = m_hdr.group(2).strip()
            out.setdefault(cur_id, {"name": clean_name or cur_id})
            desc_lines = []
            continue
        if s.startswith("## "):
            _flush()
            cur_id = ""
            desc_lines = []
            continue
        if not cur_id:
            continue
        if s.startswith("> **Deliver:**"):
            m_del = re.search(r"\*\*(party|enemy|boss)\*\*\s*·\s*\*\*(\d+)x\d+\*\*", s)
            if m_del:
                out[cur_id]["role"] = m_del.group(1)
                out[cur_id]["cell"] = int(m_del.group(2))
            if "also fought as a boss" in s or "charge_ultimate" in s:
                out[cur_id]["bossExtras"] = True
            continue
        if s and not s.startswith("Element:"):
            desc_lines.append(s.replace("*Moves*:", "Moves:").strip())
    _flush()
    return out


def generate_game_plan(game_dir: Path, *, wave_size: int = 5, palette_colors: int = 0) -> dict[str, Any]:
    """Build a complete wave-ordered art queue (<game>/art/art_queue.json) for a game repo."""
    assets_dir = game_dir / "app/src/main/assets/game"
    if not assets_dir.is_dir():
        raise FileNotFoundError(f"Not a voice-rpg-engine game directory: {game_dir}")

    spells_raw = _load_json(assets_dir / "enemies/enemy_spells.json", {})
    movesets = spells_raw.get("movesets") if isinstance(spells_raw, dict) else {}
    if not isinstance(movesets, dict):
        movesets = {}

    char_prompts_raw = _load_json(game_dir / "docs/CHARACTER_PROMPTS.json", {})
    char_prompts = char_prompts_raw.get("characters", {}) if isinstance(char_prompts_raw, dict) else {}
    if not isinstance(char_prompts, dict):
        char_prompts = {}
    deliver_briefs = _parse_deliver_briefs(game_dir)

    items: list[dict[str, Any]] = []
    # 1. Party / Characters first
    chars_dir = assets_dir / "characters"
    char_entries: list[dict[str, Any]] = []
    if chars_dir.is_dir():
        for fp in sorted(chars_dir.glob("*.json")):
            cid = fp.stem
            if cid in SKIP_CHAR_IDS or cid.startswith("narrator_") or "template" in cid:
                continue
            d = _load_json(fp, {})
            if not isinstance(d, dict):
                continue
            cp = char_prompts.get(cid, {}) if isinstance(char_prompts.get(cid), dict) else {}
            anchors = [str(a) for a in (cp.get("keyVisualAnchors") or [])[:4] if a]
            gender = str(d.get("gender") or "").strip()
            base_parts = [str(d.get("name") or cid)]
            if gender:
                base_parts.append(gender)
            if d.get("title"):
                base_parts.append(str(d["title"]))
            if anchors:
                base_parts.append(", ".join(anchors))
            elif d.get("_doc"):
                base_parts.append(str(d["_doc"]))
            char_entries.append(
                {
                    "id": cid,
                    "name": str(d.get("name") or cid),
                    "role": "party",
                    "promptDesc": ", ".join(p for p in base_parts if p),
                    "actions": _action_prompts_for_moves([], "party"),
                }
            )

    # 2. Enemies & Bosses (skip template_*)
    enemies_raw = _load_json(assets_dir / "enemies/enemies.json", [])
    enemies_list = enemies_raw.get("enemies", []) if isinstance(enemies_raw, dict) else enemies_raw
    enemy_entries: list[dict[str, Any]] = []
    seen_e = {c["id"] for c in char_entries}
    for e in enemies_list or []:
        eid = str(e.get("id") or "").strip()
        if not eid or eid in seen_e or eid.startswith("template_") or eid == "practice_dummy":
            continue
        seen_e.add(eid)
        ms_id = str(e.get("movesetId") or "")
        ms = movesets.get(ms_id) or {}
        moves = list(ms.get("moves") or [])
        br = deliver_briefs.get(eid) or {}
        role = str(br.get("role") or ("boss" if "boss" in eid or "manager" in eid or "auditor" in eid else "enemy"))
        cell = int(br.get("cell") or (128 if role == "boss" else 64))
        prompt_desc = str(br.get("desc") or f"{e.get('name', eid)}, {e.get('subtitle', '')}".strip(", "))
        enemy_entries.append(
            {
                "id": eid,
                "name": str(e.get("name") or eid),
                "role": role,
                "cell": cell,
                "promptDesc": prompt_desc,
                "actions": _action_prompts_for_moves(moves, "enemy"),
            }
        )

    deliver_chars = [
        {
            "id": did,
            "name": str(info.get("name") or did),
            "role": str(info["role"]),
            "cell": int(info["cell"]),
            "promptDesc": str(info.get("desc") or info.get("name") or did),
            "actions": _action_prompts_for_moves([], str(info["role"])),
        }
        for did, info in deliver_briefs.items()
        if info.get("role") and info.get("cell")
    ]
    all_chars = deliver_chars if deliver_chars else (char_entries + enemy_entries)
    wave_num = 1
    for i in range(0, len(all_chars), wave_size):
        chunk = all_chars[i : i + wave_size]
        wave_label = f"Wave 1.{wave_num}"
        for c in chunk:
            br_c = deliver_briefs.get(c["id"]) or {}
            role = str(br_c.get("role") or c.get("role") or "party")
            cell = int(br_c.get("cell") or c.get("cell") or (128 if role == "boss" else 64))
            boss_extras = bool(br_c.get("bossExtras"))
            base_st, base_out = _existing_status(game_dir, "base_sprite", c["id"])
            items.append(
                {
                    "id": f"base:{c['id']}",
                    "assetId": c["id"],
                    "name": f"{c['name']} (Klein 4x2 Key Poses)",
                    "kind": "base_sprite",
                    "role": role,
                    "cell": cell,
                    "bossExtras": boss_extras,
                    "machine": "laptop",
                    "wave": wave_label,
                    "promptDesc": c["promptDesc"],
                    "status": base_st,
                    "outputPath": base_out,
                    "error": "",
                    "durationSec": 0.0,
                }
            )
            wan_st, wan_out = _existing_status(game_dir, "wan_animation", c["id"])
            items.append(
                {
                    "id": f"wan:{c['id']}",
                    "assetId": c["id"],
                    "name": f"{c['name']} (Wan 2.2-14B FLF Frames)",
                    "kind": "wan_animation",
                    "role": role,
                    "cell": cell,
                    "bossExtras": boss_extras,
                    "machine": "desktop",
                    "wave": wave_label,
                    "dependsOn": f"base:{c['id']}",
                    "promptDesc": c["promptDesc"],
                    "status": wan_st,
                    "outputPath": wan_out,
                    "error": "",
                    "durationSec": 0.0,
                }
            )
            sh_st, sh_out = _existing_status(game_dir, "sprite_sheet", c["id"])
            items.append(
                {
                    "id": f"sheet:{c['id']}",
                    "assetId": c["id"],
                    "name": f"{c['name']} ({'24-Color' if palette_colors == 24 else 'Full-Color'} Sheet)",
                    "kind": "sprite_sheet",
                    "role": role,
                    "cell": cell,
                    "bossExtras": boss_extras,
                    "paletteColors": int(palette_colors),
                    "machine": "laptop",
                    "wave": wave_label,
                    "dependsOn": f"wan:{c['id']}",
                    "promptDesc": c["promptDesc"],
                    "status": sh_st,
                    "outputPath": sh_out,
                    "error": "",
                    "durationSec": 0.0,
                }
            )
        wave_num += 1

    # 3. Unique Combat Backgrounds LAST
    env_raw = _load_json(assets_dir / "environments/environments.json", [])
    env_list = env_raw.get("environments", env_raw.get("items", [])) if isinstance(env_raw, dict) else env_raw
    bg_wave = f"Wave 1.{wave_num} (Backgrounds)"
    for env in env_list or []:
        eid = str(env.get("id") or env.get("name") or "").strip()
        if not eid:
            continue
        bg_st, bg_out = _existing_status(game_dir, "combat_backdrop", eid)
        needs_retheme = eid.lower() in GENERIC_FANTASY_ENVS and game_dir.name != "echoes-of-logos"
        items.append(
            {
                "id": f"bg:{eid}",
                "assetId": eid,
                "name": f"Combat Backdrop: {eid}" + (" (retheme from fantasy!)" if needs_retheme else ""),
                "kind": "combat_backdrop",
                "machine": "laptop",
                "wave": bg_wave,
                "promptDesc": f"16-bit painted-horizon RPG battle backdrop for {game_dir.name}: {eid}",
                "status": "pending" if needs_retheme else bg_st,
                "outputPath": "" if needs_retheme else bg_out,
                "error": "",
                "durationSec": 0.0,
            }
        )

    queue_file = game_dir / "art/art_queue.json"
    queue_file.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "game": game_dir.name,
        "gameDir": str(game_dir),
        "paletteColors": int(palette_colors),
        "paused": True,
        "updatedAt": time.time(),
        "items": items,
    }
    queue_file.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    _sync_to_continuum_plans(game_dir.name, items)
    return payload


def _sync_to_continuum_plans(game_name: str, items: list[dict[str, Any]]) -> None:
    """Sync wave summary into ~/LLM-Vault/plans/<game>.json if Continuum Vault exists."""
    plans_dir = Path.home() / "LLM-Vault/plans"
    if not plans_dir.is_dir():
        return
    fp = plans_dir / f"{game_name}.json"
    by_wave: dict[str, list[dict[str, Any]]] = {}
    for it in items:
        by_wave.setdefault(str(it.get("wave") or "Wave 1"), []).append(it)
    steps = []
    for idx, (wname, witems) in enumerate(by_wave.items(), start=1):
        done = sum(1 for x in witems if x["status"] == "completed")
        st = "done" if done == len(witems) else ("in_progress" if done > 0 else "pending")
        steps.append(
            {
                "id": f"w{idx}",
                "text": f"{wname}: {done}/{len(witems)} assets",
                "status": st,
                "notes": "",
                "assignedRoute": "",
            }
        )
    data = {"repo": game_name, "plans": []}
    if fp.is_file():
        try:
            data = json.loads(fp.read_text(encoding="utf-8"))
        except Exception:
            pass
    plans = [p for p in (data.get("plans") or []) if p.get("id") != "art-studio-pipeline"]
    done_steps = sum(1 for s in steps if s["status"] == "done")
    plans.insert(
        0,
        {
            "id": "art-studio-pipeline",
            "title": "Art Studio Dual-Machine Asset Pipeline",
            "status": "completed" if steps and done_steps == len(steps) else ("in_progress" if done_steps > 0 else "incomplete"),
            "priority": 1,
            "summary": f"Managed by voice-rpg-engine Art Studio ({len(items)} total tasks).",
            "updatedAt": time.time(),
            "updatedByAgent": "art-studio",
            "steps": steps,
        },
    )
    data["repo"] = game_name
    data["plans"] = plans
    fp.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
