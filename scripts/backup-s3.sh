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
UPLOAD_TARGET=""

cleanup() {
    rm -f "${DUMP_FILE}" "${ENCRYPTED_FILE}"
}
trap cleanup EXIT

# Load environment if .env exists
if [ -f "/home/ubuntu/fincomp/.env" ]; then
    source "/home/ubuntu/fincomp/.env"
elif [ -f "./.env" ]; then
    source "./.env"
fi

POSTGRES_USER="${POSTGRES_USER:-fincomp_app}"
POSTGRES_DB="${POSTGRES_DB:-fincomp_db}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-fincomp-prod-db}"
S3_BUCKET="${S3_BACKUP_BUCKET:-}"
S3_PREFIX="${S3_BACKUP_PREFIX:-daily-dumps}"
AWS_REGION="${AWS_REGION:-}"
BACKUP_ENCRYPTION_PASSPHRASE="${BACKUP_ENCRYPTION_PASSPHRASE:-}"
BACKUP_OBJECT_LOCK_RETENTION_YEARS="${BACKUP_OBJECT_LOCK_RETENTION_YEARS:-10}"

required_commands=(docker gzip gpg aws date stat sha256sum awk)
for command_name in "${required_commands[@]}"; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "ERROR: Required command is not installed: ${command_name}" >&2
        exit 1
    fi
done

if [[ -z "${S3_BUCKET}" || -z "${AWS_REGION}" || -z "${BACKUP_ENCRYPTION_PASSPHRASE}" ]]; then
    echo "ERROR: S3_BACKUP_BUCKET, AWS_REGION and BACKUP_ENCRYPTION_PASSPHRASE are required." >&2
    echo "ERROR: Refusing to create or upload an unencrypted or non-retained backup." >&2
    exit 1
fi

if ! [[ "${BACKUP_OBJECT_LOCK_RETENTION_YEARS}" =~ ^[1-9][0-9]*$ ]]; then
    echo "ERROR: BACKUP_OBJECT_LOCK_RETENTION_YEARS must be a positive integer." >&2
    exit 1
fi

RETENTION_DATE=$(date -u -d "+${BACKUP_OBJECT_LOCK_RETENTION_YEARS} years" +"%Y-%m-%dT%H:%M:%SZ")

mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting PostgreSQL dump for ${POSTGRES_DB}..."

# Execute pg_dump inside container with gzip compression
docker exec -t "${POSTGRES_CONTAINER}" pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" | gzip > "${DUMP_FILE}"

if [ ! -s "${DUMP_FILE}" ]; then
    echo "ERROR: Backup file ${DUMP_FILE} is empty or was not created." >&2
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Encrypting database dump with AES-256..."
gpg --batch --yes --pinentry-mode loopback \
    --passphrase "${BACKUP_ENCRYPTION_PASSPHRASE}" \
    --symmetric --cipher-algo AES256 \
    -o "${ENCRYPTED_FILE}" "${DUMP_FILE}"

if [ ! -s "${ENCRYPTED_FILE}" ]; then
    echo "ERROR: Encrypted backup file was not created." >&2
    exit 1
fi
UPLOAD_TARGET="${ENCRYPTED_FILE}"
CHECKSUM=$(sha256sum "${UPLOAD_TARGET}" | awk '{print $1}')

OBJECT_KEY="${S3_PREFIX}/$(basename "${UPLOAD_TARGET}")"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Uploading encrypted backup with ${BACKUP_OBJECT_LOCK_RETENTION_YEARS}-year Object Lock retention..."
aws s3 cp "${UPLOAD_TARGET}" "s3://${S3_BUCKET}/${S3_PREFIX}/$(basename "${UPLOAD_TARGET}")" \
    --region "${AWS_REGION}" \
    --storage-class STANDARD_IA \
    --checksum-algorithm SHA256 \
    --metadata "sha256=${CHECKSUM}" \
    --object-lock-mode COMPLIANCE \
    --object-lock-retain-until-date "${RETENTION_DATE}"

REMOTE_SIZE=$(aws s3api head-object \
    --bucket "${S3_BUCKET}" \
    --key "${OBJECT_KEY}" \
    --region "${AWS_REGION}" \
    --query 'ContentLength' \
    --output text)
LOCAL_SIZE=$(stat -c '%s' "${UPLOAD_TARGET}")

if [[ "${REMOTE_SIZE}" != "${LOCAL_SIZE}" ]]; then
    echo "ERROR: Uploaded backup size does not match local encrypted backup." >&2
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup successfully uploaded and verified: s3://${S3_BUCKET}/${OBJECT_KEY}"
