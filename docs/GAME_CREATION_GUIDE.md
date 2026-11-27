# Creating a Game for VoiceRPG Engine

This guide walks through the full workflow for building a new game on **VoiceRPG Engine** — from scaffolding the repository and authoring character profiles to testing content packs and optionally generating animated battle sprites and combat backdrops with **VoiceRPG Art Studio**.

---

## 1. Scaffold Your Game Repository

VoiceRPG Engine separates **engine code** (synced from `voice-rpg-engine`) from **game content** under `app/src/main/assets/game/`:

1. Start from the engine scaffolding package (`com.voicerpg.engine.template`) or copy an existing derived game repository layout.
2. Sync engine updates into your game repo with:
   ```bash
   ./scripts/sync_engine_to_games.sh /path/to/your-game
   ```
3. Ensure your game's content root exists at `app/src/main/assets/game/` with the standard directories:
   - `manifest.json` — game title, campaign tracks, quick-names, starting scene/node.
   - `characters/*.json` — party member and NPC profiles.
   - `enemies/enemies.json` & `enemies/enemy_spells.json` — enemy/boss definitions and movesets.
   - `environments/environments.json` — combat arena/backdrop definitions.
   - `scenes/` / `chapters/` — branching story and encounter JSONs.
   - `sprites/sheets/` — animated Aseprite `.png` + `.json` sprite sheets.

---

## 2. Author Character Profiles, Enemies & Environments First

> **Important:** Author your `characters/*.json`, `enemies/enemies.json`, `enemies/enemy_spells.json`, and `environments/environments.json` **before** launching VoiceRPG Art Studio or running any asset generator.

Art Studio's Plan Generator reads these JSON files directly to decide what assets to queue, what role/cell size each character gets, and how to prompt the image/video models:

| File | Fields Read by Art Studio & Engine | Output Role / Effect |
|---|---|---|
| `characters/<id>.json` | `id` (filename stem), `name`, `title`, `_doc` | Queued as `party` (`64×64` sheet); `name`, `title`, `_doc` form the visual prompt. (`narrator*`, `hiring_portal`, `template*` are skipped.) |
| `enemies/enemies.json` | `id`, `name`, `subtitle`, `movesetId` | Queued as `enemy` (`64×64`) or `boss` (`128×128` when `id` contains `boss`, `manager`, or `auditor`). `template_*` and `practice_dummy` are skipped. |
| `enemies/enemy_spells.json` | `movesets.<movesetId>.moves` | Defines enemy combat moves and action tags. |
| `environments/environments.json` | `id` / `name` | Queued as `combat_backdrop` (`1344×768` `.png`); generic fantasy names (`forest`, `dungeon`, `swamp`, etc.) are automatically flagged for re-theming in non-fantasy games. |

---

## 3. Combat Content Reference

How a game's fights work is data. Every field below is optional, with a default that changes nothing. The design
behind them is [`COMBAT_PROGRESSION_PLAN.md`](COMBAT_PROGRESSION_PLAN.md).

### Mana or none (`manifest.json`)
| Field | Values | Effect |
|---|---|---|
| `combatResource` | `"mana"` (default) / `"none"` | Without mana: no MP, no spell costs, no attune (its words are ordinary words), spells that only restore mana are left out, and nothing on screen or in speech mentions mana. `CombatResourceContractTest` checks the content matches. |

### Spells learned from the story (classes and characters)
A fighter knows their starting kit plus every grant whose story flag is set, worked out at the start of each
battle; a fighter joining late arrives knowing what the story has already passed. Spells are only added.

| Where | Field | Meaning |
|---|---|---|
| `classes/<id>.json` | `starterSpellIds` | The hero's starting kit for that class. |
| `classes/<id>.json`, `characters/<id>.json` | `spellGrants` | `[ { "flag": "ch5_done", "spellIds": ["frost_spike"] } ]`: learned when the flag is set ("Lyra learns Frost Spike" is said and shown). |
| `characters/<id>.json` | `joinWhenFlag` | The story flag at which the character joins the party. |

A hero without a class uses its character file's combat spells and grants.

### Heals, restores and targets (`spells/spells.json`)
| Field | Meaning |
|---|---|
| `"target": "ally"` | Heals or restores the party member named ("Mending Word Lyra"); an enemy named, or none, means the caster. |
| `healPctOfMax` | Heals this share of each target's max HP (0.2 = 20%), times the resonance multiplier; with `hitsAll` it is a party heal. |
| `manaRestorePct` | Restores this share of max MP (mana games); with `"target": "ally"` or `hitsAll` it restores others. |
| `piercing` | Ignores the target's `armor`. |

### Weaknesses, resistances and armour (`enemies/enemies.json`, `combat/affinity_matrix.json`)
| Where | Field | Meaning |
|---|---|---|
| enemy | `weak`, `resist` | School ids (`PYROMANCY`, `CRYOMANCY`, `ELECTROMANCY`, `NATURE`, `HOLY`, `SHADOW`, `PHYSICAL`): x2.0 / x0.8 for that school, overriding the family table. |
| enemy | `armor` | Flat damage taken off every hit (`piercing` spells ignore it). |
| encounter `bossPhases[]` | `weak`, `resist` | A phase changes the boss's weaknesses ("the aura turns to frost"). |
| `affinity_matrix.json` | `schoolNames`, `familyNames` | The game's own names for schools and families in speech and on screen (ids stay). |

Players learn weaknesses by hitting: a school's effect is remembered per enemy type in the save (the bestiary) and
the "Enemies" voice report adds what's known. Enemies never heal or restore (a drain does damage only).

