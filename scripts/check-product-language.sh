#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGETS=("$ROOT_DIR/backend/src/main/java" "$ROOT_DIR/frontend/app" "$ROOT_DIR/frontend/components" "$ROOT_DIR/frontend/lib")
PATTERN='Lei |Artigo|Art\. |Aviso [0-9]|Base Legal|limiar legal|GBM/'
if grep -RInE "$PATTERN" "${TARGETS[@]}" --exclude-dir=node_modules --exclude-dir=.next; then
  printf '%s\n' "Prohibited external legal wording found in a product surface." >&2
  exit 1
fi
printf '%s\n' "Product-language check passed."
