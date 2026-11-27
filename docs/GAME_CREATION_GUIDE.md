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

## 3. Validate Content Packs & Build the APK

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

## 4. Optional: Generating Graphical Assets with VoiceRPG Art Studio

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
