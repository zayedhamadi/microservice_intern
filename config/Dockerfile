
# syntax=docker/dockerfile:1

# ============================================================
# GLOBAL ARGUMENTS
# ============================================================

ARG JAVA_VERSION=21
ARG MAVEN_VERSION=3.9.11


# ============================================================
# STAGE 1 : DEPENDENCIES
# ============================================================

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS dependencies

WORKDIR /build

# Copier uniquement les fichiers nécessaires à Maven.
# Cela permet de maximiser le cache Docker.
COPY pom.xml .

# Maven est déjà installé dans l'image de base.
# Pas besoin d'utiliser mvn dans Docker.


# Téléchargement des dépendances Maven avec cache BuildKit.
RUN \
    mvn -B dependency:go-offline


# ============================================================
# STAGE 2 : BUILD
# ============================================================

FROM dependencies AS builder

WORKDIR /build

COPY src ./src

RUN \
    mvn -B clean package -DskipTests


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

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

WORKDIR /app


# ============================================================
# RUNTIME DEPENDENCIES
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

ENV SPRING_PROFILES_ACTIVE=prod


# ============================================================
# JVM OPTIONS
# ============================================================

ENV JDK_JAVA_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"


# ============================================================
# NETWORK
# ============================================================

EXPOSE 8888


# ============================================================
# HEALTHCHECK
# ============================================================

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=40s \
            --retries=3 \
    CMD wget --no-verbose --tries=1 --spider \
        http://localhost:8888/actuator/health || exit 1


# ============================================================
# SIGNAL
# ============================================================

STOPSIGNAL SIGTERM


# ============================================================
# NON-ROOT
# ============================================================

USER 10001


# ============================================================
# START APPLICATION
# ============================================================

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]


