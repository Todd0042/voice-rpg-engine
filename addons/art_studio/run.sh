#!/usr/bin/env bash
set -euo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PY="python3"
[[ -x "$REPO_DIR/.venv/bin/python" ]] && PY="$REPO_DIR/.venv/bin/python"
PYTHONPATH="$REPO_DIR" exec "$PY" -m addons.art_studio.server "$@"
