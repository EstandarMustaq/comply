#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${ROOT_DIR}/package/compliance-engine}"
FRONTEND_DIR="${FRONTEND_DIR:-${ROOT_DIR}/app/application}"

backend() { mvn -q -f "$BACKEND_DIR/pom.xml" "$@"; }
frontend() { (cd "$FRONTEND_DIR" && pnpm "$@"); }

usage() {
  printf '%s\n' "Usage: $0 {install|dev|build|test|typecheck|lint|quality|compose-config|migrate|deploy|backup|keys|help}"
}

case "${1:-help}" in
  install)
    (cd "$FRONTEND_DIR" && pnpm install --frozen-lockfile)
    backend dependency:go-offline
    ;;
  dev)
    if [[ "${2:-}" == "--docker" ]]; then
      docker compose --profile app up -d --build
    else
      printf '%s\n' "Start the compliance engine and application in separate terminals:"
      printf '  (cd %q && mvn spring-boot:run)\n' "$BACKEND_DIR"
      printf '  (cd %q && pnpm dev)\n' "$FRONTEND_DIR"
    fi
    ;;
  build)
    backend package -DskipTests
    frontend build
    ;;
  test)
    backend test
    frontend exec tsc --noEmit
    ;;
  typecheck) frontend exec tsc --noEmit ;;
  lint) frontend lint ;;
  quality)
    backend test
    frontend exec tsc --noEmit
    frontend lint
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
  migrate) backend validate ;;
  deploy) "$ROOT_DIR/scripts/prod-deploy.sh" ;;
  backup) "$ROOT_DIR/scripts/backup-s3.sh" ;;
  keys) "$ROOT_DIR/scripts/generate-keys.sh" "${@:2}" ;;
  help|-h|--help) usage ;;
  *) usage; exit 2 ;;
esac
