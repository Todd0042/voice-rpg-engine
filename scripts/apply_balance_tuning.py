#!/usr/bin/env python3
"""Write a balance tuning into a game's encounters.json.

    python3 scripts/apply_balance_tuning.py <game dir> <balance-tuning.json>

The tuning (from BalanceTunerTest, app/build/reports/balance-tuning.json of a content-pack run) gives, per story
encounter, how much to scale its enemies' HP and attack. Each enemy in that encounter gets "hp" and "attack" set
to its current value (the encounter's own, else its template's) times the scale. Scales within 3% of 1 are left
alone. The file keeps its formatting: a file json.dumps reproduces exactly is rewritten; otherwise only the
changed enemy entries are edited as text, and the result must parse to exactly the intended data.
"""
import json, re, sys

def main(game, tuning_path):
    enc_path = f"{game}/app/src/main/assets/game/encounters/encounters.json"
    templates = {e["id"]: e for e in json.load(open(f"{game}/app/src/main/assets/game/enemies/enemies.json"))["enemies"]}
    tuning = json.load(open(tuning_path))
    raw = open(enc_path, encoding="utf-8").read()
    data = json.loads(raw)
    edits = {}  # (encounter id, enemy instance id) -> {"hp": n, "attack": n}
    for enc in data["encounters"]:
        t = tuning.get(enc["id"])
        if not t:
            continue
        hp_s, at_s = t["hpScale"], t["attackScale"]
        for ref in enc["enemies"]:
            tpl = templates.get(ref.get("templateId"), {})
            change = {}
            if abs(hp_s - 1) > 0.03:
                change["hp"] = max(1, round((ref.get("hp") or tpl.get("hp", 100)) * hp_s))
            if abs(at_s - 1) > 0.03:
                change["attack"] = max(1, round((ref.get("attack") or tpl.get("attack", 10)) * at_s))
            if change:
                ref.update(change)
                edits[(enc["id"], ref["id"])] = change
    if not edits:
        print("nothing to change"); return
    m = re.match(r'\{\n( +)"', raw)
    indent = len(m.group(1)) if m else 2
    canonical = json.dumps(json.loads(raw), indent=indent, ensure_ascii=False) + ("\n" if raw.endswith("\n") else "")
    if canonical == raw:
        text = json.dumps(data, indent=indent, ensure_ascii=False) + ("\n" if raw.endswith("\n") else "")
    else:
        text = raw
        for (enc_id, inst), change in edits.items():
            enc_start = text.index('"id": "%s"' % enc_id) if '"id": "%s"' % enc_id in text else text.index('"id":"%s"' % enc_id)
            pos = re.compile(r'"id"\s*:\s*"%s"' % re.escape(inst)).search(text, enc_start)
            # the enclosing { ... } of this enemy entry
            i = text.rfind("{", 0, pos.start()); depth, j = 0, i
            while True:
                if text[j] == "{": depth += 1
                elif text[j] == "}":
                    depth -= 1
                    if depth == 0: break
                j += 1
            obj = text[i:j + 1]
            for key, val in change.items():
                if re.search(r'"%s"\s*:\s*-?\d+' % key, obj):
                    obj = re.sub(r'("%s"\s*:\s*)-?\d+(\.\d+)?' % key, r'\g<1>%d' % val, obj, count=1)
                else:
                    obj = obj[:-1].rstrip() + (', "%s": %d' % (key, val)) + obj[len(obj[:-1].rstrip()):]
            text = text[:i] + obj + text[j + 1:]
        if json.loads(text) != data:
            sys.exit("text edit did not reproduce the intended data; nothing written")
    open(enc_path, "w", encoding="utf-8").write(text)
    print(f"{len(edits)} enemy entries changed in {len({e[0] for e in edits})} encounters")

if __name__ == "__main__":
    main(*sys.argv[1:3])
