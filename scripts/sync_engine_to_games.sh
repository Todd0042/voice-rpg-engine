#!/usr/bin/env bash
# ==============================================================================
# sync_engine_to_games.sh
#
# Synchronizes the shared core VoiceRPG engine source code, templates, and
# multilingual localization assets between `voice-rpg-engine` and derived games:
#   - echoes-of-logos
#   - wallys-world
#   - commander-america-rpg
#
# Modes:
#   Push (default): Distributes engine updates to downstream game repositories.
#   Pull:           Imports engine updates from a specific game back to upstream.
#   Check:          Detects divergence between engine and downstream game forks.
#
# Usage:
#   ./scripts/sync_engine_to_games.sh [--dry-run] [--verify] [target_repo...]
#   ./scripts/sync_engine_to_games.sh --pull-from <game_repo> [--verify]
#   ./scripts/sync_engine_to_games.sh --check
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENGINE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GITHUB_ROOT="$(cd "${ENGINE_ROOT}/.." && pwd)"

DRY_RUN=false
RUN_VERIFY=false
CHECK_ONLY=false
PULL_FROM=""
CUSTOM_TARGETS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --verify)
            RUN_VERIFY=true
            shift
            ;;
        --check)
            CHECK_ONLY=true
            shift
            ;;
        --pull-from)
            PULL_FROM="$2"
            shift 2
            ;;
        *)
            CUSTOM_TARGETS+=("$1")
            shift
            ;;
    esac
done

