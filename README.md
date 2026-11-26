<p align="center">
  <a href="https://ko-fi.com/todd0042">
    <img src="https://raw.githubusercontent.com/Todd0042/creator-assets/main/banners/todd_creator_banner_v1.0.png" alt="Support Todd on Ko-fi" width="100%" />
  </a>
</p>

---

# 🎙️ VoiceRPG Engine: Native Android Voice-Controlled JRPG Framework

[![Version](https://img.shields.io/badge/Version-2.0.0-ff2a85.svg)](https://github.com/Todd0042/voice-rpg-engine/releases)
[![License: PolyForm Noncommercial 1.0.0](https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-00f0ff.svg)](LICENSE)
[![Platform: Android 8.0+](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![16 KB Page Size](https://img.shields.io/badge/16%20KB-Compatible-success.svg)](https://developer.android.com/16kb-page-size)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%202024.10.01-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/Todd0042/voice-rpg-engine/actions/workflows/android_ci.yml/badge.svg)](https://github.com/Todd0042/voice-rpg-engine/actions)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-314%20Passing-39ff14.svg)](app/src/test/java/com/voicerpg/engine/)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Creator-F16061?style=flat&logo=ko-fi&logoColor=white)](https://ko-fi.com/todd0042)

> **A high-performance, modular native Android game engine designed for building audio-first, voice-commanded retro JRPGs. Every aspect of gameplay—incantations, tactical ATB party combat, visual novel exploration, dialogue choices, and system toggles—can be operated hands-free via natural voice or via retro touch controls.**

```
       _________________________________________________________________________
      |  ARCHMAGE IGNIS                            HP [████████████░░░]  80%   |
      |  "By the living flame, incinerate!"        MP [███████████████] 100%   |
      |_________________________________________________________________________|
      |  RESONANCE: MYTHIC (138%) >>> SPELL MULTIPLIER: 2.25x BASE DAMAGE       |
      |  TACTICAL TAGS: [ATTACK] [AOE] [HEAL] [DEFENSE] [MANA]                  |
      |_________________________________________________________________________|
```

---

## 📖 Table of Contents

* **Part 1 — Consumer Overview & Quick Start**
  * [🎮 What Is VoiceRPG Engine?](#-what-is-voicerpg-engine)
  * [⚡ How a Voice JRPG Works in 3 Steps](#-how-a-voice-jrpg-works-in-3-steps)
  * [✨ Core Engine Highlights at a Glance](#-core-engine-highlights-at-a-glance)
  * [📱 Screenless Pocket Mode & Accessibility](#-screenless-pocket-mode--accessibility)
  * [🚀 Quick Download & Installation](#-quick-download--installation)
* **Part 2 — Consolidated Technical Documentation & Architecture Reference**
  * [1. Hands-Free Voice Recognition & Continuous State Loop (`SpeechManager.kt`, `IntentParser.kt`)](#1-hands-free-voice-recognition--continuous-state-loop)
  * [2. Resonance Engine & Acoustic-Poetic Scoring Math (`ResonanceEngine.kt`, `SpellThesaurus.kt`)](#2-resonance-engine--acoustic-poetic-scoring-math)
  * [3. 5-Category Tactical Spell Combat, ATB Timeline & Battle Effects (`CombatModels.kt`, `MovesetTable.kt`, `BattleCue.kt`, `ui/fx/`)](#3-5-category-tactical-spell-combat-atb-timeline--battle-effects)
  * [4. Opt-In Multi-Track Campaign Architecture (`SaveModels.kt`, `GameContent.kt`)](#4-opt-in-multi-track-campaign-architecture)
  * [5. On-Device Translation Banner & Fallback Dictionaries (`BuiltInDictionaries.kt`)](#5-on-device-translation-banner--fallback-dictionaries)
  * [6. MLKit & TFLite Selfie Character Creation (`SelfiePortraitProcessor.kt`)](#6-mlkit--tflite-selfie-character-creation)
  * [7. Scaffolding Templates for New Game Development (`com.voicerpg.engine.template`)](#7-scaffolding-templates-for-new-game-development)
  * [8. Multi-Repo Distribution Pipeline (`scripts/sync_engine_to_games.sh`)](#8-multi-repo-distribution-pipeline)
  * [9. Automated Unit Verification Suite (`tests/`)](#9-automated-unit-verification-suite)
  * [10. Clean-Room Provenance & Non-Commercial Licensing](#10-clean-room-provenance--non-commercial-licensing)

---

# Part 1 — Consumer Overview & Quick Start

## 🎮 What Is VoiceRPG Engine?

**VoiceRPG Engine** is a turnkey, source-available Android framework engineered to power voice-controlled narrative and tactical JRPGs. 

While traditional mobile RPGs rely heavily on small on-screen buttons, menus, and finger taps, VoiceRPG Engine is built from the ground up around **natural vocal expression**:
1. **🗣️ Spoken Incantations**: Cast spells by speaking their names or reciting dramatic incantations with acoustic pitch and resonance evaluation.
2. **⚔️ Hands-Free Tactical Combat**: Issue strategic commands to your party (*"Cedric shield wall"*, *"Heal Lyra"*, *"Focus fire the archer"*) without ever touching the glass.
3. **💬 Voice Dialogue Navigation**: Explore branching visual novel scenes and choose dialogue prompts naturally by speaking visible keywords.
4. **🚶 100% Screenless Pocket Mode**: Slip your phone into your pocket, plug in earbuds, and play entire story chapters and combat encounters using spoken audio narration and voice commands.

---

## ⚡ How a Voice JRPG Works in 3 Steps

1. **Speak to Cast or Command**: During battle or story scenes, the engine's continuous auto-listen loop listens for spell names, party directives, or choice keywords.
2. **Resonance Multiplier Evaluates Performance**: In combat, speaking with rhythm, inflection, and poetic descriptors triggers the **Resonance Engine**, boosting spell power from 1.0x (Novice) up to 3.0x (Transcendental).
3. **Dynamic TTS Narration Responds**: Rich multi-voice Android Text-To-Speech narrates story beats, companion reactions, and combat outcomes with zero cloud latency.

---

## ✨ Core Engine Highlights at a Glance

* **5 Tactical Spell Categories:** Standardized `[ATTACK]`, `[AOE]`, `[HEAL]`, `[DEFENSE]`, and `[MANA]` categories with high-contrast neon badges and quick-tap fallback buttons.
* **Dual Campaign Track Support:** Built-in engine support for branching parallel story tracks (e.g. Hero Track vs. Villain Track) selectable from the Title Screen.
* **Multilingual On-Device Translation:** Offline fallback translation engine covering English, Spanish, German, French, Portuguese, and Italian with zero required downloads.
* **MLKit / TFLite Selfie Synthesis:** Players can take a selfie and transform their face into a retro 16-bit pixel portrait.
* **Adaptive 2-Row Party Formations:** Dynamic frontline and backline battle flairs scaling automatically to party size.

---

## 📱 Screenless Pocket Mode & Accessibility

Designed for commuters, walkers, and visually impaired players, **Pocket Mode** turns the display into an ultra-low-power OLED black touch-guard while routing all narration and audio through headphones. Spoken meta-commands (*"Status"*, *"Enemies"*, *"Fellowship"*, *"Help"*) allow players to inspect party health, mana, and enemy threats completely eyes-free.

**Reduce Motion** and **Reduce Flashing** (Options → Pocket & Accessibility) keep battles comfortable: no shake or zoom and only a brief freeze, and no full-screen flash (flashes are always limited to three a second). Both are saved with the game like the audio options. In pocket mode, cut-in quotes and boss warnings are spoken.

---

## 🚀 Quick Download & Installation

1. Download the latest **`voice-rpg-engine-v2.0.0-debug.apk`** from [Releases](https://github.com/Todd0042/voice-rpg-engine/releases).
2. Install the APK on any Android 8.0+ device (API 26 or higher).
3. Grant **Microphone Permission** when prompted to enable hands-free voice commands.

---

# Part 2 — Consolidated Technical Documentation & Architecture Reference

## 1. Hands-Free Voice Recognition & Continuous State Loop

The engine relies on [`SpeechManager.kt`](app/src/main/java/com/voicerpg/engine/audio/SpeechManager.kt) and [`IntentParser.kt`](app/src/main/java/com/voicerpg/engine/engine/IntentParser.kt) to manage a hands-free conversational loop:
- **Zero-Overlap Audio Arbitration:** Android `TextToSpeech` narration and `SpeechRecognizer` listening never overlap. The microphone automatically arms the moment speech synthesis concludes.
- **100% On-Device Speech Processing:** Employs `EXTRA_PREFER_OFFLINE` with hardware-accelerated on-device recognition (`SpeechRecognizer.createOnDeviceSpeechRecognizer`) on Android 12+ (API 31+) to guarantee privacy, zero cloud roundtrips, and offline gameplay.
- **Dynamic Entity Matching:** [`IntentParser.kt`](app/src/main/java/com/voicerpg/engine/engine/IntentParser.kt) prioritizes live party instances (`party.any { memberMatchesUtterance }`) and active alive enemy instances before falling back to legacy archetype keywords.

---

## 2. Resonance Engine & Acoustic-Poetic Scoring Math

Spoken spell incantations are evaluated by [`ResonanceEngine.kt`](app/src/main/java/com/voicerpg/engine/engine/ResonanceEngine.kt) using a compound acoustic and linguistic formula:

$$\text{Resonance Score} = S_{\text{acoustic}} + S_{\text{poetic}} + S_{\text{length}} + S_{\text{novelty}}$$

- **Acoustic Energy ($S_{\text{acoustic}}$):** Peak decibel volume (up to 30 pts) and dynamic pitch contour variation (up to 25 pts).
- **Poetic Roots ($S_{\text{poetic}}$):** Thesaurus roots matched via [`SpellThesaurus.kt`](app/src/main/java/com/voicerpg/engine/engine/SpellThesaurus.kt) (+10 to +22 pts).
- **Cadence & Length ($S_{\text{length}}$):** Word count tiers reward deliberate cadence ($\le 2$: 0 pts, $\le 4$: 10 pts, $\le 7$: 20 pts, $\le 11$: 32 pts, $> 11$: 45 pts).
- **Tiers & Multipliers:**
  - **NOVICE (0–39%):** 1.0x power
  - **ADEPT (40–74%):** 1.25x power
  - **MASTER (75–99%):** 1.5x power
  - **MYTHIC (100–150%):** 2.25x power
  - **TRANSCENDENTAL ($\ge 155\%$):** 3.0x power

---

## 3. 5-Category Tactical Spell Combat, ATB Timeline & Battle Effects

All character and enemy abilities inherit from standardized categories defined in [`CombatModels.kt`](app/src/main/java/com/voicerpg/engine/model/CombatModels.kt):
- `ATTACK`: Direct single-target damage spells.
- `AOE_ATTACK`: Full-flank sweep attacks.
- `HEAL`: Restorative recovery spells (party-wide with 0.65 split).
- `DEFENSE`: Damage mitigation, shields, and party buffs.
- `MANA`: Recovery and attunement spells.

### Battle cues & effects (`BattleCue.kt`, `CombatTiming.kt`, `ui/fx/`)

Combat logic and what the player sees are separate layers. [`CombatViewModel`](app/src/main/java/com/voicerpg/engine/viewmodel/CombatViewModel.kt) decides outcomes and announces them as an ordered stream of [`BattleCue`](app/src/main/java/com/voicerpg/engine/combat/BattleCue.kt)s; the battle screen's [`BattleFxDirector`](app/src/main/java/com/voicerpg/engine/ui/fx/BattleFxDirector.kt) plays them and never changes game state. The full design is in [`docs/COMBAT_FEEL_PLAN.md`](docs/COMBAT_FEEL_PLAN.md).

| What happens | Cues | What the player sees and hears |
| :--- | :--- | :--- |
| **Voice charge-up** | `ChargeStart` / `ChargeLevel` / `ChargeEnd` | While you speak, the arena dims, a gold aura on the caster grows with your voice and a hum follows it; the resonance meter fills live. Typed chants get a short simulated charge. |
| **Cut-in** | `CutIn` | At MYTHIC and above, a diagonal panel slides across with the caster's portrait (or selfie portrait) and quote, the arena in slow motion behind it. Bosses can have one for their ultimate. |
| **The blow lands** | `Release`, `ResonanceReveal`, `Impact` | The projectile flies; the resonance banner and fanfare arrive *with* the hit. A freeze of 60-180 ms by tier, a white flash and knockback on each target, labelled numbers (WEAK!, RESIST, MYTHIC!), a shake toward the side that was hit. Transcendental adds slow motion, a zoom punch and a screen flash. Per-school impact sounds. |
| **Bold voices** | `Stagger`, `Streak` | A MASTER+ chant on a weakness pushes the enemy's turn back; a MYTHIC+ hit breaks a charging boss ultimate ("BROKEN!"). A streak of strong casts grows a badge and the fanfare climbs; it never changes damage. |
| **Enemy turns** | `Telegraph`, `EnemyMove`, `TitleCard`, `UltimateCharge`, `Taunt` | Enemies show what they'll do at 75% gauge, wind up, and name their moves on a title card. Boss ultimates charge for a turn, shown on the boss bar. Enemies taunt in speech bubbles. |
| **Deaths** | `Death` | Enemies shatter into their own pixels (or fade, or dissolve). |
| **Music** | `MusicIntensity` | An optional intensity track fades in when the party is low or a boss changes phase. |

- **One timing table.** Every wait in a turn comes from [`CombatTiming`](app/src/main/java/com/voicerpg/engine/combat/CombatTiming.kt): a Basic turn runs about 1.2 s from release to the next input, a Transcendental one about 3.1 s. Pocket mode keeps its own audio-paced values. `CombatTiming.INSTANT` lets tests run whole battles with no real time passing.
- **Frame-rate independent.** [`FxClock`](app/src/main/java/com/voicerpg/engine/ui/fx/FxClock.kt) runs effects on frame *time* (the same at 60 and 120 Hz), with hit-stop and slow motion for the whole arena.
- **One floor for everyone.** [`BattleStage`](app/src/main/java/com/voicerpg/engine/ui/combat/BattleStage.kt) defines the floor band (from the backdrop's horizon at 43% of the arena to the bottom edge); each combatant is placed by its feet inside it, back of the formation near the horizon, front at the bottom. However tall the console below (developer tools and preset chants make the arena much shorter), nobody stands in the sky. The backdrop picture moves with the stage: each environment in `environments.json` gives its picture's painted horizon (`artHorizon`, and `artHorizonPortrait` for the `_p` variant; default 0.55), and the picture is cover-cropped so that line is drawn exactly on the stage horizon at any arena size.
- **Gauges off the field.** [`BattleHud`](app/src/main/java/com/voicerpg/engine/ui/combat/BattleHud.kt) keeps the arena clear: the party's HP, MP and turn gauges are chips in the console's top row (tap one to pick that hero; the acting hero is outlined in gold), up to two bosses get a health bar across the top of the arena with their ultimate charge, and a regular enemy shows a thin health sliver only while targeted or for 2.5 s after a hit or heal. The tutorial battle shows the same chips.
- **Only the arena shakes and zooms**: the status bar and voice console stay still and readable. Messages (charging, mana, guards) sit on one status line above the console.
- **Repeatable.** Enemy plans and damage rolls use the view model's injectable `random`.
- **FX test bench**: with developer tools on, a row of buttons plays a cast at every tier, an area spell, an enemy hit and a boss ultimate on the current encounter, without touching the battle.

**Per-game tuning** in the optional `assets/game/combat/presentation.json` (every field optional; a game without the file gets these defaults):

```json
{
  "intensity": 1.0,
  "titleCardStyle": "banner",
  "damageNumberStyle": "pop",
  "labels": { "weak": "WEAK!", "resist": "RESIST", "crit": "{tier}!" },
  "cutIn": { "minTier": "MYTHIC" },
  "streak": { "label": "RESONANCE x{n}", "minTier": "ADEPT" },
  "telegraph": { "showIntentIcons": true, "bossUltimateChargeTurns": 1 },
  "deathEffect": "shatter",
  "music": { "intensityTrack": null, "lowHpThreshold": 0.3 }
}
```

| Field | Values |
| :--- | :--- |
| `intensity` | 0-2: scales shake, freeze and knockback |
| `titleCardStyle` | `banner`, `headline` (newspaper), `pa_announcement` (intercom), `scroll` (parchment) |
| `damageNumberStyle` | `pop`, `receipt` (paper slip), `comic` (yellow stamp) |
| `deathEffect` | `shatter`, `fade`, `dissolve` |
| `telegraph.bossUltimateChargeTurns` | 0-3 boss turns of charging before an ultimate fires |

Content files can add, all optional: `enemies.json` `scale` (bosses 2.0 by default), `taunts`, `hitReaction` (`flinch`, `stagger`, `comic_spin`), `cutIn`; `enemy_spells.json` moves `titleCard`, `windupMs`, `intentIcon`; characters' `combat.cutIn` (`portrait`, `color`, `quote`); `encounters.json` `intensityTrack`; sprite `frames` `attack`, `hurt`, `death`.

### Battle sprites & animation (`ui/sprites/`, `content/SpriteSheets.kt`)

Combatants are drawn from **Aseprite sheets** (a PNG plus Aseprite's JSON export in `game/sprites/sheets/<spriteId>.json`): 64 px side-view pixel art facing right (the engine mirrors the enemy side), drawn with nearest-neighbour filtering at a whole-number scale, its `feet` slice standing on the floor line. Games without sheets yet keep their text-grid sprites, which play through the same animation path. The plan and the contract each game must meet: [`docs/SPRITE_PLAN.md`](docs/SPRITE_PLAN.md).

- **Animations are tags** (`idle`, `ready`, `charge`, `cast`, `attack`, `hurt`, `ko`, `victory`; enemies `windup`, `death`, `taunt`; bosses `charge_ultimate`, `ultimate`, and `p2_…` after a phase shift). A missing tag falls back down a chain ending at `idle` ([`SpriteSources.kt`](app/src/main/java/com/voicerpg/engine/ui/sprites/SpriteSources.kt)).
- **Driven by the battle**: [`SpriteAnimator`](app/src/main/java/com/voicerpg/engine/ui/sprites/SpriteAnimator.kt) runs on the effects clock (hit-stop freezes the frame), and the director starts each animation from its cue. A cast or attack is raised and held on the frame before its event (`release=3` / `impact=3` in the tag's user data, counted from 1) until the spell actually leaves; an enemy's wind-up is stretched to its move's `windupMs`; the charge loop speeds up with the voice.
- **The sprite contract blocks release**: every party fighter, enemy and boss needs a 64 px sheet (bosses 64/96/128, each its own) with a `feet` slice, its role's animations and the `release`/`impact` events ([`SpriteContract`](app/src/main/java/com/voicerpg/engine/content/SpriteSheets.kt)). `GameSpriteContractTest` enforces it when the game sets `"spriteContract": 1` in `manifest.json`, and always in the release variant: the sync installs `.engine-sync/release_gate.gradle.kts` in each game, which makes `assembleRelease`/`bundleRelease` run `testReleaseUnitTest` first. `scripts/sprite_coverage.sh <game>` prints what a game still lacks.
- **Sprite lineup** (gallery → **SPRITES**, [`SpriteLineup.kt`](app/src/main/java/com/voicerpg/engine/ui/gallery/SpriteLineup.kt)): every battle sprite the game uses, once each, side by side on one floor line at one pixel scale, all playing the chosen animation in step, with height guides every 16 px, a plain or backdrop background, filters for party, bosses and enemies, and a ✓/✗ for the contract. Tap a sprite for its animations, cell, feet, users and contract problems.
- **Variants**: `game/sprites/sprites.json` recolours a sheet into a new enemy (`{"sprites": {"ember_wolf": {"base": "wolf", "palette": {"#3A3A3A": "#8A2B1A"}, "scale": 1.25}}}`).
- **Placeholders**: `{"orc": {"placeholder": "template_brute"}}` lets a game map its enemies to a sprite whose art is still being made; the battle draws the placeholder, the contract still counts it as missing, and the real `orc.json` takes over the moment it lands. A sprite's own sheet always wins over a variant base or a placeholder.
- **Boss size**: a sheet is drawn at its own size (bosses are drawn at 96 or 128 px), so a boss's default 2x enlargement applies only to legacy text grids.
- **`scripts/sprite_roster.py <game> [--apply]`**: a game's `art/sprite_roster.json` maps every party fighter, enemy and boss to a sprite (families with recolours, bosses with their own sprites, party members whose boss versions reuse their sheet, leftovers to remove) and holds the artist's descriptions. The script checks that it covers everyone, writes `art/SPRITE_BRIEFS.md`, and with `--apply` sets `enemies.json` `spriteId` and `sprites/sprites.json`.
- **`scripts/sprite_lint.py <game> [--fix]`** checks delivered sheets for what AI-made art gets wrong: labels baked into frames, a `feet` slice off the soles (floating or sinking sprites), wrong cell sizes, stray preview images in the APK (moved to `art/sprite-previews/`), and flags pure-magenta pixels for a look.

---

## 4. Opt-In Multi-Track Campaign Architecture

Games can define parallel campaign branches using `CampaignTrackDef` in [`SaveModels.kt`](app/src/main/java/com/voicerpg/engine/model/SaveModels.kt) registered via `GameContent.campaignTracks`:
```kotlin
data class CampaignTrackDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val initialSceneId: String,
    val initialNodeId: String,
    val description: String = ""
)
```
When populated, the Title Screen automatically displays a stylish track selector. When unconfigured, the engine defaults cleanly to standard linear progression.

---

## 5. On-Device Translation Banner & Fallback Dictionaries

[`BuiltInDictionaries.kt`](app/src/main/java/com/voicerpg/engine/localization/BuiltInDictionaries.kt) bundles complete offline term matrices for Spanish (`es`), German (`de`), French (`fr`), Portuguese (`pt`), and Italian (`it`). The dynamic [`LanguageModelDownloadBanner.kt`](app/src/main/java/com/voicerpg/engine/ui/setup/LanguageModelDownloadBanner.kt) allows on-demand Google MLKit model downloading with immediate offline fallback.

### Offline phrase dictionaries: engine UI vs. game story

[`TranslationManager.kt`](app/src/main/java/com/voicerpg/engine/localization/TranslationManager.kt) translates a phrase by exact match in two JSON dictionaries per language before falling back to ML Kit:

| File | Holds | Owned by |
|---|---|---|
| `assets/game/localization/strings_<lang>.json` | Phrases the engine's own screens show (menus, setup, combat UI): 403 per language | **Engine**: synced byte-identical into every game |
| `assets/game/translations/strings_<lang>.json` | The game's story, spell, enemy and place phrases | **Each game**: never synced; its entries win over the engine's |

`TranslationDictionaryContractTest` fails if the engine file picks up a phrase the engine never shows, so one game's story text can't ship in every game again.

---

## 6. MLKit & TFLite Selfie Character Creation

Integrated on-device selfie segmentation and pixel-art synthesis:
- **`SelfiePortraitProcessor.kt`**: Extracts face contours using Google MLKit Selfie Segmentation.
- **`AvatarSynthesizer.kt`**: Applies retro 16-bit color palette quantization and pixel dithering to generate custom player sprites.

---

## 7. Scaffolding Templates for New Game Development

The `com.voicerpg.engine.template` package provides clean starter blueprints for rapid game creation:
- [`TemplateHero.kt`](app/src/main/java/com/voicerpg/engine/template/TemplateHero.kt): Hero stats, starting kits, and affinities.
- [`TemplateEnemy.kt`](app/src/main/java/com/voicerpg/engine/template/TemplateEnemy.kt): Enemy archetypes and boss phases.
- [`TemplateEncounters.kt`](app/src/main/java/com/voicerpg/engine/template/TemplateEncounters.kt): Turn-based encounter structures.
- [`TemplateStory.kt`](app/src/main/java/com/voicerpg/engine/template/TemplateStory.kt): Branching dialogue trees and choice hubs.

---

## 8. Multi-Repo Distribution Pipeline

The included sync utility [`scripts/sync_engine_to_games.sh`](scripts/sync_engine_to_games.sh) enables one-command distribution of engine enhancements to derived game repositories:
```bash
# Preview changes across all downstream games:
./scripts/sync_engine_to_games.sh --dry-run

# Push updates to a specific game repository:
./scripts/sync_engine_to_games.sh /path/to/commander-america-rpg
```

---

## 9. Automated Unit Verification Suite

The engine features an automated suite of **333 unit tests** across 56 test files, covering intent parsing, multilingual resonance incantations, ATB timeline priority, save state integrity, and the JVM **Balance Harness** ([`BalanceHarnessTest.kt`](app/src/test/java/com/voicerpg/engine/combat/BalanceHarnessTest.kt)):
```bash
# Run full unit test suite (including headless Balance Harness):
./gradlew testDebugUnitTest

# Build debug APK:
./gradlew assembleDebug
```

---

## 10. Clean-Room Provenance & Non-Commercial Licensing

- **100% Original Framework:** Zero copyrighted game assets or proprietary game logic bundled in the core engine.
- **PolyForm Noncommercial 1.0.0:** Free for personal, educational, and hobbyist game development. For commercial inquiries, contact **[todd.0042.git@gmail.com](mailto:todd.0042.git@gmail.com)**.
- **Media License**: the images, artwork and audio made for this project (paths listed in [LICENSE-MEDIA](LICENSE-MEDIA)) are © Todd0042 under **[CC BY-NC 4.0](LICENSE-MEDIA)**: free to share and adapt with credit for non-commercial purposes; commercial use requires permission.

---

<p align="center">
  If you enjoy building games with VoiceRPG Engine, consider supporting its development:<br/><br/>
  <a href="https://ko-fi.com/todd0042" target="_blank">
    <img src="https://img.shields.io/badge/☕_Support_VoiceRPG_Engine_on_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support on Ko-fi">
  </a>
</p>

---

<p align="center">
  <a href="https://ko-fi.com/todd0042">
    <img src="https://raw.githubusercontent.com/Todd0042/creator-assets/main/banners/todd_creator_banner_v2.0.png" alt="Support Todd on Ko-fi" width="100%" />
  </a>
</p>
