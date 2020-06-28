#!/usr/bin/env bash

set -euo pipefail

PG_IMAGE='postgres:9.6-alpine'

ACTION="$1"
STATE_FILE="$2"
shift 2


start () {
    declare deployment_id="$1"
    declare dev_stack_state_file="$2"
    declare reset_sql_file="$3"
    declare app_port="$4"

    if [[ -s "$STATE_FILE" ]]; then
        printf 'State file %s already exists\n' "$STATE_FILE"
        exit 1
    fi

    # shellcheck disable=SC1090
    source "$dev_stack_state_file"

    declare app_image
    app_image=$(podman build -q .)

    podman run --rm --network host \
           -e "DATABASE_URL=postgres://postgres:hunter2@${PG_ADDRESS}:${PG_PORT}/postgres" \
           "$app_image" migrate

    podman run --rm --network host --entrypoint pg_dump \
           -e PGPASSWORD=hunter2 \
           "$PG_IMAGE" -h "$PG_ADDRESS" -p "$PG_PORT" -U postgres -c postgres >"$reset_sql_file"

    declare app_container
    app_container=$(podman run -d --rm --network host --name "${deployment_id}-app" \
                           -e "DATABASE_URL=postgres://postgres:hunter2@${PG_ADDRESS}:${PG_PORT}/postgres" \
                           -e "PORT=${app_port}" \
                           "$app_image" start)
    cat <<EOF >>"$STATE_FILE"
APP_CONTAINER=${app_container}
APP_URL=http://localhost:${app_port}
EOF

    printf 'Waiting for the app '
    until
        curl -Lfs --output /dev/null "http://localhost:${app_port}"
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

    if [[ -v APP_CONTAINER ]]; then
        podman stop -i "$APP_CONTAINER"
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
