#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRC_ENGINE_DIR="${GRC_ENGINE_DIR:-${ROOT_DIR}/package/grc-engine}"
DESIGN_SYSTEM_DIR="${DESIGN_SYSTEM_DIR:-${ROOT_DIR}/app/design-system}"

grc_engine() { mvn -q -f "$GRC_ENGINE_DIR/pom.xml" "$@"; }
design_system() { (cd "$DESIGN_SYSTEM_DIR" && pnpm "$@"); }

usage() {
  printf '%s\n' "Usage: $0 {install|dev|build|test|typecheck|lint|quality|compose-config|migrate|deploy|backup|keys|help}"
}

case "${1:-help}" in
  install)
    design_system install --frozen-lockfile
    grc_engine dependency:go-offline
    ;;
  dev)
    if [[ "${2:-}" == "--docker" ]]; then
      docker compose --profile app up -d --build
    else
      printf '%s\n' "Start the GRC engine and design system app in separate terminals:"
      printf '  (cd %q && mvn spring-boot:run)\n' "$GRC_ENGINE_DIR"
      printf '  (cd %q && pnpm dev)\n' "$DESIGN_SYSTEM_DIR"
    fi
    ;;
  build)
    grc_engine package -DskipTests
    design_system build
    ;;
  test)
    grc_engine test
    design_system run test:unit
    ;;
  typecheck) design_system run typecheck ;;
  lint) design_system run lint ;;
  quality)
    grc_engine test
    design_system run check
    design_system run test:unit
    bash -n "$ROOT_DIR"/scripts/*.sh
    "$ROOT_DIR/scripts/check-product-language.sh"
    docker compose config --quiet
    docker compose -f "$ROOT_DIR/docker-compose.prod.yml" config --quiet
    git diff --check
    ;;
  compose-config)
    docker compose config --quiet
    docker compose -f "$ROOT_DIR/docker-compose.prod.yml" config --quiet
    ;;
  migrate) grc_engine validate ;;
  deploy) "$ROOT_DIR/scripts/prod-deploy.sh" ;;
  backup) "$ROOT_DIR/scripts/backup-s3.sh" ;;
  keys) "$ROOT_DIR/scripts/generate-keys.sh" "${@:2}" ;;
  help|-h|--help) usage ;;
  *) usage; exit 2 ;;
esac
