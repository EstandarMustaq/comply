#!/usr/bin/env bash
# ==============================================================================
# FinComp Development Environment Controller
# Orchestrates local PostgreSQL, Redis, Compliance Engine (Spring Boot), and Frontend (Next.js)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ANSI formatting
GREEN=$'\033[0;32m'
CYAN=$'\033[0;36m'
YELLOW=$'\033[1;33m'
RED=$'\033[0;31m'
BOLD=$'\033[1m'
NC=$'\033[0m'

check_env() {
    if [[ ! -f "${ROOT_DIR}/.env" ]]; then
        echo -e "${YELLOW}[!] Root .env not found. Initializing from .env.example...${NC}"
        cp "${ROOT_DIR}/.env.example" "${ROOT_DIR}/.env"
        echo -e "${GREEN}[✓] Created ${ROOT_DIR}/.env${NC}"
    fi

    if [[ ! -f "${ROOT_DIR}/app/application/.env.local" ]]; then
        echo -e "${YELLOW}[!] app/application/.env.local not found. Initializing from app/application/.env.example...${NC}"
        cp "${ROOT_DIR}/app/application/.env.example" "${ROOT_DIR}/app/application/.env.local"
        echo -e "${GREEN}[✓] Created ${ROOT_DIR}/app/application/.env.local${NC}"
    fi
}

wait_for_health() {
    local service="$1"
    local max_retries="${2:-30}"
    local count=0

    echo -ne "${CYAN}Waiting for ${service} to be healthy...${NC} "
    while [ $count -lt $max_retries ]; do
        local health_status
        health_status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' "fincomp-${service}" 2>/dev/null || echo "unstarted")

        if [ "$health_status" = "healthy" ] || [ "$health_status" = "running" ]; then
            echo -e "${GREEN}[READY]${NC}"
            return 0
        fi

        sleep 1
        count=$((count + 1))
        echo -ne "."
    done

    echo -e "\n${RED}Timeout waiting for ${service} to be healthy.${NC}"
    docker compose -f "${ROOT_DIR}/docker-compose.yml" logs "${service}"
    return 1
}

start_infra() {
    check_env
    echo -e "${BOLD}${CYAN}=== Starting FinComp Core Infrastructure (PostgreSQL + Redis) ===${NC}"
    docker compose -f "${ROOT_DIR}/docker-compose.yml" up -d postgres redis

    wait_for_health "postgres" 30
    wait_for_health "redis" 20

    echo ""
    echo -e "${GREEN}==================================================================${NC}"
    echo -e "${BOLD}FinComp Infrastructure is Ready for Local Development${NC}"
    echo -e "${GREEN}==================================================================${NC}"
    echo -e "  ${BOLD}PostgreSQL:${NC}  localhost:5432 (DB: fincomp_db, User: fincomp_app)"
    echo -e "  ${BOLD}Redis:${NC}       localhost:6379"
    echo ""
    echo -e "${BOLD}Next steps:${NC}"
    echo -e "  1. Start Compliance Engine:  ${CYAN}./scripts/dev.sh compliance-engine${NC}  (or cd package/compliance-engine && mvn spring-boot:run)"
    echo -e "  2. Start Application: ${CYAN}./scripts/dev.sh application${NC} (or cd app/application && pnpm dev)"
    echo -e "  Or run full stack: ${CYAN}./scripts/dev.sh all${NC}"
    echo ""
}

start_compliance_engine() {
    check_env
    echo -e "${BOLD}${CYAN}=== Launching FinComp Compliance Engine (Spring Boot) ===${NC}"
    cd "${ROOT_DIR}/package/compliance-engine"
    mvn spring-boot:run
}

start_application() {
    check_env
    echo -e "${BOLD}${CYAN}=== Launching FinComp Application (Next.js 16) ===${NC}"
    cd "${ROOT_DIR}/app/application"
    pnpm dev
}

start_all_docker() {
    check_env
    echo -e "${BOLD}${CYAN}=== Starting Full FinComp Stack in Docker ===${NC}"
    docker compose -f "${ROOT_DIR}/docker-compose.yml" --profile app up -d --build
    echo -e "${GREEN}All services launched. Run '${BOLD}./scripts/dev.sh status${NC}' to view health.${NC}"
}

stop_services() {
    echo -e "${YELLOW}Stopping all FinComp Docker services...${NC}"
    docker compose -f "${ROOT_DIR}/docker-compose.yml" --profile full down
    echo -e "${GREEN}All services stopped.${NC}"
}

show_status() {
    echo -e "${BOLD}${CYAN}=== FinComp Docker Container Status ===${NC}"
    docker compose -f "${ROOT_DIR}/docker-compose.yml" ps
}

show_logs() {
    local service="${1:-}"
    if [ -n "$service" ]; then
        docker compose -f "${ROOT_DIR}/docker-compose.yml" logs -f "$service"
    else
        docker compose -f "${ROOT_DIR}/docker-compose.yml" logs -f
    fi
}

usage() {
    cat << EOF
${BOLD}Usage:${NC} $(basename "$0") [COMMAND]

${BOLD}Commands:${NC}
  start, up            Start PostgreSQL and Redis containers (default)
  stop, down           Stop and remove Docker containers
  compliance-engine    Run the compliance engine locally via Maven
  application          Run the application locally via pnpm
  all                  Build and start full stack in Docker (Postgres, Redis, Backend, Frontend)
  status               Check Docker container status and health
  logs [service]       Follow logs (e.g. ./scripts/dev.sh logs compliance-engine|backend)
  help                 Display this help message
EOF
    exit 0
}

COMMAND="${1:-start}"
shift || true

case "${COMMAND}" in
    start|up)
        start_infra
        ;;
    stop|down)
        stop_services
        ;;
    compliance-engine|backend)
        start_compliance_engine
        ;;
    application|frontend)
        start_application
        ;;
    all)
        start_all_docker
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$@"
        ;;
    help|-h|--help)
        usage
        ;;
    *)
        echo -e "${RED}Unknown command: ${COMMAND}${NC}" >&2
        usage
        ;;
esac
