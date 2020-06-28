#!/usr/bin/env bash

set -auo pipefail

PG_IMAGE='postgres:9.6-alpine'

ACTION="$1"
STATE_FILE="$2"
shift 2


start () {
    declare deployment_id="$1"
    declare pg_port="$2"

    if [[ -s "$STATE_FILE" ]]; then
        printf 'State file %s already exists\n' "$STATE_FILE"
        exit 1
    fi

    declare pod
    pod=$(podman pod create --name "$deployment_id" \
                 -p "${pg_port}:5432")
    cat <<EOF >>"$STATE_FILE"
POD=${pod}
EOF

    podman pod start "$pod"

    podman run --pod "$pod" -d --name "${deployment_id}-pg" \
           -e POSTGRES_PASSWORD=hunter2 \
           "$PG_IMAGE"
    cat <<EOF >>"$STATE_FILE"
PG_ADDRESS=localhost
PG_PORT=${pg_port}
EOF

    # Pod starts listening immediately after creation, so we have to do
    # proper checks
    printf 'Waiting for PostgreSQL '
    until
        podman run --rm --network host --entrypoint psql \
               -e PGPASSWORD=hunter2 \
               "$PG_IMAGE" -h 127.0.0.1 -U postgres postgres \
               -c 'SELECT 1' >/dev/null 2>/dev/null
    do
        printf '.'
        sleep 1
    done
    printf ' done.\n'

    cat <<EOF >>"$STATE_FILE"
DEPLOYMENT_ID=${deployment_id}
EOF
}


stop () {
    if ! [[ -e "$STATE_FILE" ]]; then
        return 0
    fi

    # shellcheck disable=SC1090
    source "$STATE_FILE"

    if [[ -v POD ]] && podman pod exists "$POD"; then
        podman pod rm -f "$POD"
    fi

    rm -f -- "$STATE_FILE"
}

case "$ACTION" in
    start)
        start "$@"
        ;;
    stop)
        stop "$@"
        ;;
esac

