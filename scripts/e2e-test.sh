#!/usr/bin/env bash

set -euo pipefail

DEV_STACK_STATE_FILE="$1"
APP_STATE_FILE="$2"
RESET_SQL_FILE="$3"

# shellcheck disable=SC1090
source "$DEV_STACK_STATE_FILE"
# shellcheck disable=SC1090
source "$APP_STATE_FILE"


behave ./e2e/features \
       -D "app_base_url=${APP_URL}" \
       -D reset_script=./e2e/reset.sh \
       -D "reset_sql=${RESET_SQL_FILE}" \
       -D "pg_address=${PG_ADDRESS}" \
       -D "pg_port=${PG_PORT}" \
       -D "pg_password=hunter2"
       