### Encounters, routes and warps
| Where | Field | Meaning |
|---|---|---|
| encounter | `"solemn": true`, `endLine` | No "Weak!" or effectiveness callouts, taunts or victory fanfare; the fight ends on `endLine`. For moments the game must not make gamey. |
| `manifest.json` | `routeFlagPrefix` | Story flags starting with it pick a route (e.g. `role_` for departments); warps stay on the save's route and ask for one before it is chosen. Campaign tracks are routes too. |
| story node | `checkpoint` | A warp target label ("Shift 11"); without any, warps list scene changes in play order. A warp replays the story in memory to the checkpoint. |

### Balance and difficulty (`combat/balance.json`)
```json
{
  "balanceResonance": 1.5,
  "difficulty": "normal",
  "difficulties": { "hard": { "enemyHp": 1.3, "enemyAttack": 1.2 } },
  "enforce": 1
}
```
| Field | Meaning |
|---|---|
| `balanceResonance` | The resonance multiplier fights are balanced at (x1.5: the lower end of the mid range). |
| `difficulty`, `difficulties` | The preset in play and named presets scaling every story enemy's HP and attack (`normal` is always 1.0). |
| `enforce` | 1 = `BalanceContractTest` fails the build when a fight is off target; without it the report is still written. |

`BalanceContractTest` plays every story fight on every route (campaign track, department or hero class) with the
party and spells the story has given them at that point (`BattleSimulator`: the real combat view model on
virtual time) and writes `app/build/reports/balance.txt`. Its targets, for a sensible player at
`balanceResonance` (the median across a fight's routes):

| Fight | Rounds to win | Safety margin (rounds to wipe / rounds to win) |
|---|---|---|
| Early regular | 2-5 | 2.0-4.5 |
| Mid regular | 3-6 | 1.4-3.0 |
| Late regular | 3-7 | 1.15-2.2 |
| Boss | 5-11 | 1.1-2.0 (late 1.05-1.6) |
| Finale | 9-15 | 1.0-1.4 |

It also checks that a lazy x1.1 player can lose late bosses, a bold x1.8 player wins early fights easily, spamming
the strongest spell is clearly worse late on, no more than a third of turns go on healing, nothing loops, every
attack spell is the best choice somewhere, and Pure Story Mode wins every fight. To tune a game's fights:

```bash
# Suggest enemy HP and attack per story fight, then write them into encounters.json
VB_BALANCE_TUNE=1 VB_TEST_FILTER="*BalanceTunerTest*" ./scripts/test_content_packs.sh <game>
python3 scripts/apply_balance_tuning.py ../<game> "${TMPDIR:-/tmp}/vb-content-packs/<game>/app/build/reports/balance-tuning.json"
```

## 4. Validate Content Packs & Build the APK

Before shipping or testing on device, verify your content packs and 16 KB Android alignment:

```bash
# Validate JSON schemas and sprite contracts across content packs
./scripts/test_content_packs.sh

# Run engine unit tests + Balance Harness
./gradlew testDebugUnitTest

# Build APK and verify 16 KB page-size alignment
./gradlew assembleDebug
./scripts/check_16kb.sh app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Optional: Generating Graphical Assets with VoiceRPG Art Studio

If you want to generate consistent 16-bit pixel-art battle sprite sheets and combat backdrops locally instead of drawing them by hand, you can use the supplemental **VoiceRPG Art Studio** addon ([`addons/art_studio/`](../../addons/art_studio/README.md)) and [`scripts/generate_battle_sprites.py`](../../scripts/generate_battle_sprites.py).

### How the Pipeline Works

Art Studio uses a dual-machine (or single-workstation) queue aligned with the **Echoes of Logos** pipeline:

1. **Key-Pose Sheet (`FLUX.2 Klein`, Laptop queue):**
   Generates a unified `4×2` (`1024×512`) pixel-art key-pose sheet on flat `#FF00FF` magenta (`idle`, `ready`, `charge`, `cast`, `attack`, `hurt`, `ko`, `victory` for party; `idle`, `windup`, `attack`, `cast`, `hurt`, `death`, plus `charge_ultimate`/`ultimate` for bosses) and extracts `256×256` floor-aligned poses.
2. **Action & Idle Interpolation (`Wan 2.2-14B FLF`, Desktop queue):**
   Feeds start and end key-pose frames into `WanFirstLastFrameToVideo` (`Wan 2.2-14B` + 4-step Lightning LoRAs) for every animation tag and extracts the exact keyframes.
3. **Aseprite Packing (Laptop queue):**
   Keys and despills `#FF00FF` magenta, quantizes all frames across all tags to a **shared 24-color palette**, and packs `<id>.png` + `<id>.json` (with `frameTags`, `release`/`impact` events, and `feet`/`hit`/`cast`/`head` slices) into `app/src/main/assets/game/sprites/sheets/` plus a strip preview in `art/sprite-previews/<id>_preview.png`.
4. **Combat Backdrops (`SDXL + pixel-art-xl`, Laptop queue):**
   Generates `1344×768` pixel-art battle arena backdrops into `app/src/main/assets/game/environments/<id>.png`.

### Setup & Usage

1. **Install dependencies:**
   ```bash
   ./addons/art_studio/install.sh
   ./addons/art_studio/install.sh --check   # Verify tools & ComfyUI models
   ```
2. **Launch the Art Studio dashboard:**
   ```bash
   ./addons/art_studio/run.sh --game ~/GitHub/your-game --port 8792
   ```
   Open `http://127.0.0.1:8792` in your browser.
3. **Generate & Run the Queue:**
   - Select your game and click **Generate Plan** (writes `art/art_queue.json` and syncs wave progress).
   - Use **Resume Both**, **Pause/Resume Laptop**, or **Pause/Resume Desktop** (pausing cleanly finishes the in-flight item before stopping).
   - Click any **completed** card to open its generated `.png` preview in a new browser tab.
