#!/usr/bin/env bash
# ==============================================================================
# sync_engine_to_games.sh
#
# Synchronizes the shared core VoiceRPG engine source code, templates, and
# multilingual localization assets from `voice-rpg-engine` to derived games:
#   - echoes-of-logos
#   - wallys-world
#   - commander-america-rpg
#
# Usage:
#   ./scripts/sync_engine_to_games.sh [--dry-run] [--verify] [target_repo...]
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENGINE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GITHUB_ROOT="$(cd "${ENGINE_ROOT}/.." && pwd)"

DRY_RUN=false
RUN_VERIFY=false
CUSTOM_TARGETS=()

for arg in "$@"; do
    case "$arg" in
        --dry-run)
            DRY_RUN=true
            ;;
        --verify)
            RUN_VERIFY=true
            ;;
        *)
            CUSTOM_TARGETS+=("$arg")
            ;;
    esac
done

if [ ${#CUSTOM_TARGETS[@]} -eq 0 ]; then
    TARGET_REPOS=("echoes-of-logos" "wallys-world" "commander-america-rpg")
else
    TARGET_REPOS=("${CUSTOM_TARGETS[@]}")
fi

echo "========================================================================"
echo "VoiceRPG Engine -> Game Distribution Sync"
echo "Engine root:  ${ENGINE_ROOT}"
echo "Targets:      ${TARGET_REPOS[*]}"
echo "Dry run:      ${DRY_RUN}"
echo "Verify tests: ${RUN_VERIFY}"
echo "========================================================================"

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
    "viewmodel"
    "content"
)

# Shared multilingual assets under app/src/main/assets/game/
SHARED_ASSETS=(
    "commands"
    "localization"
)

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
                echo "    [+] Synced asset: assets/game/${asset}"
            fi
        fi
    done

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
