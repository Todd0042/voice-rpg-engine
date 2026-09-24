# VoiceRPG Engine: Core Generality & Non-Subservience Principle

## 1. Engine Purpose: Mechanics ONLY, Content Modular
* **The VoiceRPG Engine is a standalone engine framework.**
* It exists **ONLY** to provide:
  1. Voice recognition (speech-to-text, acoustic pitch detection, keyword thesaurus).
  2. Tactical ATB / turn-based combat systems (spells, affinities, status effects, damage formulas).
  3. Interactive branching narrative trees (choice elimination, progression flags).
  4. Multi-voice Text-To-Speech (TTS) narration with per-character voice model & pitch binding.
  5. Multi-slot atomic save persistence and game state management.
* **The engine is NOT subservient to *Echoes of the Logos*, *Aethelgard*, or ANY individual game title.**
* It must be 100% capable of powering games in ANY genre:
  - High fantasy, sci-fi space opera, cyberpunk, wild west, political satire, modern detective thriller, horror, etc.

## 2. All Assets and Lore Must Be Replaceable
* When using this engine to build a new game, **ALL assets, characters, storylines, spells, and audio must be replaced**:
  - Replace `StoryScript.kt` with the new game's narrative scenes, branches, and nodes.
  - Replace `StoryEncounters.kt` and `ClassSpellLibrary.kt` with the new game's battles and spells.
  - Replace character portraits, scene backgrounds, music, sound effects, and app icons.
  - Register the new game's characters in the voice assignment system.

## 3. Strict Proscription: No Hardcoded Reference Lore in Engine Core
* Engine infrastructure files must NEVER hardcode game-specific character names or storylines:
  - `CombatNarrator.kt`: Must dynamically bind voices to whatever `DialogueSpeaker`s are in the active game.
  - `AudioSetupScreen.kt`: Must dynamically display the active game's cast badges rather than hardcoding static names.
  - `OptionsDialog.kt`: Must use generic text (e.g., "announces speaker names", "install character voices").
  - `CharacterCreationScreen.kt`: Must use generic fallbacks ("Hero", "Embark", "Begin Adventure").
  - `CombatViewModel.kt`: Must initialize fallbacks from `TemplateHero` or configured starter parties, never hardcoded reference characters.
