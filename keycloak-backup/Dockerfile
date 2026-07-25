FROM alpine:3.20

LABEL org.opencontainers.image.description="Job de sauvegarde du volume de données Keycloak"

RUN apk add --no-cache tar gzip coreutils bash

RUN addgroup -S backup && adduser -S backup -G backup

WORKDIR /

COPY --chown=backup:backup backup.sh /backup.sh
RUN chmod +x /backup.sh

USER backup:backup

ENTRYPOINT ["/backup.sh"]