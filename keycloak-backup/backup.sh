#!/bin/bash
set -Eeuo pipefail

# ============================================================
# KEYCLOAK BACKUP JOB
# ============================================================
# Sauvegarde de /data vers /backup, avec :
#   - écriture atomique (fichier temporaire + mv)
#   - vérification d'intégrité de l'archive
#   - checksum SHA-256
#   - chiffrement au repos (GPG symétrique AES256)
#   - copie hors-site optionnelle (rsync/SSH) -> principe 3-2-1
#   - notification webhook en cas de succès/échec
#   - métriques Prometheus Pushgateway
#   - politique de rétention avec minimum garanti
#
# Variables configurables :
#   DATA_DIR=/data
#   BACKUP_DIR=/backup
#   RETENTION_DAYS=7
#   MIN_BACKUPS_KEPT=3
#
#   ENCRYPT=true                     # chiffrer l'archive avant stockage
#   GPG_PASSPHRASE_FILE=             # obligatoire si ENCRYPT=true
#
#   OFFSITE_ENABLED=false            # copier l'archive vers un hôte distant
#   REMOTE_HOST=
#   REMOTE_USER=
#   REMOTE_PATH=
#   SSH_KEY_FILE=
#   SSH_PORT=22
#
#   WEBHOOK_URL=                     # notification Slack/Discord (payload {"text":...})
#   PUSHGATEWAY_URL=                 # ex: http://pushgateway:9091
#   PUSHGATEWAY_JOB=keycloak_backup
#
# Exemple :
#   DATA_DIR=/data BACKUP_DIR=/backup RETENTION_DAYS=14 \
#   ENCRYPT=true GPG_PASSPHRASE_FILE=/run/secrets/backup_gpg_pass \
#   OFFSITE_ENABLED=true REMOTE_HOST=backup.example.com \
#   REMOTE_USER=backup REMOTE_PATH=/srv/keycloak-backups \
#   SSH_KEY_FILE=/run/secrets/backup_ssh_key \
#   ./backup.sh
# ============================================================

# ============================================================
# CONFIGURATION
# ============================================================

DATA_DIR="${DATA_DIR:-/data}"
BACKUP_DIR="${BACKUP_DIR:-/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
MIN_BACKUPS_KEPT="${MIN_BACKUPS_KEPT:-3}"

ENCRYPT="${ENCRYPT:-true}"
GPG_PASSPHRASE_FILE="${GPG_PASSPHRASE_FILE:-}"

OFFSITE_ENABLED="${OFFSITE_ENABLED:-false}"
REMOTE_HOST="${REMOTE_HOST:-}"
REMOTE_USER="${REMOTE_USER:-}"
REMOTE_PATH="${REMOTE_PATH:-}"
SSH_KEY_FILE="${SSH_KEY_FILE:-}"
SSH_PORT="${SSH_PORT:-22}"

WEBHOOK_URL="${WEBHOOK_URL:-}"
PUSHGATEWAY_URL="${PUSHGATEWAY_URL:-}"
PUSHGATEWAY_JOB="${PUSHGATEWAY_JOB:-keycloak_backup}"

TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"

FILENAME="keycloak_backup_${TIMESTAMP}.tar.gz"
TMP_FILE="${BACKUP_DIR}/.${FILENAME}.tmp"
PLAIN_FILE="${BACKUP_DIR}/${FILENAME}"
LOCK_FILE="${BACKUP_DIR}/.backup.lock"

# Renseignés au fil du script ; lus par le trap EXIT même en cas
# d'échec précoce, d'où l'initialisation à des valeurs neutres.
FINAL_FILE=""
CHECKSUM_FILE=""
SIZE_BYTES=0
OFFSITE_STATUS="disabled"

START_TIME="$(date +%s)"

# Les sauvegardes peuvent contenir des informations sensibles.
umask 077

# ============================================================
# LOGGING
# ============================================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

error() {
    log "ERROR: $*" >&2
}

# ============================================================
# NOTIFICATIONS
# ============================================================

notify() {
    local status="$1" message="$2" emoji="OK"
    [[ -z "$WEBHOOK_URL" ]] && return 0
    [[ "$status" == "failure" ]] && emoji="FAIL"

    curl -fsS -m 10 -X POST \
        -H 'Content-Type: application/json' \
        -d "{\"text\":\"[${emoji}] [keycloak-backup] ${message}\"}" \
        "$WEBHOOK_URL" >/dev/null 2>&1 \
        || log "Avertissement : l'envoi de la notification webhook a échoué."
}

