#!/bin/bash
set -euo pipefail

DATA_DIR="/data"
LOG_DIR="/logs"
LOG_FILE="$LOG_DIR/upload_$(date +%Y%m%d_%H%M%S).log"

# All output goes to both stdout (visible in docker logs) and the log file
exec > >(tee -a "$LOG_FILE") 2>&1

timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

echo "[$(timestamp)] AzCopy upload starting"

# Validate that the SAS URI is configured
if [ -z "${AZCOPY_SAS_URI:-}" ]; then
    echo "[$(timestamp)] ERROR: AZCOPY_SAS_URI is not set. Check your .env file."
    exit 1
fi

upload_file() {
    local file="$1"
    local filename
    filename=$(basename "$file")

    echo "[$(timestamp)] Uploading: $filename"
    azcopy copy "$file" "$AZCOPY_SAS_URI" --log-level INFO
    echo "[$(timestamp)] Finished:  $filename"
}

if [ -n "${1:-}" ]; then
    # ── Single file mode ──────────────────────────────────────────────────────
    # Usage: docker compose run azcopy <filename.csv>
    FILE="$DATA_DIR/$1"

    if [ ! -f "$FILE" ]; then
        echo "[$(timestamp)] ERROR: File not found: $FILE"
        echo "[$(timestamp)] Make sure '$1' is placed in the ./data directory."
        exit 1
    fi

    upload_file "$FILE"
else
    # ── Batch mode ────────────────────────────────────────────────────────────
    # Usage: docker compose run azcopy
    # Uploads every *.csv found in ./data
    shopt -s nullglob
    CSV_FILES=("$DATA_DIR"/*.csv)

    if [ ${#CSV_FILES[@]} -eq 0 ]; then
        echo "[$(timestamp)] ERROR: No CSV files found in $DATA_DIR"
        echo "[$(timestamp)] Drop CSV files into the ./data directory and try again."
        exit 1
    fi

    echo "[$(timestamp)] Batch mode: found ${#CSV_FILES[@]} CSV file(s)"
    for file in "${CSV_FILES[@]}"; do
        upload_file "$file"
    done
fi

echo "[$(timestamp)] All uploads complete. Log saved to $LOG_FILE"
