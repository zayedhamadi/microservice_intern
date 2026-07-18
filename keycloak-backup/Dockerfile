FROM alpine:3.20

RUN apk add --no-cache tar gzip coreutils bash

# Utilisateur non-root dédié au backup
RUN addgroup -S backup && adduser -S backup -G backup

WORKDIR /

COPY backup.sh /backup.sh
RUN chmod +x /backup.sh && chown backup:backup /backup.sh

USER backup:backup

ENTRYPOINT ["/backup.sh"]