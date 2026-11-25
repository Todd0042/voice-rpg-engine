#!/usr/bin/env bash
# Sprite coverage: which of a game's party fighters, enemies and bosses still miss the sprite contract
# (docs/SPRITE_PLAN.md §3). Runs the engine's GameSpriteContractTest on the game's own content and prints its report.
#
#   scripts/sprite_coverage.sh echoes-of-logos      # a game next to the engine
#   scripts/sprite_coverage.sh /path/to/game
set -euo pipefail
ENGINE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
game="${1:?usage: sprite_coverage.sh <game folder or name>}"
dir="$game"; [ -d "$dir" ] || dir="$ENGINE_ROOT/../$game"
[ -x "$dir/gradlew" ] || { echo "No Gradle project at $dir" >&2; exit 2; }
(cd "$dir" && ./gradlew -q testDebugUnitTest --tests 'com.voicerpg.engine.content.GameSpriteContractTest' >/dev/null 2>&1) || true
report="$dir/app/build/reports/sprite-contract.txt"
[ -f "$report" ] || { echo "No report written; run the game's tests to see why." >&2; exit 1; }
cat "$report"
grep -q ", 0 problem(s)" "$report"
