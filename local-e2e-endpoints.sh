#!/usr/bin/env bash

set -euo pipefail

ACTION="$1"

STATE_FILE="$2"
RESET_SQL_FILE="$3"

APP_PORT="$4"
PG_PORT="$5"

POD="$6"

stop () {
    if podman pod exists "$POD"; then
        podman pod rm -f "$POD"
    fi

    rm -f "$STATE_FILE"
    rm -f "$RESET_SQL_FILE"
}


start () {
    stop

    if ! podman pod exists "$POD"; then
        podman pod create --name "$POD" -p "${APP_PORT}:3000" -p "${PG_PORT}:5432"
        podman pod start "$POD"
    fi

    declare pg_image

    pg_image=$(podman build -q development/postgres)

    podman run --pod "$POD" -d --name "${POD}-pg" \
           -e POSTGRES_PASSWORD=hunter2 \
           "${pg_image:0:12}"

    until nc -z localhost "$PG_PORT"; do
        echo Wait for DB
        sleep 1
    done

    declare app_image

    app_image=$(podman build -q .)

    podman run --pod "$POD" --rm \
           -e DATABASE_URL=postgres://postgres:hunter2@localhost/postgres \
           "$app_image" migrate

    podman run --pod "$POD" --rm --entrypoint pg_dump \
           -e PGPASSWORD=hunter2 \
           "$pg_image" -h 127.0.0.1 -U postgres -c postgres > "$RESET_SQL_FILE"

    podman run --pod "$POD" -d --name "$POD-app" \
           -e DATABASE_URL=postgres://postgres:hunter2@localhost/postgres \
           "$app_image" start

    until curl -Lfs --output /dev/null http://localhost:"$APP_PORT"; do
        echo Wait for app
        sleep 1
    done

    cat <<EOF > "$STATE_FILE"
APP_BASE_URL='http://localhost:${APP_PORT}'
PG_ADDRESS=localhost
PG_PORT=${PG_PORT}
EOF
}

case "$ACTION" in
    start)
        start
        ;;
    stop)
        stop
        ;;
esac
