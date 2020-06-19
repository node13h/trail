#!/usr/bin/env bash

set -auo pipefail

PG_IMAGE='postgres:9.6-alpine'

ACTION="$1"
shift


start () {
    declare ns="$1"
    declare state_file="$2"
    declare pg_port="$3"

    if podman pod exists "$ns"; then
        printf 'Pod %s already exists\n' "$ns"
        exit 1
    fi

    podman pod create --name "$ns" \
           -p "${pg_port}:5432"

    podman pod start "$ns"

    podman run --pod "$ns" -d --name "${ns}-pg" \
           -e POSTGRES_PASSWORD=hunter2 \
           "$PG_IMAGE"

    # Pod starts listening immediately after creation, so we have to do
    # proper checks
    until
        podman run --rm --network host --entrypoint psql \
               -e PGPASSWORD=hunter2 \
               "$PG_IMAGE" -h 127.0.0.1 -U postgres postgres \
               -c 'SELECT 1' > /dev/null 2> /dev/null
    do
        echo Wait for PostgreSQL
        sleep 1
    done

    cat <<EOF > "$state_file"
PG_ADDRESS=localhost
PG_PORT=${pg_port}
EOF
}


stop () {
    declare ns="$1"
    declare state_file="$2"

    if podman pod exists "$ns"; then
        podman pod rm -f "$ns"
    fi

    rm -f -- "$state_file"
}

case "$ACTION" in
    start)
        start "$@"
        ;;
    stop)
        stop "$@"
        ;;
esac