push_metrics() {
    local success="$1" duration="$2" size="$3"
    [[ -z "$PUSHGATEWAY_URL" ]] && return 0

    cat <<-EOF | curl -fsS -m 10 --data-binary @- \
        "${PUSHGATEWAY_URL}/metrics/job/${PUSHGATEWAY_JOB}" >/dev/null 2>&1 \
        || log "Avertissement : le push des métriques vers Pushgateway a échoué."
        keycloak_backup_success ${success}
        keycloak_backup_duration_seconds ${duration}
        keycloak_backup_size_bytes ${size}
        keycloak_backup_last_run_timestamp $(date +%s)
        EOF
}

# ============================================================
# CLEANUP
# ============================================================

cleanup() {
    if [[ -f "$TMP_FILE" ]]; then
        log "Suppression de l'archive temporaire : $TMP_FILE"
        rm -f -- "$TMP_FILE" || true
    fi
}

on_exit() {
    local exit_code=$? end_time duration
    end_time="$(date +%s)"
    duration=$(( end_time - START_TIME ))

    if (( exit_code == 0 )); then
        log "Job terminé avec succès (code de sortie : ${exit_code})."
        notify "success" "Backup terminé en ${duration}s (${SIZE_BYTES} bytes). Offsite: ${OFFSITE_STATUS}."
        push_metrics 1 "$duration" "$SIZE_BYTES"
    else
        error "Job terminé en échec (code de sortie : ${exit_code})."
        notify "failure" "Échec du backup après ${duration}s (code ${exit_code})."
        push_metrics 0 "$duration" "$SIZE_BYTES"
    fi
}

trap cleanup ERR
trap 'cleanup; exit 130' INT
trap 'cleanup; exit 143' TERM
trap on_exit EXIT

# ============================================================
# VALIDATION DES PARAMÈTRES
# ============================================================

if ! [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]]; then
    error "RETENTION_DAYS doit être un entier positif ou 0."
    exit 1
fi

if ! [[ "$MIN_BACKUPS_KEPT" =~ ^[1-9][0-9]*$ ]]; then
    error "MIN_BACKUPS_KEPT doit être un entier supérieur ou égal à 1."
    exit 1
fi

if [[ "$ENCRYPT" == "true" ]]; then
    if [[ -z "$GPG_PASSPHRASE_FILE" ]]; then
        error "ENCRYPT=true requiert GPG_PASSPHRASE_FILE."
        exit 1
    fi
    if [[ ! -r "$GPG_PASSPHRASE_FILE" ]]; then
        error "GPG_PASSPHRASE_FILE '$GPG_PASSPHRASE_FILE' est introuvable ou illisible."
        exit 1
    fi
fi

if [[ "$OFFSITE_ENABLED" == "true" ]]; then
    for var_name in REMOTE_HOST REMOTE_USER REMOTE_PATH SSH_KEY_FILE; do
        if [[ -z "${!var_name}" ]]; then
            error "OFFSITE_ENABLED=true requiert ${var_name}."
            exit 1
        fi
    done
    if [[ ! -r "$SSH_KEY_FILE" ]]; then
        error "SSH_KEY_FILE '$SSH_KEY_FILE' est introuvable ou illisible."
        exit 1
    fi
fi

# ============================================================
# VALIDATION SOURCE
# ============================================================

if [[ ! -d "$DATA_DIR" ]]; then
    error "Le répertoire source '$DATA_DIR' n'existe pas."
    exit 1
fi

if [[ ! -r "$DATA_DIR" ]]; then
    error "Le répertoire source '$DATA_DIR' n'est pas accessible en lecture."
    exit 1
fi

# ============================================================
# DESTINATION
# ============================================================

mkdir -p "$BACKUP_DIR"

if [[ ! -d "$BACKUP_DIR" ]]; then
    error "Impossible de créer '$BACKUP_DIR'."
    exit 1
fi

if [[ ! -w "$BACKUP_DIR" ]]; then
    error "Le répertoire '$BACKUP_DIR' n'est pas accessible en écriture."
    exit 1
fi

# ============================================================
# PROTECTION CONTRE DATA_DIR == BACKUP_DIR
# ============================================================

DATA_REAL="$(readlink -f "$DATA_DIR")"
BACKUP_REAL="$(readlink -f "$BACKUP_DIR")"

