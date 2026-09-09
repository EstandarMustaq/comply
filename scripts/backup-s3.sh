#!/bin/bash
# ==============================================================================
# FinComp AML-CFT Automated Database Backup Script to AWS S3
# Legal Mandate: Aviso n.º 10/GBM/2024 (Art. 89) - 10 Year Retention & Immutability
# ==============================================================================

set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/tmp/fincomp_backups"
DUMP_FILE="${BACKUP_DIR}/fincomp_db_${TIMESTAMP}.sql.gz"
ENCRYPTED_FILE="${DUMP_FILE}.gpg"

# Load environment if .env exists
if [ -f "/home/ubuntu/fincomp/.env" ]; then
    source "/home/ubuntu/fincomp/.env"
elif [ -f "./.env" ]; then
    source "./.env"
fi

POSTGRES_USER="${POSTGRES_USER:-fincomp_app}"
POSTGRES_DB="${POSTGRES_DB:-fincomp_db}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-fincomp-prod-db}"
S3_BUCKET="${S3_BACKUP_BUCKET:-fincomp-backups-production}"
S3_PREFIX="${S3_BACKUP_PREFIX:-daily-dumps}"
AWS_REGION="${AWS_REGION:-af-south-1}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting PostgreSQL dump for ${POSTGRES_DB}..."

# Execute pg_dump inside container with gzip compression
docker exec -t "${POSTGRES_CONTAINER}" pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" | gzip > "${DUMP_FILE}"

if [ ! -s "${DUMP_FILE}" ]; then
    echo "ERROR: Backup file ${DUMP_FILE} is empty or was not created." >&2
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Encrypting database dump with AES-256..."
if [ -n "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]; then
    gpg --batch --yes --passphrase "${BACKUP_ENCRYPTION_PASSPHRASE}" --symmetric --cipher-algo AES256 -o "${ENCRYPTED_FILE}" "${DUMP_FILE}"
    UPLOAD_TARGET="${ENCRYPTED_FILE}"
else
    echo "WARNING: BACKUP_ENCRYPTION_PASSPHRASE not set. Uploading unencrypted compressed dump."
    UPLOAD_TARGET="${DUMP_FILE}"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Uploading backup to AWS S3 (s3://${S3_BUCKET}/${S3_PREFIX}/)..."
aws s3 cp "${UPLOAD_TARGET}" "s3://${S3_BUCKET}/${S3_PREFIX}/$(basename "${UPLOAD_TARGET}")" \
    --region "${AWS_REGION}" \
    --storage-class STANDARD_IA

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup successfully uploaded to S3: s3://${S3_BUCKET}/${S3_PREFIX}/$(basename "${UPLOAD_TARGET}")"

# Clean up local temporary files
rm -f "${DUMP_FILE}" "${ENCRYPTED_FILE:-}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Local temporary files cleaned up. Backup completed successfully."
