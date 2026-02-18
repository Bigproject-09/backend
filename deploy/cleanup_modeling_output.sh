#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${OUTPUT_DIR:-/opt/randi/data/modeling/output}"
TMP_DIR="${TMP_DIR:-/opt/randi/data/modeling/tmp}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [[ ! -d "$OUTPUT_DIR" ]]; then
  echo "[WARN] OUTPUT_DIR not found: $OUTPUT_DIR"
else
  echo "[INFO] cleaning output: $OUTPUT_DIR (older than ${RETENTION_DAYS} days)"
  find "$OUTPUT_DIR" -type f -mtime +"$RETENTION_DAYS" -name "*.pptx" -print -delete
fi

if [[ ! -d "$TMP_DIR" ]]; then
  echo "[WARN] TMP_DIR not found: $TMP_DIR"
else
  echo "[INFO] cleaning tmp: $TMP_DIR (older than ${RETENTION_DAYS} days)"
  find "$TMP_DIR" -type f -mtime +"$RETENTION_DAYS" -print -delete
fi

echo "[INFO] cleanup done"
