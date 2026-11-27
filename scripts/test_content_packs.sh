#!/usr/bin/env bash
# ==============================================================================
# test_content_packs.sh
#
# Runs the engine's full unit-test suite against each game's content pack: this engine's code
# (working tree, uncommitted changes included) + one game's app/src/main/assets. It answers
# "does the engine support every game?" without touching the games' repos.
#
# Each run happens in a throwaway copy under ${TMPDIR:-/tmp}/vb-content-packs/<game>, so the engine
# and game repos are never modified.
#
# Usage:
#   ./scripts/test_content_packs.sh                 # all games
#   ./scripts/test_content_packs.sh wallys-world    # one or more games
#   GAME_REF_echoes_of_logos=origin/main ./scripts/test_content_packs.sh echoes-of-logos
#       Content from a git ref instead of the working tree (dashes in the repo name become underscores).
#
# Output: one line per game (PASS / FAIL with the failing test classes), and the full Gradle log and
# JUnit XML in the run's folder. Exit code 1 if any game fails.
# ==============================================================================
set -euo pipefail

ENGINE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Games are expected next to the engine; VB_GAMES_ROOT points elsewhere (e.g. a folder of worktrees).
# VB_TEST_FILTER runs only matching tests (Gradle --tests pattern), e.g. VB_TEST_FILTER="*BalanceContractTest*".
GITHUB_ROOT="${VB_GAMES_ROOT:-$(cd "${ENGINE_ROOT}/.." && pwd)}"
WORK_ROOT="${TMPDIR:-/tmp}/vb-content-packs"
GAMES=("$@")
[ ${#GAMES[@]} -eq 0 ] && GAMES=("echoes-of-logos" "wallys-world" "commander-america-rpg" "superstore-saga")

if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Android/Sdk" ]; then export ANDROID_HOME="$HOME/Android/Sdk"; fi

mkdir -p "$WORK_ROOT"
summary=()
failed=0

for game in "${GAMES[@]}"; do
    game_dir="${GITHUB_ROOT}/${game}"
    if [ ! -e "$game_dir/.git" ]; then
        summary+=("SKIP  ${game}: no repository at ${game_dir}")
        continue
    fi
    run="${WORK_ROOT}/${game}"
    echo "[*] ${game}: preparing engine + content in ${run}"
    rm -rf "$run"
    mkdir -p "$run"
    # The engine's working tree, minus build output and local state.
    rsync -a --exclude '.git/' --exclude 'build/' --exclude '.gradle/' --exclude 'app/build/' \
        --exclude 'local.properties' --exclude '.idea/' "${ENGINE_ROOT}/" "$run/"
    rm -rf "$run/app/src/main/assets"
    mkdir -p "$run/app/src/main/assets"

    ref_var="GAME_REF_${game//-/_}"
    ref="${!ref_var:-}"
    if [ -n "$ref" ]; then
        if ! git -C "$game_dir" rev-parse --verify -q "$ref^{commit}" >/dev/null; then
            summary+=("FAIL  ${game}: ref ${ref} not found (git fetch in ${game_dir}?)")
            failed=1
            continue
        fi
        git -C "$game_dir" archive "$ref" app/src/main/assets | tar -x -C "$run"
        source_desc="${ref} ($(git -C "$game_dir" rev-parse --short "$ref"))"
    else
        rsync -a "$game_dir/app/src/main/assets/" "$run/app/src/main/assets/"
        dirty=""
        if [ -n "$(git -C "$game_dir" status --porcelain -- app/src/main/assets)" ]; then dirty=", uncommitted changes"; fi
        source_desc="working tree ($(git -C "$game_dir" rev-parse --short HEAD)${dirty})"
    fi
    if [ ! -d "$run/app/src/main/assets/game" ]; then
        summary+=("FAIL  ${game}: no app/src/main/assets/game in ${source_desc}")
        failed=1
        continue
    fi
    # Asset folders the engine owns (ENGINE_OWNED_PATHS in sync_engine_to_games.sh) come from this
    # engine, exactly as a sync would leave them; the game's copies may be from an older engine.
    for owned in commands localization; do
        rm -rf "$run/app/src/main/assets/game/$owned"
        if [ -d "$ENGINE_ROOT/app/src/main/assets/game/$owned" ]; then
            cp -a "$ENGINE_ROOT/app/src/main/assets/game/$owned" "$run/app/src/main/assets/game/"
        fi
    done

    echo "[*] ${game}: running engine tests on content from ${source_desc}"
    log="$run/gradle-test.log"
    if (cd "$run" && ./gradlew --console=plain --no-daemon testDebugUnitTest ${VB_TEST_FILTER:+--tests "$VB_TEST_FILTER"} >"$log" 2>&1); then
        summary+=("PASS  ${game}  [content: ${source_desc}]")
    else
        failed=1
        results="$run/app/build/test-results/testDebugUnitTest"
        if compgen -G "$results/*.xml" >/dev/null; then
            failing=$(grep -l -E '<(failure|error)' "$results"/*.xml | sed -E 's#.*/TEST-(.*)\.xml#\1#' | sed 's/^com\.voicerpg\.engine\.//' | tr '\n' ' ')
            counts=$(cat "$results"/*.xml | grep -oE 'tests="[0-9]+"[^>]*failures="[0-9]+" errors="[0-9]+"' |
                awk -F'"' '{t+=$2; f+=$(NF-3); e+=$(NF-1)} END {printf "%d tests, %d failed", t, f+e}')
            summary+=("FAIL  ${game}  [content: ${source_desc}] ${counts}: ${failing}")
        else
            summary+=("FAIL  ${game}  [content: ${source_desc}] build failed before tests ran; see ${log}")
        fi
    fi
done

echo ""
echo "========================================================================"
engine_dirty=""
if [ -n "$(git -C "$ENGINE_ROOT" status --porcelain)" ]; then engine_dirty=" + uncommitted changes"; fi
echo "Engine $(git -C "$ENGINE_ROOT" rev-parse --short HEAD)${engine_dirty} against each game's content"
echo "========================================================================"
printf '%s\n' "${summary[@]}"
echo "Logs and JUnit results: ${WORK_ROOT}/<game>/"
exit $failed
