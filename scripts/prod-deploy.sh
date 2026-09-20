#!/usr/bin/env bash
# ==============================================================================
# Fincomp Production Deployment Script
# Pre-flight environment validation, security audits, and zero-downtime deployment
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ANSI colors
GREEN=$'\033[0;32m'
CYAN=$'\033[0;36m'
YELLOW=$'\033[1;33m'
RED=$'\033[0;31m'
BOLD=$'\033[1m'
NC=$'\033[0m'

COMPOSE_FILE="${ROOT_DIR}/docker-compose.prod.yml"
SKIP_SECURITY_CHECK=false
BUILD_NO_CACHE=false
DOWN_MODE=false

usage() {
    cat << EOF
${BOLD}Usage:${NC} $(basename "$0") [OPTIONS]

${BOLD}Options:${NC}
  --skip-security-check   Bypass secret entropy checks (for staging/testing only)
  --no-cache              Build Docker images with --no-cache
  --down                  Tear down the production stack and exit
  --help, -h              Display this help message
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-security-check)
            SKIP_SECURITY_CHECK=true
            shift
            ;;
        --no-cache)
            BUILD_NO_CACHE=true
            shift
            ;;
        --down)
            DOWN_MODE=true
            shift
            ;;
        --help|-h)
            usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}" >&2
            usage
            ;;
    esac
done

if [ "${DOWN_MODE}" = true ]; then
    echo -e "${YELLOW}Shutting down Fincomp production stack...${NC}"
    docker compose -f "${COMPOSE_FILE}" down
    echo -e "${GREEN}Production services stopped.${NC}"
    exit 0
fi

echo -e "${BOLD}${CYAN}==================================================================${NC}"
echo -e "${BOLD}Fincomp production deployment pre-flight checks${NC}"
echo -e "${BOLD}${CYAN}==================================================================${NC}"

# 1. Verify Docker environment
if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}Error: 'docker' command is not installed.${NC}" >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker daemon is not accessible. Make sure Docker is running.${NC}" >&2
    exit 1
fi

# 2. Check root .env
if [[ ! -f "${ROOT_DIR}/.env" ]]; then
    echo -e "${RED}Error: Production .env file not found at ${ROOT_DIR}/.env!${NC}" >&2
    echo -e "${YELLOW}Please initialize .env with production credentials before deploying.${NC}" >&2
    exit 1
fi

# Load variables
set -a
source "${ROOT_DIR}/.env"
set +a

# 3. Security checks for production
FAILED_CHECKS=0

