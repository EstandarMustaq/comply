#!/usr/bin/env bash
# ==============================================================================
# FinComp Full Stack Build Script
# Compiles and validates both Backend (Spring Boot 3) and Frontend (Next.js 16)
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

START_TIME=$(date +%s)
SKIP_TESTS=true

while [[ $# -gt 0 ]]; do
    case "$1" in
        --with-tests|-t)
            SKIP_TESTS=false
            shift
            ;;
        --help|-h)
            echo "Usage: $(basename "$0") [--with-tests]"
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 1
            ;;
    esac
done

echo -e "${BOLD}${CYAN}==================================================================${NC}"
echo -e "${BOLD}Starting FinComp Full Stack Build Pipeline${NC}"
echo -e "${BOLD}${CYAN}==================================================================${NC}"

# 1. Build Backend
echo -e "\n${BOLD}${YELLOW}[1/2] Building Compliance Engine (Spring Boot 3 + Maven)...${NC}"
cd "${ROOT_DIR}/package/compliance-engine"

if [ "$SKIP_TESTS" = true ]; then
    echo -e "${CYAN}Running: mvn clean package -DskipTests${NC}"
    mvn clean package -DskipTests
else
    echo -e "${CYAN}Running: mvn clean package (with unit tests)${NC}"
    mvn clean package
fi

BACKEND_JAR=$(find target -name "fincomp-backend-*.jar" ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | head -n 1)

if [[ -z "${BACKEND_JAR}" || ! -f "${BACKEND_JAR}" ]]; then
    echo -e "${RED}Error: Compliance Engine JAR not found in package/compliance-engine/target! Build failed.${NC}" >&2
    exit 1
fi

JAR_SIZE=$(ls -lh "${BACKEND_JAR}" | awk '{print $5}')
echo -e "${GREEN}[✓] Backend build succeeded: ${BACKEND_JAR} (${JAR_SIZE})${NC}"

# 2. Build Frontend
echo -e "\n${BOLD}${YELLOW}[2/2] Building Application (Next.js 16 + Turbopack)...${NC}"
cd "${ROOT_DIR}/app/application"

# Ensure .env.local exists for build-time validation
if [[ ! -f ".env.local" ]]; then
    echo -e "${YELLOW}Notice: .env.local not found in app/application. Copying from .env.example...${NC}"
    cp ".env.example" ".env.local"
fi

echo -e "${CYAN}Running: pnpm build${NC}"
pnpm build

if [[ ! -d ".next" ]]; then
    echo -e "${RED}Error: Next.js .next build directory was not generated! Build failed.${NC}" >&2
    exit 1
fi

echo -e "${GREEN}[✓] Frontend Next.js 16 build succeeded!${NC}"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo -e "${BOLD}${GREEN}==================================================================${NC}"
echo -e "${BOLD}FinComp Full Stack Build Completed Successfully in ${DURATION}s!${NC}"
echo -e "${BOLD}${GREEN}==================================================================${NC}"
echo -e "  ${BOLD}Backend Artifact:${NC}  ${BACKEND_JAR} (${JAR_SIZE})"
echo -e "  ${BOLD}Application Artifact:${NC} app/application/.next (Production Turbopack bundle)"
echo ""
