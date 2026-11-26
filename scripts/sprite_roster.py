#!/usr/bin/env python3
"""A game's battle sprite roster: who draws which sprite, and the artist's brief (docs/SPRITE_PLAN.md).

The roster (`<game>/art/sprite_roster.json`) maps every party fighter, enemy and boss to a sprite and describes
each sprite to draw. From it this script writes `<game>/art/SPRITE_BRIEFS.md` and, with --apply, points the game
at those sprites (`enemies.json` spriteId, `sprites/sprites.json` placeholders and recolours).

  scripts/sprite_roster.py ../wallys-world            # check the roster covers every fighter and enemy; write the brief
  scripts/sprite_roster.py ../echoes-of-logos --apply # also update enemies.json and sprites.json

Roster sections (all optional except families/bosses):
  style, animations      extra style lines / animation lists (engine defaults below otherwise)
  party                  [{id, name, look, moves?, variant_of?, boss_versions?: [{id, costume}]}]  heroes (variant_of: a
                         recolour of another party sheet, e.g. a spirit form); a hero with boss
                         versions carries the boss tags too, and those bosses are recolours of the hero's sheet
  families               [{key, name, look, moves?, cell?, members: [{id, variant?, scale?}]}]  enemies that share
                         a sprite; a member with `variant` gets its own key (a recolour of the family sheet)
  bosses                 [{id, name, cell, look, moves, variant_of?, placeholder?}]  each boss its own sprite
  mirrors                [{id, of}]  dark recolours of a party sheet, generated from its palette
  remove                 [{id, why}]  enemies the game should drop (unused leftovers) instead of drawing
"""
from __future__ import annotations

import argparse
import json
import os
import sys

STYLE = [
    "16-bit SNES-era JRPG battle sprite, side view, facing RIGHT (the engine mirrors enemies to face the party).",
    "Limited palette (4-8 colours per sprite), 1 px dark outline, no anti-aliasing, transparent background.",
    "Cell 64x64 for party fighters and regular enemies (the body about 40-56 px tall); bosses 96x96 or 128x128 as listed.",
    "Soles on the same row in every frame (row 58 of a 64 cell, 90 of 96, 122 of 128); floating things put their lowest "
    "point or hover shadow there. The feet slice's bottom edge is the sole line.",
    "Nothing but the character in a frame: no labels, captions, frame names or text.",
    "Frame duration 190 ms unless a move needs faster frames; events in the tag user data, counted from 1 within the tag.",
]
ANIMATIONS = {
    "party": ["idle (4f loop)", "ready (4f loop: turn ready, weapon up)", "charge (4f loop: speaking a chant)",
              "cast (5f, release=4)", "attack (5f, impact=4)", "hurt (4f)", "ko (6f, ends lying down)", "victory (4f loop)"],
    "enemy": ["idle (4f loop)", "attack (5f, impact=4)", "hurt (4f)"],
    "enemy_recommended": ["windup (4f loop)", "cast (5f, release=4)", "death (6f; else the engine shatters it)"],
    "boss": ["idle (4f loop)", "windup (4f loop)", "attack (5f, impact=4)", "cast (5f, release=4)", "hurt (4f)",
             "charge_ultimate (4f loop)", "ultimate (6-8f, release=5)"],
    "boss_recommended": ["death (6-8f)", "taunt (4f)", "p2_idle, p2_attack... (second phase)"],
}
BOSS_EXTRA_TAGS = "windup, charge_ultimate, ultimate (and death)"
SHEETS = "app/src/main/assets/game/sprites/sheets"
GAME = "app/src/main/assets/game"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


