#!/usr/bin/env bash
set -euo pipefail

RUN_JS_URL="https://raw.githubusercontent.com/mtbarr/spript/main/create-spript-project/run.js"
PROJECT_NAME="${1:-}"

if [ -z "$PROJECT_NAME" ]; then
  echo "Usage: bash install.sh <project-name>"
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "Error: Node.js is required."
  exit 1
fi

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if command -v curl >/dev/null 2>&1; then
  curl -fsSL "$RUN_JS_URL" > "$WORKDIR/run.js"
elif command -v wget >/dev/null 2>&1; then
  wget -qO "$WORKDIR/run.js" "$RUN_JS_URL"
else
  echo "Error: curl or wget is required."
  exit 1
fi

node "$WORKDIR/run.js" "$PROJECT_NAME"
