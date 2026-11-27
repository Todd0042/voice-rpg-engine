#!/usr/bin/env bash
# VoiceRPG Engine — Art Studio & Battle Sprite Pipeline Dependency Installer
# Usage:
#   ./addons/art_studio/install.sh                 # Install Python + system deps into .venv
#   ./addons/art_studio/install.sh --check         # Verify environment, tools, and ComfyUI models
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STUDIO_DIR="${GRAPHICAL_STUDIO_DIR:-$HOME/graphical-studio}"
VENV_DIR="${ART_STUDIO_VENV:-$REPO_DIR/.venv}"
CHECK_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --check) CHECK_ONLY=1 ;;
    -h|--help)
      sed -n '2,6p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 2
      ;;
  esac
done

echo "==> Checking system dependencies..."
missing_sys=()
for bin in python3 ffmpeg ssh scp; do
  command -v "$bin" >/dev/null 2>&1 || missing_sys+=("$bin")
done

if ((${#missing_sys[@]} > 0)); then
  echo "Missing system tools: ${missing_sys[*]}"
  if ((CHECK_ONLY == 1)); then
    exit 1
  fi
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y python3 python3-venv python3-pip ffmpeg openssh-client
  elif command -v pacman >/dev/null 2>&1; then
    sudo pacman -S --needed --noconfirm python ffmpeg openssh
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y python3 python3-pip ffmpeg openssh-clients
  else
    echo "Please install: ${missing_sys[*]}" >&2
    exit 1
  fi
fi

if ((CHECK_ONLY == 0)); then
  if [[ ! -d "$VENV_DIR" ]]; then
    echo "==> Creating virtualenv at $VENV_DIR..."
    python3 -m venv "$VENV_DIR"
  fi
  echo "==> Installing Python packages into $VENV_DIR..."
  "$VENV_DIR/bin/python" -m pip install --upgrade pip
  "$VENV_DIR/bin/python" -m pip install \
    fastapi \
    uvicorn \
    pillow \
    numpy \
    rembg \
    onnxruntime
fi

echo "==> Checking ComfyUI / graphical-studio models in $STUDIO_DIR..."
MODELS_DIR="$STUDIO_DIR/ComfyUI/models"
check_model() {
  local label="$1" path="$2"
  if [[ -f "$path" ]]; then
    echo "  [OK] $label ($path)"
  else
    echo "  [MISSING] $label ($path)"
  fi
}

check_model "FLUX.2 Klein UNet"      "$MODELS_DIR/unet/flux-2-klein-4b-Q8_0.gguf"
check_model "Qwen 3 4B Text Encoder" "$MODELS_DIR/text_encoders/qwen_3_4b.safetensors"
check_model "FLUX.2 VAE"             "$MODELS_DIR/vae/flux2-vae.safetensors"
check_model "SDXL Base 1.0"          "$MODELS_DIR/checkpoints/sd_xl_base_1.0.safetensors"
check_model "Pixel Art XL LoRA"      "$MODELS_DIR/loras/pixel-art-xl.safetensors"
check_model "Wan 2.2 14B HighNoise"  "$MODELS_DIR/unet/Wan2.2-I2V-A14B-HighNoise-Q4_K_S.gguf"
check_model "Wan 2.2 14B LowNoise"   "$MODELS_DIR/unet/Wan2.2-I2V-A14B-LowNoise-Q4_K_S.gguf"
check_model "Wan 2.1 VAE"            "$MODELS_DIR/vae/wan_2.1_vae.safetensors"
check_model "UMT5 XXL Text Encoder"  "$MODELS_DIR/text_encoders/umt5_xxl_fp8_e4m3fn_scaled.safetensors"

echo ""
echo "Done. Start Art Studio with:"
echo "  $REPO_DIR/addons/art_studio/run.sh"