class Roster:
    def __init__(self, game_dir: str):
        self.dir = os.path.abspath(game_dir)
        self.r = load(os.path.join(self.dir, "art/sprite_roster.json"))
        self.enemies = {e["id"]: e for e in load(os.path.join(self.dir, GAME, "enemies/enemies.json"))["enemies"]}
        self.characters = {}
        cdir = os.path.join(self.dir, GAME, "characters")
        for f in sorted(os.listdir(cdir)):
            if f.endswith(".json"):
                d = load(os.path.join(cdir, f))
                self.characters[d.get("id")] = d

    def delivered(self, key: str) -> bool:
        return os.path.isfile(os.path.join(self.dir, SHEETS, f"{key}.json"))

    def section(self, name):
        return self.r.get(name) or []

    def check(self) -> list[str]:
        seen = []
        for f in self.section("families"):
            seen += [m["id"] for m in f["members"]]
        for p in self.section("party"):
            seen += [v["id"] for v in p.get("boss_versions", [])]
        seen += [m["id"] for m in self.section("mirrors")] + [b["id"] for b in self.section("bosses")] + \
            [x["id"] for x in self.section("remove")]
        problems = []
        missing = sorted(set(self.enemies) - set(seen))
        unknown = sorted(set(seen) - set(self.enemies))
        dupes = sorted({x for x in seen if seen.count(x) > 1})
        boss_ids = {b["id"] for b in self.section("bosses")} | {v["id"] for p in self.section("party") for v in p.get("boss_versions", [])} | \
            {x["id"] for x in self.section("remove")}
        bosses_out = sorted(e for e, d in self.enemies.items() if d.get("isBoss") and e not in boss_ids)
        fighters = {c for c, d in self.characters.items() if d.get("combat")}
        party_missing = sorted(fighters - {p["id"] for p in self.section("party")} - set(self.r.get("drawn", {})))
        if missing: problems.append(f"enemies not in the roster: {missing}")
        if unknown: problems.append(f"not enemies of this game: {unknown}")
        if dupes: problems.append(f"listed twice: {dupes}")
        if bosses_out: problems.append(f"bosses (isBoss) listed as regular enemies: {bosses_out}")
        if party_missing: problems.append(f"party fighters without a brief: {party_missing}")
        not_bosses = sorted(b["id"] for b in self.section("bosses") if b["id"] in self.enemies and not self.enemies[b["id"]].get("isBoss"))
        # An enemy an encounter gives boss phases to is a boss: without the flag it never fires its ultimate,
        # gets no boss bar, and its sheet is checked (and briefed) as a regular enemy's.
        enc_path = os.path.join(self.dir, GAME, "encounters/encounters.json")
        phased = set()
        for x in (load(enc_path).get("encounters", []) if os.path.exists(enc_path) else []):
            inst = {m.get("id"): m.get("templateId") or m.get("id") for m in x.get("enemies", []) if isinstance(m, dict)}
            phased |= {inst.get(ph.get("enemyId"), ph.get("enemyId")) for ph in x.get("bossPhases", [])}
        unflagged = sorted(t for t in phased if t in self.enemies and not self.enemies[t].get("isBoss"))
        if unflagged: problems.append(f"given boss phases in an encounter but not \"isBoss\" in enemies.json: {unflagged}")
        if not_bosses: problems.append(f"listed as bosses but not \"isBoss\" in enemies.json: {not_bosses}")
        bad_cells = sorted(b["id"] for b in self.section("bosses") if b.get("cell", 96) not in (64, 96, 128))
        if bad_cells: problems.append(f"boss cells must be 64, 96 or 128: {bad_cells}")
        return problems

    # ---- apply ----------------------------------------------------------------------------------------------

    def apply(self):
        sprite_of, settings = {}, {}
        for f in self.section("families"):
            key = f["key"]
            if not self.delivered(key) and f.get("placeholder"):
                settings[key] = {"placeholder": f["placeholder"]}
            for m in f["members"]:
                if m.get("variant"):
                    sprite_of[m["id"]] = m["id"]
                    settings[m["id"]] = {"base": key, **({"scale": m["scale"]} if m.get("scale") else {})}
                else:
                    sprite_of[m["id"]] = key
        for p in self.section("party"):
            if p.get("variant_of"):
                settings[p["id"]] = {"base": p["variant_of"]}
            for v in p.get("boss_versions", []):
                sprite_of[v["id"]] = v["id"]
                settings[v["id"]] = {"base": p["id"]}
        for m in self.section("mirrors"):
            sprite_of[m["id"]] = m["id"]
            settings[m["id"]] = {"base": m["of"], "palette": self.shadow_palette(m["of"])}
        for b in self.section("bosses"):
            sprite_of[b["id"]] = b["id"]
            if b.get("variant_of"):
                settings[b["id"]] = {"base": b["variant_of"]}
            elif not self.delivered(b["id"]) and b.get("placeholder"):
                settings[b["id"]] = {"placeholder": b["placeholder"]}
        path = os.path.join(self.dir, GAME, "enemies/enemies.json")
        doc = load(path)
        for e in doc["enemies"]:
            if e["id"] in sprite_of:
                e["spriteId"] = sprite_of[e["id"]]
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(doc, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        os.makedirs(os.path.join(self.dir, GAME, "sprites"), exist_ok=True)
        with open(os.path.join(self.dir, GAME, "sprites/sprites.json"), "w", encoding="utf-8") as fh:
            json.dump({"_doc": "Generated by voice-rpg-engine/scripts/sprite_roster.py from art/sprite_roster.json; edit the roster, "
                               "not this file. placeholder = drawn until the sprite's own sheet arrives; base = a recolour of another sheet.",
                       "sprites": settings}, fh, indent=2, ensure_ascii=False)
            fh.write("\n")

    def shadow_palette(self, key: str) -> dict[str, str]:
        """A dark violet recolour of a party sheet: each colour keeps its brightness."""
        from PIL import Image
        import numpy as np
        meta = load(os.path.join(self.dir, SHEETS, f"{key}.json"))["meta"]
        a = np.asarray(Image.open(os.path.join(self.dir, SHEETS, meta["image"])).convert("RGBA")).reshape(-1, 4)
        dark, light = np.array([12, 8, 24]), np.array([132, 104, 200])
        out = {}
        for c in np.unique(a[a[:, 3] > 0][:, :3], axis=0):
            lum = (0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]) / 255
            n = (dark + (light - dark) * min(1.0, lum ** 0.85 * 1.1)).round().astype(int)
            out["#%02X%02X%02X" % tuple(c)] = "#%02X%02X%02X" % tuple(n)
        return out

    # ---- brief ----------------------------------------------------------------------------------------------

    def who(self, eid):
        e = self.enemies[eid]
        el = "/".join(x.lower() for x in (e.get("family"), e.get("selfElement")) if x)
        return f"{e['name']} ({e.get('subtitle', '')}{', ' + el if el else ''})"

    def mark(self, key):
        return " ✅ delivered" if self.delivered(key) else ""

    def spec(self, key, role, cell, extra=""):
        """The exact delivery spec, so a sheet can't come back as the wrong role or size."""
        tags = {"party": "idle, ready, charge, cast (release=4), attack (impact=4), hurt, ko, victory",
                "enemy": "idle, attack (impact=4), hurt (+ windup, cast (release=4), death)",
                "boss": "idle, windup, attack (impact=4), cast (release=4), hurt, charge_ultimate, ultimate (release=5) (+ death)"}[role]
        return f"> **Deliver:** `{SHEETS}/{key}.png` + `{key}.json` · **{role}** · **{cell}x{cell}** cells · tags: {tags}{extra}"

    def brief(self) -> str:
        r = self.r
        title = r.get("title", os.path.basename(self.dir))
        party, fams, bosses = self.section("party"), self.section("families"), self.section("bosses")
        own_bosses = [b for b in bosses if not b.get("variant_of")]
        todo = [k for k in [p["id"] for p in party if not p.get("variant_of")] + [f["key"] for f in fams if not f.get("delivered")] + [b["id"] for b in own_bosses]
                if not self.delivered(k)]
        L = [f"# {title}: battle sprite brief", "",
             "Generated from `art/sprite_roster.json` by `voice-rpg-engine/scripts/sprite_roster.py`; edit the roster, not this file.",
             "Every fighter, enemy and boss is mapped to a sprite key below. A sheet named after its key, dropped into",
             f"`{SHEETS}/`, appears in battle as soon as the game runs the sprite-ready engine.", ""]
        if r.get("summary"):
            L += [r["summary"], ""]
        def n(k, word):
            return f"{k} {word}" + ("" if k == 1 else ("es" if word.endswith("s") else "s"))
        L += [f"**To draw: {n(len(todo), 'sheet')}** ({n(sum(1 for p in party if not p.get('variant_of') and not self.delivered(p['id'])), 'party fighter')}, "
              f"{n(sum(1 for f in fams if not f.get('delivered') and not self.delivered(f['key'])), 'enemy family').replace('familys', 'families')}, "
              f"{n(sum(1 for b in own_bosses if not self.delivered(b['id'])), 'boss')}). "
              f"Everything else is a recolour of one of those.", "",
              "## Style (every sprite)", ""]
        L += [f"- {s}" for s in r.get("style") or STYLE] + [f"- {s}" for s in r.get("style_extra", [])]
        L += ["", "## Animations (Aseprite tags)", "",
              "- **Party fighters**: " + ", ".join(ANIMATIONS["party"]) + ". A fighter who is also fought as a boss adds " + BOSS_EXTRA_TAGS + ".",
              "- **Regular enemies**: required " + ", ".join(ANIMATIONS["enemy"]) + "; recommended " + ", ".join(ANIMATIONS["enemy_recommended"]) + ".",
              "- **Bosses**: required " + ", ".join(ANIMATIONS["boss"]) + "; recommended " + ", ".join(ANIMATIONS["boss_recommended"]) + ".", "",
              "## Delivering a sprite", "",
              f"1. Save `<key>.png` and its Aseprite JSON export `<key>.json` in `{SHEETS}/`, with the tags above and a `feet` slice.",
              "   Previews go in `art/sprite-previews/`, never in the sheets folder.",
              "2. Run `python3 ../voice-rpg-engine/scripts/sprite_lint.py . --fix`, then `../voice-rpg-engine/scripts/sprite_coverage.sh <game>`.",
              "3. Compare it with everyone else in the game's gallery, **SPRITES** tab.",
              "4. A recolour (listed under *Recolours*) needs no art; a sheet named after its own id replaces the recolour if you want it to differ.", ""]
        if r.get("drawn"):
            L += ["## Already drawn", ""] + [f"- `{k}`: {v}" for k, v in r["drawn"].items()] + [""]
        if party:
            L += [f"## Party fighters ({len(party)}, 64 px)", ""]
            for p in party:
                c = self.characters.get(p["id"], {})
                L += [f"### `{p['id']}`: {p['name']}{self.mark(p['id'])}", ""]
                if not p.get("variant_of"):
                    L += [self.spec(p["id"], "party", 64, " · also the boss tags" if p.get("boss_versions") else ""), ""]
                if c.get("portraitAsset"):
                    L += [f"Portrait: `{c['portraitAsset']}`.", ""]
                L += [p["look"], ""]
                if p.get("variant_of"):
                    L += [f"A recolour of `{p['variant_of']}` (no art needed unless it should differ).", ""]
                if p.get("moves"):
                    L += [f"*Moves*: {p['moves']}", ""]
                if p.get("boss_versions"):
                    L += [f"Also fought as a boss, so this sheet also needs {BOSS_EXTRA_TAGS}. Boss versions (recolours of this sheet; "
                          "draw a separate sheet named after the id only for a costume change):"]
                    L += [f"- `{v['id']}`: {self.who(v['id'])}: {v['costume']}" for v in p["boss_versions"]]
                    L += [""]
        if fams:
            L += [f"## Enemy families ({len(fams)}, 64 px)", ""]
            for f in fams:
                L += [f"### `{f['key']}`: {f['name']}{self.mark(f['key'])}", "", self.spec(f["key"], "enemy", f.get("cell", 64)), "", f["look"], ""]
                if f.get("moves"):
                    L += [f"*Moves*: {f['moves']}", ""]
                plain = [m for m in f["members"] if not m.get("variant")]
                rec = [m for m in f["members"] if m.get("variant")]
                if plain:
                    L += ["Draws: " + "; ".join(self.who(m["id"]) for m in plain), ""]
                if rec:
                    L += ["Recolours:"] + [f"- `{m['id']}`: {self.who(m['id'])}: {m['variant']}" for m in rec] + [""]
        if self.section("mirrors"):
            L += ["## Mirror shadows (no art needed)", "",
                  "Dark violet recolours of the party sheets, generated by the script: " +
                  ", ".join(f"`{m['id']}` ← `{m['of']}`" for m in self.section("mirrors")) + ".", ""]
        if bosses:
            L += [f"## Bosses ({len(own_bosses)} to draw, each its own sprite)", ""]
            for b in bosses:
                L += [f"### `{b['id']}`: {b['name']} ({b.get('cell', 96)} px){self.mark(b['id'])}", ""]
                if not b.get("variant_of"):
                    L += [self.spec(b["id"], "boss", b.get("cell", 96)), ""]
                L += [b["look"], ""]
                if b.get("moves"):
                    L += [f"*Moves*: {b['moves']}", ""]
                e = self.enemies.get(b["id"], {})
                element = "/".join(x.lower() for x in (e.get("family"), e.get("selfElement")) if x)
                bits = [f"Element: {element}."] if element else []
                if b.get("variant_of"):
                    bits.append(f"A recolour of `{b['variant_of']}` (no art needed unless it should differ).")
                if b.get("note"):
                    bits.append(b["note"])
                if bits:
                    L += [" ".join(bits), ""]
        if self.section("remove"):
            L += ["## Not drawn: remove from the game", "",
                  "These enemies aren't reached by the story; remove them (and their encounters) from `enemies.json` at the "
                  "sprite rollout instead of drawing them, or the sprite contract will block the release:"]
            L += [f"- `{x['id']}`: {self.who(x['id'])}: {x['why']}" for x in self.section("remove")] + [""]
        if r.get("notes"):
            L += ["## Notes for the game", ""] + [f"- {n}" for n in r["notes"]] + [""]
        return "\n".join(L).rstrip() + "\n"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("game")
    ap.add_argument("--apply", action="store_true", help="also update enemies.json spriteId and sprites/sprites.json")
    args = ap.parse_args()
    roster = Roster(args.game)
    problems = roster.check()
    if problems:
        print("\n".join(problems))
        return 1
    n_fam = len(roster.section("families"))
    print(f"roster covers all {len(roster.enemies)} enemies ({n_fam} families, {len(roster.section('bosses'))} bosses, "
          f"{len(roster.section('party'))} party fighters, {len(roster.section('remove'))} to remove)")
    if args.apply:
        roster.apply()
        print("updated enemies.json and sprites/sprites.json")
    with open(os.path.join(roster.dir, "art/SPRITE_BRIEFS.md"), "w", encoding="utf-8") as fh:
        fh.write(roster.brief())
    print("wrote art/SPRITE_BRIEFS.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
