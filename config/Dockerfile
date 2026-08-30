# syntax=docker/dockerfile:1

# ============================================================
# GLOBAL ARGUMENTS
# ============================================================

ARG JAVA_VERSION=21
ARG MAVEN_VERSION=3.9.11


# ============================================================
# STAGE 1 : DEPENDENCIES
# ============================================================
#
# SUPPLY-CHAIN SECURITY : pour figer la version exacte de l'image
# de base (recommandé en prod), remplacer la ligne FROM par un
# digest obtenu via :
#   docker pull maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}
#   docker inspect --format='{{index .RepoDigests 0}}' maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}
# puis :
#   FROM maven:3.9.11-eclipse-temurin-21@sha256:<digest> AS dependencies
# ============================================================

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS dependencies

WORKDIR /build

# Copier uniquement les fichiers nécessaires à Maven.
# Cela permet de maximiser le cache Docker.
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# Téléchargement des dépendances Maven avec cache BuildKit.
# id=m2-config : identifiant unique du cache pour ce service,
# afin d'éviter les collisions/écrasements quand plusieurs
# services sont buildés en parallèle (docker buildx bake / CI matrix).
RUN --mount=type=cache,target=/root/.m2,id=m2-config \
    ./mvnw -B dependency:go-offline


# ============================================================
# STAGE 2 : BUILD
# ============================================================

FROM dependencies AS builder

WORKDIR /build

COPY src ./src

RUN --mount=type=cache,target=/root/.m2,id=m2-config \
    ./mvnw -B clean package -DskipTests


# ============================================================
# STAGE 3 : SPRING BOOT LAYERS
# ============================================================

FROM builder AS extractor

WORKDIR /build

RUN mkdir -p /build/extracted \
    && java \
        -Djarmode=layertools \
        -jar target/*.jar \
        extract \
        --destination /build/extracted


# ============================================================
# STAGE 4 : RUNTIME
# ============================================================

ARG JAVA_VERSION

# Même remarque que ci-dessus pour figer eclipse-temurin par digest.
FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

WORKDIR /app


# ============================================================
# RUNTIME DEPENDENCIES
# ============================================================
#
# apt-get upgrade -y : FIX — manquait dans l'original (seul service
# sur les 5 à ne pas appliquer les patches de sécurité du système
# de base). Désormais standardisé.
# wget est utilisé uniquement pour le HEALTHCHECK ci-dessous.
# Si l'orchestrateur (docker-compose healthcheck / k8s livenessProbe)
# fait déjà ce contrôle en dehors du conteneur, ce paquet peut être
# retiré pour réduire la surface d'attaque de l'image runtime.
# ============================================================

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*


# ============================================================
# NON-ROOT USER
# ============================================================

RUN groupadd \
        --system \
        --gid 10001 \
        spring \
    && useradd \
        --system \
        --uid 10001 \
        --gid 10001 \
        --no-create-home \
        --shell /usr/sbin/nologin \
        spring


# ============================================================
# SPRING BOOT LAYERS
# ============================================================

# Dependencies
COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/dependencies/ \
    ./

# Spring Boot loader
COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/spring-boot-loader/ \
    ./

# Snapshot dependencies
COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/snapshot-dependencies/ \
    ./

# Application
COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/application/ \
    ./


# ============================================================
# IMAGE METADATA
# ============================================================

ARG GIT_COMMIT=unknown
ARG BUILD_DATE=unknown
ARG APP_VERSION=unknown

LABEL org.opencontainers.image.title="config-server" \
      org.opencontainers.image.description="Hirely Spring Cloud Config Server" \
      org.opencontainers.image.vendor="Hirely" \
      org.opencontainers.image.source="https://github.com/zayedhamadi/microservice_intern" \
      org.opencontainers.image.licenses="Proprietary" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${APP_VERSION}"


# ============================================================
# SPRING PROFILE
# ============================================================
#
# FIX : manquait dans l'original, désormais standardisé sur les
# 5 services backend.
# ============================================================

ENV SPRING_PROFILES_ACTIVE=prod


# ============================================================
# JVM CONFIGURATION
# ============================================================
#
# JDK_JAVA_OPTIONS (JEP 293) est lu silencieusement par le
# lanceur java, contrairement à JAVA_TOOL_OPTIONS / _JAVA_OPTIONS
# qui impriment "Picked up ..." sur stderr à chaque invocation et
# polluent les logs applicatifs.
# -XX:+UseContainerSupport est explicite ici par cohérence avec
# les autres services (déjà activé par défaut sur JDK21, mais
# une valeur explicite évite toute ambiguïté lors d'un audit).
# ============================================================

ENV JDK_JAVA_OPTIONS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:InitialRAMPercentage=25.0 \
-XX:+UseG1GC \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/./urandom"


# ============================================================
# SECURITY
# ============================================================

USER 10001:10001


# ============================================================
# NETWORK
# ============================================================

EXPOSE 8888


# ============================================================
# HEALTHCHECK
# ============================================================

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=5 \
    CMD wget \
        --no-verbose \
        --tries=1 \
        --spider \
        http://127.0.0.1:8888/actuator/health \
        || exit 1


STOPSIGNAL SIGTERM


ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]