# syntax=docker/dockerfile:1

# ============================================================
# GLOBAL BUILD ARGUMENTS
# ============================================================

ARG JAVA_VERSION=21
ARG MAVEN_VERSION=3.9.11


# ============================================================
# STAGE 1 — DEPENDENCIES
# ============================================================
#
# SUPPLY-CHAIN SECURITY : figer par digest en prod. Procédure :
#   docker pull maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}
#   docker inspect --format='{{index .RepoDigests 0}}' maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}
# puis remplacer la ligne FROM ci-dessous par :
#   FROM maven:3.9.11-eclipse-temurin-21@sha256:<digest> AS dependencies
# ============================================================

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS dependencies

WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# id=m2-gateway : identifiant unique du cache pour ce service, afin
# d'éviter les collisions/écrasements quand plusieurs services sont
# buildés en parallèle (docker buildx bake / CI matrix).
RUN --mount=type=cache,target=/root/.m2,id=m2-gateway \
    ./mvnw -B dependency:go-offline


# ============================================================
# STAGE 2 — BUILD
# ============================================================

FROM dependencies AS builder

WORKDIR /build

COPY src ./src

RUN --mount=type=cache,target=/root/.m2,id=m2-gateway \
    ./mvnw -B clean package -DskipTests


# ============================================================
# STAGE 3 — SPRING BOOT LAYERS
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
# STAGE 4 — RUNTIME
# ============================================================

ARG JAVA_VERSION

# Même remarque que ci-dessus pour figer eclipse-temurin par digest.
FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

WORKDIR /app


# ============================================================
# SYSTEM DEPENDENCIES
# ============================================================
#
# apt-get upgrade -y : applique les patches de sécurité du
# système de base, standardisé sur les 5 services backend.
# wget : requis uniquement pour le HEALTHCHECK.
# ============================================================

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*


# ============================================================
# SECURITY — NON ROOT USER
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

COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/dependencies/ \
    ./

COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/spring-boot-loader/ \
    ./

COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/snapshot-dependencies/ \
    ./

COPY --from=extractor \
    --chown=10001:10001 \
    /build/extracted/application/ \
    ./


# ============================================================
# OCI IMAGE METADATA
# ============================================================

ARG GIT_COMMIT=unknown
ARG BUILD_DATE=unknown
ARG APP_VERSION=unknown

LABEL org.opencontainers.image.title="gateway-service" \
      org.opencontainers.image.description="Hirely API Gateway Service" \
      org.opencontainers.image.vendor="Hirely" \
      org.opencontainers.image.source="https://github.com/zayedhamadi/microservice_intern" \
      org.opencontainers.image.licenses="Proprietary" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${APP_VERSION}"


# ============================================================
# SPRING PROFILE
# ============================================================

ENV SPRING_PROFILES_ACTIVE=prod


# ============================================================
# JVM CONFIGURATION
# ============================================================
#
# JDK_JAVA_OPTIONS (JEP 293) est lu silencieusement par le
# lanceur java, contrairement à JAVA_TOOL_OPTIONS / _JAVA_OPTIONS
# qui polluent stderr avec "Picked up ...".
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

EXPOSE 8080


# ============================================================
# HEALTHCHECK
# ============================================================

HEALTHCHECK \
    --interval=15s \
    --timeout=5s \
    --start-period=40s \
    --retries=5 \
    CMD wget \
        --no-verbose \
        --tries=1 \
        --spider \
        http://127.0.0.1:8080/actuator/health \
        || exit 1


# ============================================================
# GRACEFUL SHUTDOWN
# ============================================================

STOPSIGNAL SIGTERM


# ============================================================
# START APPLICATION
# ============================================================

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]