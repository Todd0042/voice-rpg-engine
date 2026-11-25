#!/usr/bin/env bash
# ==============================================================================
# sync_engine_to_games.sh
#
# Keeps every game on the identical engine. The engine owns these paths in each game (and nothing
# else): ENGINE_OWNED_PATHS below. In a game they are read-only copies of the engine.
#
#   Push (default)   Mirror the engine into games: exact copy by checksum, stray files removed.
#                    Writes .engine-sync/ (engine version, checksum manifest, guard script) and
#                    installs the pre-commit hook that blocks edits to engine files in the game.
#   --check          Report, per game: NOT MIGRATED / modified / missing / extra / out of date / in sync.
#   --diff <game>    Show a unified diff (engine -> game) of the engine-owned paths, for review.
#   --pull-from <game> <path>...
#                    Copy the named files (paths relative to the repo root) from a game into the
#                    engine, after showing their diff. Only for changes that belong upstream.
#
# Push options:
#   --verify         Run each game's unit tests after syncing.
#   --dry-run        Show what would change; write nothing.
#   --prune          Delete files in the game's engine-owned paths that the engine doesn't have
#                    (otherwise the push stops and lists them: move game tests to com/voicerpg/game).
#   --allow-dirty    Push an engine with uncommitted changes (local experiments; the version is
#                    recorded as <hash>-dirty and the game's CI guard still checks exact checksums).
#
# Usage:
#   ./scripts/sync_engine_to_games.sh [--verify] [--dry-run] [--prune] [--allow-dirty] [game...]
#   ./scripts/sync_engine_to_games.sh --check [game...]
#   ./scripts/sync_engine_to_games.sh --diff <game>
#   ./scripts/sync_engine_to_games.sh --pull-from <game> <path>...
# ==============================================================================
set -euo pipefail
export LC_ALL=C   # one sort order for sort/comm

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENGINE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# Games are expected next to the engine; VB_GAMES_ROOT points elsewhere (e.g. a folder of worktrees).
GITHUB_ROOT="${VB_GAMES_ROOT:-$(cd "${ENGINE_ROOT}/.." && pwd)}"
ALL_GAMES=("echoes-of-logos" "wallys-world" "commander-america-rpg" "superstore-saga")

# Everything the engine owns inside a game. Directories end with '/'.
ENGINE_OWNED_PATHS=(
    "app/src/main/java/com/voicerpg/engine/"
    "app/src/main/assets/game/commands/"
    "app/src/main/assets/game/localization/"
    "app/src/test/java/com/voicerpg/engine/"
    "app/src/test/fixtures/template-pack/"
)
ENGINE_JAVA="app/src/main/java/com/voicerpg/engine/"
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Android/Sdk" ]; then export ANDROID_HOME="$HOME/Android/Sdk"; fi

MODE="push"; DRY_RUN=false; RUN_VERIFY=false; PRUNE=false; ALLOW_DIRTY=false
PULL_FROM=""; DIFF_GAME=""; TARGETS=(); PULL_PATHS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --check) MODE="check"; shift ;;
        --diff) MODE="diff"; DIFF_GAME="${2:?--diff needs a game}"; shift 2 ;;
        --pull-from) MODE="pull"; PULL_FROM="${2:?--pull-from needs a game}"; shift 2
            while [[ $# -gt 0 && "$1" != --* ]]; do PULL_PATHS+=("$1"); shift; done ;;
        --verify) RUN_VERIFY=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        --prune) PRUNE=true; shift ;;
        --allow-dirty) ALLOW_DIRTY=true; shift ;;
        -h|--help) sed -n '2,32p' "$0"; exit 0 ;;
        --*) echo "Unknown option: $1" >&2; exit 2 ;;
        *) TARGETS+=("$1"); shift ;;
    esac
