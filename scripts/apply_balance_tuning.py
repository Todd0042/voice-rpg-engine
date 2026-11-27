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
    edits = {}  # (encounter id, index in its enemies list) -> {"hp": n, "attack": n}
    for enc in data["encounters"]:
        t = tuning.get(enc["id"])
        if not t:
            continue
        hp_s, at_s = t["hpScale"], t["attackScale"]
        for k, ref in enumerate(enc["enemies"]):
            tpl = templates.get(ref.get("templateId"), {})
            change = {}
            if abs(hp_s - 1) > 0.03:
                change["hp"] = max(1, round((ref.get("hp") or tpl.get("hp", 100)) * hp_s))
            if abs(at_s - 1) > 0.03:
                change["attack"] = max(1, round((ref.get("attack") or tpl.get("attack", 10)) * at_s))
            if change:
                ref.update(change)
                edits[(enc["id"], k)] = change
    if not edits:
        print("nothing to change"); return
    m = re.match(r'\{\n( +)"', raw)
    indent = len(m.group(1)) if m else 2
    canonical = json.dumps(json.loads(raw), indent=indent, ensure_ascii=False) + ("\n" if raw.endswith("\n") else "")
    if canonical == raw:
        text = json.dumps(data, indent=indent, ensure_ascii=False) + ("\n" if raw.endswith("\n") else "")
    else:
        text = raw

        def match_close(t, i, open_ch, close_ch):
            depth, j, in_str = 0, i, False
            while True:
                c = t[j]
                if in_str:
                    if c == "\\": j += 1
                    elif c == '"': in_str = False
                elif c == '"': in_str = True
                elif c == open_ch: depth += 1
                elif c == close_ch:
                    depth -= 1
                    if depth == 0: return j
                j += 1

        for (enc_id, k), change in sorted(edits.items(), key=lambda e: -text.find('"%s"' % e[0][0])):
            m = re.search(r'"id"\s*:\s*"%s"' % re.escape(enc_id), text)
            enc_start = text.rfind("{", 0, m.start())
            enc_end = match_close(text, enc_start, "{", "}")
            arr = re.compile(r'"enemies"\s*:\s*\[').search(text, enc_start, enc_end)
            j = arr.end()
            # walk to the k-th object of the array
            for idx in range(k + 1):
                j = text.index("{", j)
                end = match_close(text, j, "{", "}")
                if idx < k: j = end + 1
            i, obj = j, text[j:end + 1]
            for key, val in change.items():
                if re.search(r'"%s"\s*:\s*-?\d+' % key, obj):
                    obj = re.sub(r'("%s"\s*:\s*)-?\d+(\.\d+)?' % key, r'\g<1>%d' % val, obj, count=1)
                else:
                    body = obj[:-1].rstrip()
                    if "\n" in obj:
                        # a multi-line entry: a new line, indented like the entry's last key
                        indent = re.findall(r'\n([ \t]+)"', body)[-1]
                        obj = body + (',\n%s"%s": %d' % (indent, key, val)) + obj[len(body):]
                    else:
                        obj = body + (', "%s": %d' % (key, val)) + obj[len(body):]
            text = text[:i] + obj + text[end + 1:]
        if json.loads(text) != data:
            sys.exit("text edit did not reproduce the intended data; nothing written")
    open(enc_path, "w", encoding="utf-8").write(text)
    print(f"{len(edits)} enemy entries changed in {len({e[0] for e in edits})} encounters")

if __name__ == "__main__":
    main(*sys.argv[1:3])
