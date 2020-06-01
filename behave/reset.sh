#!/usr/bin/env bash

PG_ADDRESS="$1"
PG_PORT="$2"
PG_PASSWORD="$3"
PG_IMAGE="${4:-postgres:9.6-alpine}"

podman run \
       -i \
       --rm \
       --network host \
       --entrypoint psql \
       -e PGPASSWORD="$PG_PASSWORD" \
       "$PG_IMAGE" \
       -h "$PG_ADDRESS" \
       -p "$PG_PORT" \
       -U postgres \
       postgres \
       < reset.sql >/dev/null
