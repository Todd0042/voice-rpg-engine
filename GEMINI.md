# VoiceRPG Engine: Agent Guidelines & Engineering Rules

# VoiceRPG Engine: Agent Guidelines & Engineering Rules

This repository is the **standalone, reusable engine** for voice-commanded JRPGs on Android (Kotlin, Jetpack Compose, Android Speech & TTS APIs). All game content has been stripped out and replaced with **drop-in JSON templates** under `app/src/main/assets/game/`. When working on this codebase, adhere strictly to the rules below.

---

## 1. Content Neutrality (THE core rule)

- **Engine code never references game content.** `app/src/main` must not hardcode character names, story chapters, spell names, dialogue, encounters, or art/music filenames. The completed game (`echoes-of-logos` / *Echoes of the Logos*) is a **separate repo and must never be reintroduced here**.
- All content lives in `app/src/main/assets/game/**` as JSON, loaded at runtime by `GameContent`:
  - `manifest.json` (title, starting scene/node, default encounter, victory/defeat lines)
  - `characters/*.json`, `classes/*.json`, `spells/spells.json`
  - `enemies/enemies.json`, `encounters/encounters.json`
  - `story/scenes.json`, `story/nodes.json`
- Bundled art/music referenced by those JSON files live under `app/src/main/assets/{portraits,story,audio,sprites}`.
- New game content = **JSON + assets only**. If a change requires touching `app/src/main`, it must be a genuinely generic engine improvement, not content.

## 2. Tests: Contract Tests and Mechanics Tests

The unit suite (`./gradlew testDebugUnitTest`) runs in the engine **and in every game**, on that game's
content. It must stay green for all four games. Two kinds of test:

1. **Contract tests** run on the content that's loaded (the game's own `app/src/main/assets/game`). They
   assert engine-contract invariants that every game must meet: every node link resolves, no dead ends
   except `game_completed`, every choice has a `voiceKeyword`, every battle trigger resolves, scenes and
   hub flags resolve, full reachability (incidents count as entry points), assets referenced by JSON
   exist, chant calibration of the game's spells. **Never assert specific content in a contract test**:
   iterate over whatever is loaded.
2. **Mechanics tests** check engine behaviour on known data (a named encounter, hero, spell, node). They
   use the frozen template fixture, `app/src/test/fixtures/template-pack/game`, through
   `@get:Rule val templateFixture = TemplateFixture()` (see `testing/TemplateFixture.kt`), so they behave
   the same in every game. Never point a mechanics test at the game's content, and never edit the
   fixture to suit one game.

Before committing an engine change, also run **`./scripts/test_content_packs.sh`**: it runs the whole
suite against each game's content (engine working tree + the game's assets, in a throwaway copy). A
failure there means the change breaks a game, or a test assumes template data.

## 3. Story Graph Conventions (engine contract)

- **No endless loops:** hub choices set a `completionFlag`, completed choices are eliminated on return, and a hub whose sub-stories are all complete auto-redirects via `hubCompletion.requiredFlags` + `hubCompletion.redirectToNodeId`.
- **No dead ends:** every node must continue via `nextNodeId`, `choices`, `hubCompletion`, or a battle trigger (`triggerBattleEncounterId` + `nextNodeId`). The **only** terminal node is one that sets `game_completed`.
- Scenes bind background + music + entry node; `chapterIndex`/`actNumber`/milestone fields feed the recap and chapter-warp menu — nothing hardcoded.

## 4. Resonance Calibration Invariant

- `SpellChantPresets` builds generic per-tier chants from any spell name; `AcousticProfile` constants per tier are fixed.
- The engine's thematic matcher (root lexicon in `SpellThesaurus`) must cover each spell's name with **exactly one** root word. Zero-root names are unreachable at TRANSCENDENTAL; two-root names overflow BASIC. Keep the lexicon per-school and generic; `testAllSpellChantPresetsAcrossAllResonanceTiers` guards this for the whole spell catalog.
- **Resonance is sacred:** it is the only skill multiplier (1.0x–3.0x). Affinity and stats may never exceed or replace it.

## 5. ViewModel Tests: The Settle Pattern (livelock guard)

Driving `StoryViewModel`/`TutorialBattleViewModel`/`CombatViewModel` in tests that rapidly cancel + relaunch animation jobs on the same event loop can livelock kotlinx-coroutines' `LockFreeLinkedList.finishAdd` and hang the suite for minutes. Follow the pattern in `TutorialBattleTest`: advance state, then **wait for a ref/state change and settle** (small `delay` loop) before the next step. Never `advance()` in a hot loop without a settle.

