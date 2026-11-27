# VoiceRPG Art Studio (`addons/art_studio`)

A supplemental web UI and dual-machine resumable queue for generating battle sprite sheets and combat backdrops for any **VoiceRPG Engine** game repository.

See [`docs/GAME_CREATION_GUIDE.md`](../../docs/GAME_CREATION_GUIDE.md) for the full game-creation workflow.

## Prerequisites

Before running **Generate Plan** in Art Studio, your target game repo must already have:
- `app/src/main/assets/game/characters/*.json` (`name`, `title`, `_doc`)
- `app/src/main/assets/game/enemies/enemies.json` & `enemy_spells.json` (`id`, `name`, `subtitle`, `movesetId`)
- `app/src/main/assets/game/environments/environments.json` (`id`, `name`)

## Installation

```bash
# Install system tools (python3, ffmpeg, openssh-client) + Python venv deps
./addons/art_studio/install.sh

# Check tools and ComfyUI / graphical-studio models
./addons/art_studio/install.sh --check
```

## Running

```bash
./addons/art_studio/run.sh --game ~/GitHub/superstore-saga --port 8792
```

Then open `http://127.0.0.1:8792`:
- **Left Pane (Laptop Queue):** `FLUX.2 Klein` `4×2` key-pose sheets (`base:<id>`), 24-color Aseprite packing (`sheet:<id>`), and `SDXL` combat backdrops (`bg:<id>`).
- **Right Pane (Desktop Queue):** `Wan 2.2-14B` `WanFirstLastFrameToVideo` animations (`wan:<id>`) over SSH (`ART_DESKTOP_SSH`, default `todd@192.168.1.236`).
- **Controls:** **Generate Plan**, **Pause/Resume Both**, **Pause/Resume Laptop**, **Pause/Resume Desktop**, and click any completed card to preview the generated asset in a new tab.

## Tests

```bash
PYTHONPATH=. python3 -m unittest addons.art_studio.test_art_studio_e2e -v
```
