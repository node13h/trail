#!/usr/bin/env bash

RESET_SQL="$1"
PG_ADDRESS="$2"
PG_PORT="$3"
PG_PASSWORD="$4"
PG_IMAGE="${5:-postgres:9.6-alpine}"

podman run -i --rm --network host --entrypoint psql \
       -e PGPASSWORD="$PG_PASSWORD" \
       "$PG_IMAGE" -h "$PG_ADDRESS" -p "$PG_PORT" -U postgres postgres \
       < "$RESET_SQL" >/dev/null