## 6. Voice & Audio-First (retained engine pillars)

- All dialogue and combat actions must be operable hands-free: TTS narration, `Read choices`, meta-commands ("Status", "Enemies", "Options", "Pocket mode", "Auto listen", "Toggle narration"), and auto-listen re-arm after TTS.
- Bind distinct physical TTS voices per character via `android.speech.tts.Voice`; characters declare `preferredVoiceId` (fallback: pitch/rate modulation). Only characters with a non-null `preferredVoiceId` appear in `GameContent.preferredVoiceIds` / `CombatNarrator.PREFERRED_VOICE_IDS`.

## 7. Verification Before Any Commit

- Always run the full unit suite: `./gradlew testDebugUnitTest` (must be clean).
- Compilation of engine code: `./gradlew compileDebugKotlin` (must be clean).
- **16 KB page size** (Play blocks apps that fail it): after adding or upgrading any dependency, build the
  APK and run `bash scripts/check_16kb.sh app/build/outputs/apk/debug/app-debug.apk` (CI runs it too).
  A failing `.so` comes from a dependency: upgrade it to a 16 KB-aligned version (TensorFlow Lite was
  replaced by LiteRT for this reason). Never ship a 4 KB-aligned library.
- GitHub releases / ADB device installs are the **game project's** choice; do not auto-create releases or install without being asked.

## 8. Unique App Identity (every app is its own unique game)

- **This repository builds a unique reference app** — the engine's own sandbox for exercising and testing the framework. Its `applicationId` (`com.voicerpg.engine.reference`) is **reserved** to this repo and must never be shipped as a game.
- **Every app built from this engine MUST be its own unique Android app and its own unique game:** its own `applicationId` in `app/build.gradle.kts`, its own app label/icon in `app/src/main/res/`, and its own game content (JSON + assets under `app/src/main/assets/game/`). Never ship an app that keeps the engine's reference `applicationId`, the engine's app name, another game's `applicationId`, or another game's name/branding.
- **Why:** Android treats `applicationId` as the install identity. Two APKs that share an `applicationId` silently replace each other on the same device — the newest install overwrites the existing one and wipes its app data. This is exactly what happened to *Commander America* when the engine reference app was installed: both used `com.voicerpg.engine`, so building this repo replaced that game on the phone.
- **Before building or installing any derivative:** change `applicationId` (and, when feasible, the `namespace`) in `app/build.gradle.kts` to an identifier you own, and give the app its own label, icon, and content. Reusing the engine's reference id **or** any other game's `applicationId` is a rule violation — the resulting app must never overwrite or impersonate another VoiceRPG title.

---

## 9. One Engine, Every Game (sync protocol)

Every game (`echoes-of-logos`, `wallys-world`, `commander-america-rpg`, `superstore-saga`) runs the
**identical** engine. The engine owns these paths in each game (`ENGINE_OWNED_PATHS` in
`scripts/sync_engine_to_games.sh`): `app/src/main/java/com/voicerpg/engine/**` (MainActivity included),
`assets/game/commands/**`, `assets/game/localization/**`, `app/src/test/java/com/voicerpg/engine/**` and
`app/src/test/fixtures/template-pack/**`. In a game they are read-only.

- **Engine changes happen here, never in a game.** Keep them content-neutral: a game needs different
  behaviour → a JSON field with a default that leaves the other games unchanged (opt-in, §3 of the
  sync protocol).
- **Workflow:** change → `./gradlew testDebugUnitTest` → `./scripts/test_content_packs.sh` → commit →
  `./scripts/sync_engine_to_games.sh --verify` → commit the sync in each game ("Sync engine <hash>").
- **Push mirrors exactly** (checksums, stray files removed) and records `.engine-sync/ENGINE_VERSION` and
  `MANIFEST.sha256` in the game. It refuses an engine with uncommitted changes, a game branch with no
  engine (not migrated), uncommitted engine edits in the game, and files in engine paths the engine
  doesn't have (game tests go in `app/src/test/java/com/voicerpg/game/`).
- **Guards in each game:** `.engine-sync/guard.sh` in CI (`--ci`) and as a pre-commit hook (`--staged`)
  reject engine files that don't match the recorded engine version, and hand-edited manifests.
- **Found an engine change in a game?** Review it with `--diff <game>`; bring over only what belongs
  upstream with `--pull-from <game> <path>...`; make it content-neutral; then follow the workflow.
- **Audit:** `./scripts/sync_engine_to_games.sh --check` must report every game as in sync.
