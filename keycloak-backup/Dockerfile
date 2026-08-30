# syntax=docker/dockerfile:1

# ============================================================
# KEYCLOAK BACKUP
# ============================================================
#
# Image dédiée au job de sauvegarde Keycloak.
#
# /data   -> données Keycloak à sauvegarder
# /backup -> destination des sauvegardes
#
# SUPPLY-CHAIN SECURITY : pour figer par digest,
#   docker pull alpine:3.20.6
#   docker inspect --format='{{index .RepoDigests 0}}' alpine:3.20.6
# puis : FROM alpine:3.20.6@sha256:<digest>
# ============================================================

FROM alpine:3.20.6


# ============================================================
# IMAGE METADATA
# ============================================================

ARG GIT_COMMIT=unknown
ARG BUILD_DATE=unknown
ARG APP_VERSION=1.1.0

LABEL org.opencontainers.image.title="keycloak-backup" \
      org.opencontainers.image.description="Secure Keycloak backup job" \
      org.opencontainers.image.vendor="Hirely" \
      org.opencontainers.image.source="https://github.com/zayedhamadi/microservice_intern" \
      org.opencontainers.image.licenses="Proprietary" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${APP_VERSION}"


# ============================================================
# RUNTIME DEPENDENCIES
# ============================================================
#
# bash       -> exécution du script
# coreutils  -> sha256sum, stat, date, readlink, du, df...
# findutils  -> find
# gzip       -> compression
# tar        -> archivage
# util-linux -> flock
# gnupg      -> chiffrement des archives au repos (AES256)
# openssh-client / rsync -> copie hors-site (principe 3-2-1)
#
# ============================================================

RUN apk add --no-cache \
    bash \
    coreutils \
    findutils \
    gzip \
    tar \
    util-linux \
    gnupg \
    openssh-client \
    rsync \
    curl


# ============================================================
# NON-ROOT USER
# ============================================================

RUN addgroup \
        --system \
        --gid 10001 \
        backup \
    && adduser \
        --system \
        --uid 10001 \
        --ingroup backup \
        --no-create-home \
        --shell /sbin/nologin \
        backup


# ============================================================
# DIRECTORIES
# ============================================================
#
# /data   -> source des données Keycloak
# /backup -> stockage des backups
#
# Ces répertoires seront montés avec Docker Compose.
#
# ============================================================

RUN mkdir -p /data /backup \
    && chown -R 10001:10001 /data /backup \
    && chmod 700 /data /backup


# ============================================================
# BACKUP SCRIPT
# ============================================================

COPY --chown=10001:10001 backup.sh /backup.sh

RUN chmod 750 /backup.sh


# ============================================================
# SECURITY
# ============================================================

USER 10001:10001


# ============================================================
# WORKING DIRECTORY
# ============================================================

WORKDIR /backup


# ============================================================
# CONTAINER ENTRYPOINT
# ============================================================

ENTRYPOINT ["/backup.sh"]