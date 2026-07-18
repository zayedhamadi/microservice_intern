#!/bin/bash
set -e

TS=$(date +%Y%m%d_%H%M%S)
FILE="/backup/keycloak_backup_${TS}.tar.gz"

echo "Backing up /data -> $FILE"
tar czf "$FILE" -C /data .

echo "Done: $FILE"
ls -lh /backup