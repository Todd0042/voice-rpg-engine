# VoiceRPG Engine: Global Architecture & Multi-Repo Sync Protocol

This rule governs `voice-rpg-engine` and all derived game forks (`echoes-of-logos`, `wallys-world`, `commander-america-rpg`). Every AI agent interacting with this codebase or any game fork MUST strictly adhere to this protocol.

---

## 1. Upstream-First Rule (Game Forks Must Update the Engine)
- **Engine Logic Belongs Upstream**: The core engine packages (`com.voicerpg.engine.*`) and core shared assets (`app/src/main/assets/game/commands/`, `app/src/main/assets/game/localization/`) are owned by `voice-rpg-engine`.
- **Zero Forked Divergence**: If an AI agent or developer fixes a bug, improves performance, or adds an engine feature while working in a game fork (e.g. `echoes-of-logos`):
  1. The change **must** be committed upstream in `voice-rpg-engine`.
  2. You can use `./scripts/sync_engine_to_games.sh --pull-from <game_repo> --verify` from `voice-rpg-engine` to safely import and verify the change.
  3. Game repositories must **never** maintain private, divergent forks of `com.voicerpg.engine.*`.

---

## 2. Universal Downstream Propagation Rule (Engine Updates Push to All Games)
- **Automatic Distribution**: Whenever an update, bug fix, or refactor is made to `voice-rpg-engine`:
  1. Verify the engine test suite: `./gradlew testDebugUnitTest`.
  2. Run the distribution script to push the update to all downstream games:
     ```bash
     ./scripts/sync_engine_to_games.sh --verify
     ```
  3. Every game must cleanly accept the update. If a game's unit tests fail after sync, the engine change violated backwards-compatibility or broke a contract and must be fixed upstream.

---

## 3. Opt-In Features & Zero-Forced Adoption Principle
- **Games Do Not Have to Use All Features**: Downstream games are never required to adopt every engine feature (e.g. selfie avatar generation, parallel campaign tracks, mini-games, or offline translation banners).
- **Graceful Defaults & Backward Compatibility**: Any new engine capability, model field, or configuration must provide sensible default values (e.g. `null`, empty list, or disabled by default).
- Downstream games must be able to pull engine updates cleanly and maintain full compilation and test passing without modifying their game-specific logic or adopting features irrelevant to their design.

---

## 4. Strict Content Neutrality in the Engine
- **No Story or Character Leaks**: `voice-rpg-engine` must NEVER hardcode story chapters, specific character names, dialogue, encounters, or custom art/audio.
- All game-specific narrative lives strictly in JSON files under `app/src/main/assets/game/**` in each respective game repository.

---

## 4. Periodic Divergence Audit
- To ensure no game has drifted from the engine, run:
  ```bash
  ./scripts/sync_engine_to_games.sh --check
  ```
  This command will report any divergent files across all games.