if [ ${#CUSTOM_TARGETS[@]} -eq 0 ]; then
    TARGET_REPOS=("echoes-of-logos" "wallys-world" "commander-america-rpg" "superstore-saga")
else
    TARGET_REPOS=("${CUSTOM_TARGETS[@]}")
fi

# Shared engine packages under app/src/main/java/com/voicerpg/engine/
SHARED_PACKAGES=(
    "audio"
    "combat"
    "engine"
    "model"
    "save"
    "ui/creation"
    "ui/setup"
    "ui/story"
    "ui/combat"
    "ui/tutorial"
    "ui/gallery"
    "ui/vfx"
    "ui/theme"
    "ui/title"
    "ui/pocket"
    "ui/sprites"
    "ui/environment"
    "ui/minigame"
    "localization"
    "template"
    "viewmodel"
    "content"
)

# Shared multilingual assets under app/src/main/assets/game/
SHARED_ASSETS=(
    "commands"
    "localization"
)

# ==============================================================================
# MODE 1: Check divergence across repositories
# ==============================================================================
if [ "$CHECK_ONLY" = true ]; then
    echo "========================================================================"
    echo "VoiceRPG Engine Divergence Audit"
    echo "Engine root:  ${ENGINE_ROOT}"
    echo "Targets:      ${TARGET_REPOS[*]}"
    echo "========================================================================"
    DIVERGENCE_FOUND=0

    for target in "${TARGET_REPOS[@]}"; do
        TARGET_DIR="${GITHUB_ROOT}/${target}"
        if [ ! -d "${TARGET_DIR}" ]; then
            echo "[-] Target '${target}' not found at ${TARGET_DIR}. Skipping."
            continue
        fi

        echo ""
        echo "[*] Auditing divergence for: ${target}"
        TARGET_DIVERGED=0

        for pkg in "${SHARED_PACKAGES[@]}"; do
            SRC_PKG="${ENGINE_ROOT}/app/src/main/java/com/voicerpg/engine/${pkg}"
            DST_PKG="${TARGET_DIR}/app/src/main/java/com/voicerpg/engine/${pkg}"
            if [ -d "${SRC_PKG}" ] && [ -d "${DST_PKG}" ]; then
                DIFF_OUT=$(diff -rq "${SRC_PKG}" "${DST_PKG}" 2>/dev/null || true)
                if [ -n "$DIFF_OUT" ]; then
                    echo "    [!] Package com.voicerpg.engine.${pkg} differs:"
                    echo "$DIFF_OUT" | sed 's/^/        /'
                    TARGET_DIVERGED=1
                    DIVERGENCE_FOUND=1
                fi
            fi
        done

        if [ $TARGET_DIVERGED -eq 0 ]; then
            echo "    [✓] In sync with upstream engine."
        fi
    done

    echo ""
    if [ $DIVERGENCE_FOUND -eq 0 ]; then
        echo "========================================================================"
        echo "[✓] All repositories are 100% in sync with upstream voice-rpg-engine."
        echo "========================================================================"
        exit 0
    else
        echo "========================================================================"
        echo "[!] Divergence detected. Use --pull-from or run sync to reconcile."
        echo "========================================================================"
        exit 1
    fi
fi

# ==============================================================================
# MODE 2: Pull engine changes from a game fork back into voice-rpg-engine
# ==============================================================================
if [ -n "$PULL_FROM" ]; then
    SRC_DIR="${GITHUB_ROOT}/${PULL_FROM}"
    if [ ! -d "${SRC_DIR}" ]; then
        echo "[-] Error: Source repository '${PULL_FROM}' not found at ${SRC_DIR}"
        exit 1
    fi

    echo "========================================================================"
    echo "VoiceRPG Engine <- Import from Game Fork"
    echo "Source:       ${PULL_FROM} (${SRC_DIR})"
    echo "Engine root:  ${ENGINE_ROOT}"
    echo "Dry run:      ${DRY_RUN}"
    echo "Verify:       ${RUN_VERIFY}"
    echo "========================================================================"

    for pkg in "${SHARED_PACKAGES[@]}"; do
        SRC_PKG="${SRC_DIR}/app/src/main/java/com/voicerpg/engine/${pkg}"
        DST_PKG="${ENGINE_ROOT}/app/src/main/java/com/voicerpg/engine/${pkg}"

        if [ -d "${SRC_PKG}" ]; then
            if [ "$DRY_RUN" = true ]; then
                echo "    [dry-run] rsync -av --update ${SRC_PKG}/ -> ${DST_PKG}/"
            else
                mkdir -p "${DST_PKG}"
                rsync -av --update "${SRC_PKG}/" "${DST_PKG}/" > /dev/null
                echo "    [+] Imported package: com.voicerpg.engine.${pkg}"
            fi
        fi
    done

    for asset in "${SHARED_ASSETS[@]}"; do
        SRC_ASSET="${SRC_DIR}/app/src/main/assets/game/${asset}"
        DST_ASSET="${ENGINE_ROOT}/app/src/main/assets/game/${asset}"

        if [ -d "${SRC_ASSET}" ]; then
            if [ "$DRY_RUN" = true ]; then
                echo "    [dry-run] rsync -av --update ${SRC_ASSET}/ -> ${DST_ASSET}/"
            else
                mkdir -p "${DST_ASSET}"
                rsync -av --update "${SRC_ASSET}/" "${DST_ASSET}/" > /dev/null
                echo "    [+] Imported asset: assets/game/${asset}"
            fi
        fi
    done

    if [ "$RUN_VERIFY" = true ] && [ "$DRY_RUN" = false ]; then
        echo "    [*] Running unit tests in voice-rpg-engine..."
        (cd "${ENGINE_ROOT}" && ./gradlew testDebugUnitTest --quiet)
        echo "    [✓] Engine tests passed!"
    fi

    echo ""
    echo "[✓] Upstream import complete! Run without --pull-from to distribute to all other games."
    exit 0
fi

# ==============================================================================
# MODE 3: Push engine changes downstream to game repositories (Default)
# ==============================================================================
echo "========================================================================"
echo "VoiceRPG Engine -> Game Distribution Sync"
echo "Engine root:  ${ENGINE_ROOT}"
echo "Targets:      ${TARGET_REPOS[*]}"
echo "Dry run:      ${DRY_RUN}"
echo "Verify tests: ${RUN_VERIFY}"
echo "========================================================================"

for target in "${TARGET_REPOS[@]}"; do
    TARGET_DIR="${GITHUB_ROOT}/${target}"
    if [ ! -d "${TARGET_DIR}" ]; then
        echo "[-] Skipping target ${target}: directory not found at ${TARGET_DIR}"
        continue
    fi

    echo ""
    echo "[*] Syncing engine to: ${target}"

    # Sync shared packages
    for pkg in "${SHARED_PACKAGES[@]}"; do
        SRC_PKG="${ENGINE_ROOT}/app/src/main/java/com/voicerpg/engine/${pkg}"
        DST_PKG="${TARGET_DIR}/app/src/main/java/com/voicerpg/engine/${pkg}"

        if [ -d "${SRC_PKG}" ]; then
            if [ "$DRY_RUN" = true ]; then
                echo "    [dry-run] rsync -av --update ${SRC_PKG}/ -> ${DST_PKG}/"
            else
                mkdir -p "${DST_PKG}"
                rsync -av --update "${SRC_PKG}/" "${DST_PKG}/" > /dev/null
                echo "    [+] Synced package: com.voicerpg.engine.${pkg}"
            fi
        fi
    done

    # Sync shared localization and command catalogs
    for asset in "${SHARED_ASSETS[@]}"; do
        SRC_ASSET="${ENGINE_ROOT}/app/src/main/assets/game/${asset}"
        DST_ASSET="${TARGET_DIR}/app/src/main/assets/game/${asset}"

        if [ -d "${SRC_ASSET}" ]; then
            if [ "$DRY_RUN" = true ]; then
                echo "    [dry-run] rsync -av --update ${SRC_ASSET}/ -> ${DST_ASSET}/"
            else
                mkdir -p "${DST_ASSET}"
                rsync -av --update "${SRC_ASSET}/" "${DST_ASSET}/" > /dev/null
            fi
        fi
    done

    # Sync root MainActivity.kt
    if [ "$DRY_RUN" = true ]; then
        echo "    [dry-run] cp ${ENGINE_ROOT}/app/src/main/java/com/voicerpg/engine/MainActivity.kt -> ${TARGET_DIR}/app/src/main/java/com/voicerpg/engine/MainActivity.kt"
    else
        cp "${ENGINE_ROOT}/app/src/main/java/com/voicerpg/engine/MainActivity.kt" "${TARGET_DIR}/app/src/main/java/com/voicerpg/engine/MainActivity.kt"
        echo "    [+] Synced root Activity: MainActivity.kt"
    fi

    if [ "$RUN_VERIFY" = true ] && [ "$DRY_RUN" = false ]; then
        echo "    [*] Running unit tests in ${target}..."
        (cd "${TARGET_DIR}" && ./gradlew testDebugUnitTest --quiet)
        echo "    [✓] Tests passed for ${target}"
    fi
done

echo ""
echo "========================================================================"
echo "[✓] Engine distribution complete!"
echo "========================================================================"