done
[ ${#TARGETS[@]} -eq 0 ] && TARGETS=("${ALL_GAMES[@]}")

die() { echo "[-] $*" >&2; exit 1; }

# List engine-owned files (relative paths) present under a root.
owned_files() {
    local root="$1" p
    for p in "${ENGINE_OWNED_PATHS[@]}"; do
        if [ -d "$root/$p" ]; then (cd "$root" && find "$p" -type f ! -name '.DS_Store'); fi
    done | sort
    return 0
}

engine_version() {
    local v; v="$(git -C "$ENGINE_ROOT" rev-parse HEAD)"
    if [ -n "$(git -C "$ENGINE_ROOT" status --porcelain -- "${ENGINE_OWNED_PATHS[@]}")" ]; then v="${v}-dirty"; fi
    echo "$v"
}

# Compare engine vs one game. Prints a report; returns 0 if identical.
compare_game() {
    local game="$1" dir="${GITHUB_ROOT}/$1" status=0
    local tmp; tmp="$(mktemp -d)"
    owned_files "$ENGINE_ROOT" > "$tmp/engine"
    owned_files "$dir" > "$tmp/game"
    local missing extra modified
    missing=$(comm -23 "$tmp/engine" "$tmp/game")
    extra=$(comm -13 "$tmp/engine" "$tmp/game")
    modified=$(comm -12 "$tmp/engine" "$tmp/game" | while read -r f; do cmp -s "$ENGINE_ROOT/$f" "$dir/$f" || echo "$f"; done)
    rm -rf "$tmp"
    [ -n "$modified" ] && { echo "    modified ($(wc -l <<<"$modified")):"; sed 's/^/        /' <<<"$modified"; status=1; }
    [ -n "$missing" ] && { echo "    missing in game ($(wc -l <<<"$missing")):"; sed 's/^/        /' <<<"$missing"; status=1; }
    [ -n "$extra" ] && { echo "    extra in game, not in engine ($(wc -l <<<"$extra")):"; sed 's/^/        /' <<<"$extra"; status=1; }
    return $status
}

# ==============================================================================
# --check
# ==============================================================================
if [ "$MODE" = "check" ]; then
    echo "Engine: $(engine_version)"
    overall=0
    for game in "${TARGETS[@]}"; do
        dir="${GITHUB_ROOT}/${game}"
        if [ ! -d "$dir" ]; then echo "[?] ${game}: not found at ${dir}"; overall=1; continue; fi
        branch="$(git -C "$dir" branch --show-current 2>/dev/null || echo '?')"
        if [ ! -d "$dir/$ENGINE_JAVA" ]; then
            echo "[!] ${game} (${branch}): NOT MIGRATED: no ${ENGINE_JAVA} on this branch"
            overall=1; continue
        fi
        recorded="$(cat "$dir/.engine-sync/ENGINE_VERSION" 2>/dev/null || echo 'none')"
        if report="$(compare_game "$game")"; then
            if [ "$recorded" = "$(engine_version)" ]; then
                echo "[✓] ${game} (${branch}): in sync"
            else
                echo "[~] ${game} (${branch}): files identical, but .engine-sync/ENGINE_VERSION is ${recorded:0:12} (run a push to record it)"
                overall=1
            fi
        else
            echo "[!] ${game} (${branch}): DIVERGED from engine (recorded engine version: ${recorded:0:12})"
            echo "$report"
            overall=1
        fi
    done
    exit $overall
fi

# ==============================================================================
# --diff <game>
# ==============================================================================
if [ "$MODE" = "diff" ]; then
    dir="${GITHUB_ROOT}/${DIFF_GAME}"
    [ -d "$dir/$ENGINE_JAVA" ] || die "${DIFF_GAME} has no ${ENGINE_JAVA} (not migrated on its current branch)"
    for p in "${ENGINE_OWNED_PATHS[@]}"; do
        if [ -d "$ENGINE_ROOT/$p" ] || [ -d "$dir/$p" ]; then
            diff -ruN "$ENGINE_ROOT/$p" "$dir/$p" | sed "s#${ENGINE_ROOT}/#engine/#; s#${dir}/#${DIFF_GAME}/#" || true
        fi
    done
    exit 0
fi

# ==============================================================================
# --pull-from <game> <path>...
# ==============================================================================
if [ "$MODE" = "pull" ]; then
    dir="${GITHUB_ROOT}/${PULL_FROM}"
    [ -d "$dir" ] || die "${PULL_FROM} not found at ${dir}"
    [ ${#PULL_PATHS[@]} -gt 0 ] || die "Name the files to pull, e.g. --pull-from ${PULL_FROM} ${ENGINE_JAVA}engine/SaveManager.kt (see --diff ${PULL_FROM})"
    for f in "${PULL_PATHS[@]}"; do
        owned=false
        for p in "${ENGINE_OWNED_PATHS[@]}"; do [[ "$f" == "$p"* ]] && owned=true; done
        $owned || die "$f is not an engine-owned path"
        [ -f "$dir/$f" ] || die "$f does not exist in ${PULL_FROM}"
        echo "=== ${f}"
        diff -u "$ENGINE_ROOT/$f" "$dir/$f" || true
    done
    if [ "$DRY_RUN" = true ]; then echo "[dry-run] nothing copied"; exit 0; fi
    for f in "${PULL_PATHS[@]}"; do
        mkdir -p "$(dirname "$ENGINE_ROOT/$f")"
        cp "$dir/$f" "$ENGINE_ROOT/$f"
        echo "[+] pulled ${f}"
    done
    echo "Review, make it content-neutral, run ./gradlew testDebugUnitTest and scripts/test_content_packs.sh, commit, then push to all games."
    exit 0
fi

# ==============================================================================
# Push (default)
# ==============================================================================
if [ "$ALLOW_DIRTY" = false ] && [ "$DRY_RUN" = false ] && \
   [ -n "$(git -C "$ENGINE_ROOT" status --porcelain -- "${ENGINE_OWNED_PATHS[@]}")" ]; then
    die "The engine has uncommitted changes in engine-owned paths. Commit them first (games record the engine commit), or pass --allow-dirty."
fi
VERSION="$(engine_version)"
echo "Engine ${VERSION} -> ${TARGETS[*]}$( [ "$DRY_RUN" = true ] && echo '  [dry run]')"

failures=0
for game in "${TARGETS[@]}"; do
    dir="${GITHUB_ROOT}/${game}"
    echo ""
    echo "[*] ${game}"
    if [ ! -e "$dir/.git" ]; then echo "    [-] not found at ${dir}; skipped"; failures=1; continue; fi
    branch="$(git -C "$dir" branch --show-current)"
    if [ ! -d "$dir/$ENGINE_JAVA" ]; then
        echo "    [-] NOT MIGRATED: branch '${branch}' has no ${ENGINE_JAVA}. Check out the engine-based branch first."
        failures=1; continue
    fi
    if [ -n "$(git -C "$dir" status --porcelain -- "${ENGINE_OWNED_PATHS[@]}")" ] && \
       ! { [ -f "$dir/.engine-sync/guard.sh" ] && (cd "$dir" && bash .engine-sync/guard.sh --ci >/dev/null 2>&1); }; then
        # (Uncommitted files that exactly match the last recorded sync are that sync, not edits: safe to replace.)
        echo "    [-] uncommitted changes in engine-owned paths on '${branch}'; a push would overwrite them:"
        git -C "$dir" status --porcelain -- "${ENGINE_OWNED_PATHS[@]}" | sed 's/^/        /'
        echo "        If they belong upstream: --pull-from ${game} <path>. Otherwise discard them."
        failures=1; continue
    fi
    extra=$(comm -13 <(owned_files "$ENGINE_ROOT") <(owned_files "$dir"))
    if [ -n "$extra" ] && [ "$PRUNE" = false ]; then
        echo "    [-] files in engine-owned paths that the engine doesn't have:"
        sed 's/^/        /' <<<"$extra"
        echo "        Game-specific tests belong in app/src/test/java/com/voicerpg/game/. Engine code belongs upstream."
        echo "        Move or upstream them, or re-run with --prune to delete them."
        failures=1; continue
    fi

    for p in "${ENGINE_OWNED_PATHS[@]}"; do
        [ -d "$ENGINE_ROOT/$p" ] || continue
        if [ "$DRY_RUN" = true ]; then
            rsync -a --checksum --delete --dry-run --itemize-changes "$ENGINE_ROOT/$p" "$dir/$p" | sed "s#^#    [dry-run] ${p}: #"
        else
            mkdir -p "$dir/$p"
            rsync -a --checksum --delete "$ENGINE_ROOT/$p" "$dir/$p"
        fi
    done
    if [ "$DRY_RUN" = true ]; then continue; fi

    # Record the version and every engine-owned file's checksum, and install the guard.
    mkdir -p "$dir/.engine-sync" "$dir/.githooks"
    echo "$VERSION" > "$dir/.engine-sync/ENGINE_VERSION"
    printf '%s\n' "${ENGINE_OWNED_PATHS[@]}" > "$dir/.engine-sync/OWNED_PATHS"
    (cd "$dir" && owned_files "$dir" | xargs -d '\n' sha256sum) > "$dir/.engine-sync/MANIFEST.sha256"
    cp "$ENGINE_ROOT/scripts/engine_guard.sh" "$dir/.engine-sync/guard.sh"
    chmod +x "$dir/.engine-sync/guard.sh"
    cp "$ENGINE_ROOT/scripts/githooks/pre-commit" "$dir/.githooks/pre-commit"
    chmod +x "$dir/.githooks/pre-commit"
    # The release gate: a release build runs the strict release-variant tests (sprite contract, docs/SPRITE_PLAN.md).
    cp "$ENGINE_ROOT/scripts/game-files/release_gate.gradle.kts" "$dir/.engine-sync/release_gate.gradle.kts"
    gate_line='apply(from = rootProject.file(".engine-sync/release_gate.gradle.kts")) // engine release gate'
    if [ -f "$dir/app/build.gradle.kts" ] && ! grep -qF '.engine-sync/release_gate.gradle.kts' "$dir/app/build.gradle.kts"; then
        printf '\n%s\n' "$gate_line" >> "$dir/app/build.gradle.kts"
        echo "    [+] app/build.gradle.kts now applies the engine release gate"
    fi
    # The agent rule: in .agy/rules/ (Antigravity) and as a marked block in AGENTS.md / GEMINI.md.
    mkdir -p "$dir/.agy/rules"
    cp "$ENGINE_ROOT/scripts/game-files/00_voicerpg_engine.md" "$dir/.agy/rules/00_voicerpg_engine.md"
    for doc in AGENTS.md GEMINI.md; do
        python3 - "$dir/$doc" "$ENGINE_ROOT/scripts/game-files/00_voicerpg_engine.md" <<'PY'
import os, re, sys
doc, rule = sys.argv[1], open(sys.argv[2]).read().strip()
start, end = "<!-- voicerpg-engine-rule:start -->", "<!-- voicerpg-engine-rule:end -->"
block = f"{start}\n{rule}\n{end}"
text = open(doc).read() if os.path.exists(doc) else ""
if start in text and end in text:
    text = re.sub(re.escape(start) + r".*?" + re.escape(end), lambda m: block, text, flags=re.S)
else:
    text = block + "\n\n" + text
open(doc, "w").write(text)
PY
    done

    current_hooks="$(git -C "$dir" config core.hooksPath || true)"
    if [ -z "$current_hooks" ] || [ "$current_hooks" = ".githooks" ]; then
        git -C "$dir" config core.hooksPath .githooks
    else
        echo "    [!] core.hooksPath is '${current_hooks}'; add .githooks/pre-commit to it yourself."
    fi
    # CI check: its own small workflow (checksums only, a few seconds), kept up to date by every sync.
    mkdir -p "$dir/.github/workflows"
    cp "$ENGINE_ROOT/scripts/game-files/engine_guard.yml" "$dir/.github/workflows/engine_guard.yml"
    echo "    [+] synced (${branch}); engine version recorded: ${VERSION:0:12}"

    if [ "$RUN_VERIFY" = true ]; then
        echo "    [*] running ${game} unit tests..."
        if (cd "$dir" && ./gradlew testDebugUnitTest --quiet > "${TMPDIR:-/tmp}/engine-sync-verify-${game}.log" 2>&1); then
            echo "    [✓] ${game} tests passed"
        else
            echo "    [-] ${game} tests FAILED (log: ${TMPDIR:-/tmp}/engine-sync-verify-${game}.log). Either the engine change broke"
            echo "        a game contract (fix it upstream) or the game's content breaks one (fix the content)."
            failures=1
        fi
    fi
    echo "    Commit in ${game}:  git add -A ${ENGINE_OWNED_PATHS[*]} .engine-sync .githooks .github/workflows/engine_guard.yml app/build.gradle.kts AGENTS.md GEMINI.md && git commit -m \"Sync engine ${VERSION:0:12}\""
done

echo ""
[ $failures -eq 0 ] && echo "[✓] done" || { echo "[-] finished with problems (see above)"; exit 1; }