check_var() {
    local var_name="$1"
    local var_val="${!var_name:-}"
    if [[ -z "${var_val}" ]]; then
        echo -e "${RED}[✗] Required environment variable '${var_name}' is not set!${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

check_var "DOMAIN"
check_var "POSTGRES_DB"
check_var "POSTGRES_USER"
check_var "POSTGRES_PASSWORD"
check_var "REDIS_PASSWORD"
check_var "JWT_SECRET"
check_var "MFA_ENCRYPTION_KEY"
check_var "S3_BACKUP_BUCKET"
check_var "AWS_REGION"
check_var "BACKUP_ENCRYPTION_PASSPHRASE"

if [ ${FAILED_CHECKS} -gt 0 ]; then
    echo -e "${RED}Pre-flight validation failed: Missing required environment variables.${NC}" >&2
    exit 1
fi

if [ "${SKIP_SECURITY_CHECK}" = false ]; then
    echo -e "${CYAN}Auditing secrets and credentials...${NC}"

    # Dev secret patterns to block in production
    if [[ "${JWT_SECRET}" == *"404E635266556A586E3272"* ]] || [[ ${#JWT_SECRET} -lt 32 ]]; then
        echo -e "${RED}[✗] Insecure JWT_SECRET detected! Cannot use default dev secret in production.${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi

    if [[ "${MFA_ENCRYPTION_KEY}" == *"0123456789abcdef"* ]] || [[ ${#MFA_ENCRYPTION_KEY} -lt 16 ]]; then
        echo -e "${RED}[✗] Insecure MFA_ENCRYPTION_KEY detected! Cannot use default dev key in production.${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi

    if [[ "${POSTGRES_PASSWORD}" == "fincomp_dev_password" ]] || [[ ${#POSTGRES_PASSWORD} -lt 12 ]]; then
        echo -e "${RED}[✗] Insecure POSTGRES_PASSWORD detected! Must be at least 12 characters and not default dev password.${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi

    if [[ "${REDIS_PASSWORD}" == "redis_dev_password" ]] || [[ ${#REDIS_PASSWORD} -lt 12 ]]; then
        echo -e "${RED}[✗] Insecure REDIS_PASSWORD detected! Must be at least 12 characters and not default dev password.${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi

    if [[ ${#BACKUP_ENCRYPTION_PASSPHRASE} -lt 32 ]]; then
        echo -e "${RED}[✗] BACKUP_ENCRYPTION_PASSPHRASE is too short! Use at least 32 characters.${NC}" >&2
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi

    if [ ${FAILED_CHECKS} -gt 0 ]; then
        echo ""
        echo -e "${RED}Security audit failed! Weak or default credentials detected.${NC}" >&2
        echo -e "${YELLOW}Generate strong credentials with: ${BOLD}./scripts/generate-keys.sh --write${NC}" >&2
        echo -e "${YELLOW}(Or use --skip-security-check only if deploying in a simulated local test environment)${NC}" >&2
        exit 1
    fi
    echo -e "${GREEN}[✓] Credentials audit passed! Cryptographic entropy verified.${NC}"
fi

# 4. Deployment execution
echo -e "\n${BOLD}${YELLOW}Deploying Fincomp via production Compose...${NC}"

DOCKER_BUILD_ARGS=""
if [ "${BUILD_NO_CACHE}" = true ]; then
    DOCKER_BUILD_ARGS="--no-cache"
fi

docker compose -f "${COMPOSE_FILE}" up -d --build ${DOCKER_BUILD_ARGS}

echo -e "\n${BOLD}${CYAN}Waiting for services to become healthy...${NC}"

wait_for_container() {
    local container_name="$1"
    local max_retries="${2:-40}"
    local count=0

    echo -ne "${CYAN}Waiting for ${container_name}...${NC} "
    while [ $count -lt $max_retries ]; do
        local health_status
        health_status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' "${container_name}" 2>/dev/null || echo "unstarted")

        if [ "$health_status" = "healthy" ] || [ "$health_status" = "running" ]; then
            echo -e "${GREEN}[READY]${NC}"
            return 0
        fi

        sleep 2
        count=$((count + 1))
        echo -ne "."
    done

    echo -e "\n${YELLOW}Notice: Service ${container_name} is still starting up or does not have healthcheck.${NC}"
}

wait_for_container "fincomp-prod-db" 30
wait_for_container "fincomp-prod-redis" 20
wait_for_container "fincomp-grc-engine" 45
wait_for_container "fincomp-design-system" 30
wait_for_container "fincomp-prod-caddy" 15

echo ""
echo -e "${BOLD}${GREEN}==================================================================${NC}"
echo -e "${BOLD}Fincomp production stack successfully deployed.${NC}"
echo -e "${BOLD}${GREEN}==================================================================${NC}"
echo -e "  ${BOLD}Domain:${NC}            https://${DOMAIN}"
echo -e "  ${BOLD}Reverse Proxy:${NC}     Caddy (Auto-HTTPS / TLS terminating at :80 & :443)"
echo -e "  ${BOLD}Design System:${NC}     http://localhost:3000 (Internal upstream)"
echo -e "  ${BOLD}GRC Engine API:${NC}    http://localhost:8080/api (Internal upstream)"
echo ""
echo -e "  ${BOLD}Monitor logs:${NC}      docker compose -f docker-compose.prod.yml logs -f"
echo -e "  ${BOLD}Tear down:${NC}         ./scripts/prod-deploy.sh --down"
echo ""