if [[ "$DATA_REAL" == "$BACKUP_REAL" ]]; then
    error "DATA_DIR et BACKUP_DIR ne peuvent pas être identiques."
    exit 1
fi

# ============================================================
# LOCK
# ============================================================
# Empêche deux sauvegardes simultanées.

exec 200>"$LOCK_FILE"

if ! flock -n 200; then
    error "Une sauvegarde Keycloak est déjà en cours."
    exit 1
fi

# ============================================================
# VALIDATION DU CONTENU
# ============================================================

if [[ -z "$(find "$DATA_DIR" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
    error "Le répertoire source '$DATA_DIR' est vide."
    exit 1
fi

# ============================================================
# VÉRIFICATION ESPACE DISQUE
# ============================================================

DATA_SIZE_KB="$(du -sk "$DATA_DIR" | awk '{print $1}')"
AVAILABLE_KB="$(df -Pk "$BACKUP_DIR" | awk 'NR==2 {print $4}')"

if [[ -z "$DATA_SIZE_KB" || -z "$AVAILABLE_KB" ]]; then
    error "Impossible de déterminer l'espace disque disponible."
    exit 1
fi

# Marge de sécurité de 20 %.
REQUIRED_KB=$(( DATA_SIZE_KB + DATA_SIZE_KB / 5 ))

if (( AVAILABLE_KB < REQUIRED_KB )); then
    error "Espace disque insuffisant."
    error "Disponible : ${AVAILABLE_KB} KB"
    error "Requis approximatif : ${REQUIRED_KB} KB"
    exit 1
fi

# ============================================================
# INFORMATIONS
# ============================================================

log "============================================================"
log "KEYCLOAK BACKUP"
log "============================================================"
log "Source          : $DATA_DIR"
log "Destination     : $BACKUP_DIR"
log "Rétention       : ${RETENTION_DAYS} jour(s)"
log "Minimum         : ${MIN_BACKUPS_KEPT} backup(s)"
log "Chiffrement     : ${ENCRYPT}"
log "Copie hors-site : ${OFFSITE_ENABLED}"
log "Archive         : $PLAIN_FILE"
log "============================================================"

# ============================================================
# SAUVEGARDE
# ============================================================

log "Démarrage de la sauvegarde..."

tar \
    --create \
    --gzip \
    --file="$TMP_FILE" \
    --directory="$DATA_DIR" \
    .

log "Archive temporaire créée avec succès."

# ============================================================
# VALIDATION DE L'ARCHIVE
# ============================================================

log "Vérification de l'intégrité de l'archive..."

if ! tar -tzf "$TMP_FILE" >/dev/null; then
    error "L'archive générée est invalide ou corrompue."
    exit 1
fi

log "Archive valide."

# ============================================================
# FINALISATION ATOMIQUE
# ============================================================

mv -- "$TMP_FILE" "$PLAIN_FILE"

log "Archive finalisée : $PLAIN_FILE"

# ============================================================
# CHIFFREMENT AU REPOS (AES256, GPG symétrique)
# ============================================================
# L'archive en clair n'est jamais conservée sur disque une fois
# chiffrée : elle est effacée de façon sécurisée (shred).
# ============================================================

if [[ "$ENCRYPT" == "true" ]]; then

    log "Chiffrement de l'archive (AES256)..."

    if ! gpg --batch --yes --pinentry-mode loopback \
        --passphrase-file "$GPG_PASSPHRASE_FILE" \
        --symmetric --cipher-algo AES256 \
        --output "${PLAIN_FILE}.gpg" \
        "$PLAIN_FILE"; then
        error "Le chiffrement GPG a échoué."
        rm -f -- "$PLAIN_FILE"
        exit 1
    fi

    shred -u "$PLAIN_FILE" 2>/dev/null || rm -f -- "$PLAIN_FILE"

    FINAL_FILE="${PLAIN_FILE}.gpg"
    log "Archive chiffrée : $FINAL_FILE"
    log "Archive en clair supprimée de façon sécurisée."
else
    FINAL_FILE="$PLAIN_FILE"
fi

CHECKSUM_FILE="${FINAL_FILE}.sha256"

# ============================================================
# SHA-256
# ============================================================

log "Génération du checksum SHA-256..."

sha256sum "$FINAL_FILE" > "$CHECKSUM_FILE"

if ! sha256sum -c "$CHECKSUM_FILE" --status; then
    error "Échec de la vérification SHA-256."
    rm -f -- "$FINAL_FILE" "$CHECKSUM_FILE"
    exit 1
fi

log "Checksum vérifié avec succès."

# ============================================================
# INFORMATIONS BACKUP
# ============================================================

SIZE_BYTES="$(stat -c '%s' "$FINAL_FILE")"
SIZE_HUMAN="$(du -h "$FINAL_FILE" | cut -f1)"

log "Taille       : $SIZE_HUMAN"
log "Taille bytes : $SIZE_BYTES"

# ============================================================
# COPIE HORS-SITE (principe 3-2-1)
# ============================================================
# Un échec de la copie hors-site n'invalide pas la sauvegarde
# locale (qui reste utilisable), mais est signalé clairement
# via les logs, la notification webhook et les métriques.
# ============================================================

if [[ "$OFFSITE_ENABLED" == "true" ]]; then

    log "Copie hors-site vers ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH} ..."

    if rsync -az \
        -e "ssh -i ${SSH_KEY_FILE} -p ${SSH_PORT} -o StrictHostKeyChecking=accept-new -o BatchMode=yes" \
        "$FINAL_FILE" "$CHECKSUM_FILE" \
        "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/"; then
        OFFSITE_STATUS="ok"
        log "Copie hors-site terminée avec succès."
    else
        OFFSITE_STATUS="failed"
        error "La copie hors-site a échoué (la sauvegarde locale reste disponible)."
    fi
fi

# ============================================================
# RÉTENTION
# ============================================================
# On supprime uniquement les anciennes archives lorsqu'il
# restera toujours au minimum MIN_BACKUPS_KEPT backups.
# Le motif couvre les archives en clair (.tar.gz) et chiffrées
# (.tar.gz.gpg), au cas où ENCRYPT aurait changé entre deux runs.
# ============================================================

if (( RETENTION_DAYS > 0 )); then

    log "Application de la politique de rétention..."

    mapfile -t BACKUPS < <(
        find "$BACKUP_DIR" \
            -maxdepth 1 \
            -type f \
            \( -name "keycloak_backup_*.tar.gz" -o -name "keycloak_backup_*.tar.gz.gpg" \) \
            -printf '%T@ %p\n' \
        | sort -n \
        | cut -d' ' -f2-
    )

    TOTAL_BACKUPS="${#BACKUPS[@]}"

    log "Nombre actuel de sauvegardes : ${TOTAL_BACKUPS}"

    if (( TOTAL_BACKUPS > MIN_BACKUPS_KEPT )); then

        for BACKUP in "${BACKUPS[@]}"; do

            CURRENT_COUNT="$(
                find "$BACKUP_DIR" \
                    -maxdepth 1 \
                    -type f \
                    \( -name "keycloak_backup_*.tar.gz" -o -name "keycloak_backup_*.tar.gz.gpg" \) \
                | wc -l
            )"

            if (( CURRENT_COUNT <= MIN_BACKUPS_KEPT )); then
                break
            fi

            if find "$BACKUP_DIR" \
                -maxdepth 1 \
                -type f \
                -name "$(basename "$BACKUP")" \
                -mtime "+${RETENTION_DAYS}" \
                -print -quit \
                | grep -q .; then

                log "Suppression de l'ancienne sauvegarde : $BACKUP"

                rm -f -- "$BACKUP"
                rm -f -- "${BACKUP}.sha256"
            fi
        done

    else
        log "Aucune purge : minimum de ${MIN_BACKUPS_KEPT} sauvegardes requis."
    fi

else
    log "Rétention désactivée : RETENTION_DAYS=0."
fi

# ============================================================
# RÉSUMÉ FINAL
# ============================================================

log "============================================================"
log "BACKUP KEYCLOAK TERMINÉ AVEC SUCCÈS"
log "============================================================"
log "Archive         : $FINAL_FILE"
log "SHA-256         : $CHECKSUM_FILE"
log "Taille          : $SIZE_HUMAN"
log "Copie hors-site : ${OFFSITE_STATUS}"
log "============================================================"

log "Sauvegardes disponibles :"

find "$BACKUP_DIR" \
    -maxdepth 1 \
    -type f \
    \( -name "keycloak_backup_*.tar.gz" -o -name "keycloak_backup_*.tar.gz.gpg" \) \
    -printf '%TY-%Tm-%Td %TH:%TM:%TS | %10s bytes | %p\n' \
    | sort -r

log "============================================================"

exit 0