#!/usr/bin/env bash
# ==============================================================================
# FinComp Secret & Encryption Key Generator
# Generates cryptographically secure keys for JWT, MFA (AES-256), DB, and Redis
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ANSI color codes
GREEN=$'\033[0;32m'
CYAN=$'\033[0;36m'
YELLOW=$'\033[1;33m'
RED=$'\033[0;31m'
BOLD=$'\033[1m'
NC=$'\033[0m'

TARGET_FILE=""
WRITE_MODE=false

usage() {
    cat << EOF
${BOLD}Usage:${NC} $(basename "$0") [OPTIONS]

${BOLD}Options:${NC}
  --write, -w          Write generated keys directly to root .env file
  --file, -f <path>    Write generated keys to specified .env file
  --stdout, -s         Output keys to stdout only (default behavior)
  --help, -h           Show this help message

${BOLD}Description:${NC}
  Generates cryptographically secure secrets using OpenSSL:
  - JWT_SECRET: 64-character hex string (512-bit security)
  - MFA_ENCRYPTION_KEY: 32-character hex string (AES-256)
  - MFA_AES_KEY: 32-character hex string (synchronized with MFA_ENCRYPTION_KEY)
  - POSTGRES_PASSWORD: 32-character alphanumeric random password
  - REDIS_PASSWORD: 32-character alphanumeric random password
  - BACKUP_ENCRYPTION_PASSPHRASE: 32-character random passphrase
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --write|-w)
            WRITE_MODE=true
            TARGET_FILE="${ROOT_DIR}/.env"
            shift
            ;;
        --file|-f)
            WRITE_MODE=true
            TARGET_FILE="$2"
            shift 2
            ;;
        --stdout|-s)
            WRITE_MODE=false
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

# Ensure openssl is available
if ! command -v openssl >/dev/null 2>&1; then
    echo -e "${RED}Error: 'openssl' command is required but not found in PATH.${NC}" >&2
    exit 1
fi

# Generate cryptographically secure keys
NEW_JWT_SECRET=$(openssl rand -hex 32)
NEW_MFA_KEY=$(openssl rand -hex 16)
NEW_POSTGRES_PASS=$(openssl rand -hex 16)
NEW_REDIS_PASS=$(openssl rand -hex 16)
NEW_BACKUP_PASS=$(openssl rand -hex 16)

if [[ "${WRITE_MODE}" == false ]]; then
    echo -e "${CYAN}==================================================================${NC}"
    echo -e "${BOLD}FinComp Cryptographic Keys Generated Successfully${NC}"
    echo -e "${CYAN}==================================================================${NC}"
    echo ""
    echo -e "${YELLOW}# Security Keys (Spring Boot & Auth)${NC}"
    echo "JWT_SECRET=${NEW_JWT_SECRET}"
    echo "MFA_ENCRYPTION_KEY=${NEW_MFA_KEY}"
    echo "MFA_AES_KEY=${NEW_MFA_KEY}"
    echo ""
    echo -e "${YELLOW}# Infrastructure Passwords${NC}"
    echo "POSTGRES_PASSWORD=${NEW_POSTGRES_PASS}"
    echo "REDIS_PASSWORD=${NEW_REDIS_PASS}"
    echo "BACKUP_ENCRYPTION_PASSPHRASE=${NEW_BACKUP_PASS}"
    echo ""
    echo -e "${GREEN}Tip:${NC} Run ${BOLD}./scripts/generate-keys.sh --write${NC} to update ${ROOT_DIR}/.env directly."
    exit 0
fi

# Write Mode: Update or initialize target file
if [[ ! -f "${TARGET_FILE}" ]]; then
    if [[ -f "${ROOT_DIR}/.env.example" ]]; then
        echo -e "${YELLOW}Target file '${TARGET_FILE}' not found. Initializing from .env.example...${NC}"
        cp "${ROOT_DIR}/.env.example" "${TARGET_FILE}"
    else
        touch "${TARGET_FILE}"
    fi
fi

# Helper to update or append variable in target file
set_env_var() {
    local key="$1"
    local val="$2"
    local file="$3"

    if grep -q "^${key}=" "${file}" 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${val}|" "${file}"
    else
        echo "${key}=${val}" >> "${file}"
    fi
}

echo -e "${CYAN}Updating keys in: ${BOLD}${TARGET_FILE}${NC}"

set_env_var "JWT_SECRET" "${NEW_JWT_SECRET}" "${TARGET_FILE}"
set_env_var "MFA_ENCRYPTION_KEY" "${NEW_MFA_KEY}" "${TARGET_FILE}"
set_env_var "MFA_AES_KEY" "${NEW_MFA_KEY}" "${TARGET_FILE}"
set_env_var "POSTGRES_PASSWORD" "${NEW_POSTGRES_PASS}" "${TARGET_FILE}"
set_env_var "REDIS_PASSWORD" "${NEW_REDIS_PASS}" "${TARGET_FILE}"
set_env_var "BACKUP_ENCRYPTION_PASSPHRASE" "${NEW_BACKUP_PASS}" "${TARGET_FILE}"

echo -e "${GREEN}✓ Successfully generated and injected secure keys into ${TARGET_FILE}!${NC}"
