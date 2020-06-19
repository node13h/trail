#!/usr/bin/env bash

set -euo pipefail

PG_IMAGE='postgres:9.6-alpine'

ACTION="$1"
shift


start () {
    declare ns="$1"
    declare state_file="$2"
    declare dev_stack_state_file="$3"
    declare reset_sql_file="$4"
    declare app_port="$5"

    # shellcheck disable=SC1090
    source "$dev_stack_state_file"

    declare app_image
    app_image=$(podman build -q .)

    podman run --rm --network host \
           -e "DATABASE_URL=postgres://postgres:hunter2@${PG_ADDRESS}:${PG_PORT}/postgres" \
           "$app_image" migrate

    podman run --rm --network host --entrypoint pg_dump \
           -e PGPASSWORD=hunter2 \
           "$PG_IMAGE" -h "$PG_ADDRESS" -p "$PG_PORT" -U postgres -c postgres > "$reset_sql_file"

    podman run -d --rm --network host --name "${ns}-app" \
           -e "DATABASE_URL=postgres://postgres:hunter2@${PG_ADDRESS}:${PG_PORT}/postgres" \
           -e "PORT=${app_port}" \
           "$app_image" start

    until
        curl -Lfs --output /dev/null "http://localhost:${app_port}"
    do
        echo Wait for app
        sleep 1
    done

    cat <<EOF > "$state_file"
APP_URL=http://localhost:${app_port}
EOF
}


stop () {
    declare ns="$1"
    declare state_file="$2"

    podman stop -i "${ns}-app"

    while
        podman container exists "${ns}-app"
    do
        sleep 1
    done

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
