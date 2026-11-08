<p align="center">
  <a href="https://ko-fi.com/todd0042">
    <img src="https://raw.githubusercontent.com/Todd0042/creator-assets/main/banners/todd_creator_banner_v1.0.png" alt="Support Todd on Ko-fi" width="100%" />
  </a>
</p>

---

# 🎙️ VoiceRPG Engine: Native Android Voice-Controlled JRPG Framework

[![Version](https://img.shields.io/badge/Version-2.0.0-ff2a85.svg)](https://github.com/Todd0042/voice-rpg-engine/releases)
[![License: PolyForm Noncommercial](https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-00f0ff.svg)](LICENSE)
[![Platform: Android 8.0+](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![16 KB Page Size](https://img.shields.io/badge/16%20KB-Compatible-success.svg)](https://developer.android.com/16kb-page-size)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%202024.10.01-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-234%20Passing-39ff14.svg)](app/src/test/java/com/voicerpg/engine/)
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
  * [3. 5-Category Tactical Spell Combat & ATB Timeline (`CombatModels.kt`, `MovesetTable.kt`)](#3-5-category-tactical-spell-combat--atb-timeline)
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

**VoiceRPG Engine** is a turnkey, open-source Android framework engineered to power voice-controlled narrative and tactical JRPGs. 

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

---

## 🚀 Quick Download & Installation

1. Download the latest **`voice-rpg-engine-v2.0.0-debug.apk`** from [Releases](https://github.com/Todd0042/voice-rpg-engine/releases).
2. Install the APK on any Android 8.0+ device (API 26 or higher).
3. Grant **Microphone Permission** when prompted to enable hands-free voice commands.

---

# Part 2 — Consolidated Technical Documentation & Architecture Reference

## 1. Hands-Free Voice Recognition & Continuous State Loop

The engine relies on [`SpeechManager.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/audio/SpeechManager.kt) and [`IntentParser.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/engine/IntentParser.kt) to manage a hands-free conversational loop:
- **Zero-Overlap Audio Arbitration:** Android `TextToSpeech` narration and `SpeechRecognizer` listening never overlap. The microphone automatically arms the moment speech synthesis concludes.
- **Dynamic Entity Matching:** [`IntentParser.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/engine/IntentParser.kt) prioritizes live party instances (`party.any { memberMatchesUtterance }`) and active alive enemy instances before falling back to legacy archetype keywords.

---

## 2. Resonance Engine & Acoustic-Poetic Scoring Math

Spoken spell incantations are evaluated by [`ResonanceEngine.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/engine/ResonanceEngine.kt) using a compound acoustic and linguistic formula:

$$\text{Resonance Score} = S_{\text{acoustic}} + S_{\text{poetic}} + S_{\text{length}} + S_{\text{novelty}}$$

- **Acoustic Energy ($S_{\text{acoustic}}$):** Peak decibel volume (up to 30 pts) and dynamic pitch contour variation (up to 25 pts).
- **Poetic Roots ($S_{\text{poetic}}$):** Thesaurus roots matched via [`SpellThesaurus.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/engine/SpellThesaurus.kt) (+10 to +22 pts).
- **Cadence & Length ($S_{\text{length}}$):** Word count tiers reward deliberate cadence ($\le 2$: 0 pts, $\le 4$: 10 pts, $\le 7$: 20 pts, $\le 11$: 32 pts, $> 11$: 45 pts).
- **Tiers & Multipliers:**
  - **NOVICE (0–39%):** 1.0x power
  - **ADEPT (40–74%):** 1.25x power
  - **MASTER (75–99%):** 1.5x power
  - **MYTHIC (100–150%):** 2.25x power
  - **TRANSCENDENTAL ($\ge 155\%$):** 3.0x power

---

## 3. 5-Category Tactical Spell Combat & ATB Timeline

All character and enemy abilities inherit from standardized categories defined in [`CombatModels.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/model/CombatModels.kt):
- `ATTACK`: Direct single-target damage spells.
- `AOE_ATTACK`: Full-flank sweep attacks.
- `HEAL`: Restorative recovery spells (party-wide with 0.65 split).
- `DEFENSE`: Damage mitigation, shields, and party buffs.
- `MANA`: Recovery and attunement spells.

---

## 4. Opt-In Multi-Track Campaign Architecture

Games can define parallel campaign branches using `CampaignTrackDef` in [`SaveModels.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/model/SaveModels.kt) registered via `GameContent.campaignTracks`:
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

[`BuiltInDictionaries.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/localization/BuiltInDictionaries.kt) bundles complete offline term matrices for Spanish (`es`), German (`de`), French (`fr`), Portuguese (`pt`), and Italian (`it`). The dynamic [`LanguageModelDownloadBanner.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/ui/setup/LanguageModelDownloadBanner.kt) allows on-demand Google MLKit model downloading with immediate offline fallback.

---

## 6. MLKit & TFLite Selfie Character Creation

Integrated on-device selfie segmentation and pixel-art synthesis:
- **`SelfiePortraitProcessor.kt`**: Extracts face contours using Google MLKit Selfie Segmentation.
- **`AvatarSynthesizer.kt`**: Applies retro 16-bit color palette quantization and pixel dithering to generate custom player sprites.

---

## 7. Scaffolding Templates for New Game Development

The `com.voicerpg.engine.template` package provides clean starter blueprints for rapid game creation:
- [`TemplateHero.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/template/TemplateHero.kt): Hero stats, starting kits, and affinities.
- [`TemplateEnemy.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/template/TemplateEnemy.kt): Enemy archetypes and boss phases.
- [`TemplateEncounters.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/template/TemplateEncounters.kt): Turn-based encounter structures.
- [`TemplateStory.kt`](file:///home/todd/Documents/GitHub/voice-rpg-engine/app/src/main/java/com/voicerpg/engine/template/TemplateStory.kt): Branching dialogue trees and choice hubs.

---

## 8. Multi-Repo Distribution Pipeline

The included sync utility [`scripts/sync_engine_to_games.sh`](file:///home/todd/Documents/GitHub/voice-rpg-engine/scripts/sync_engine_to_games.sh) enables one-command distribution of engine enhancements to derived game repositories:
```bash
# Preview changes across all downstream games:
./scripts/sync_engine_to_games.sh --dry-run

# Push updates to a specific game repository:
./scripts/sync_engine_to_games.sh /path/to/commander-america-rpg
```

---

## 9. Automated Unit Verification Suite

The engine features an automated suite of **234 unit tests** covering intent parsing, multilingual resonance incantations, ATB timeline priority, and save state integrity:
```bash
# Run full unit test suite:
./gradlew testDebugUnitTest

# Build debug APK:
./gradlew assembleDebug
```

---

## 10. Clean-Room Provenance & Non-Commercial Licensing

- **100% Original Framework:** Zero copyrighted game assets or proprietary game logic bundled in the core engine.
- **PolyForm Noncommercial 1.0.0:** Free for personal, educational, and hobbyist game development. For commercial inquiries, contact **[todd.0042.git@gmail.com](mailto:todd.0042.git@gmail.com)**.

---

<p align="center">
  If you enjoy building games with VoiceRPG Engine, consider supporting open-source development:<br/><br/>
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
