#!/usr/bin/env bash

set -euo pipefail

NS="$1"

if ! [[ -r "./${NS}-reset.sql" && -r "./${NS}-endpoints" ]]; then
    echo 'Please deploy a stack to run tests against'
    exit 1
fi

source "./${NS}-endpoints"

pipenv run behave \
       -D reset_script="./reset.sh" \
       -D reset_sql="./${NS}-reset.sql" \
       -D pg_address="$PG_ADDRESS" \
       -D pg_port="$PG_PORT" \
       -D pg_password=hunter2 \
       -D app_base_url="$APP_BASE_URL"
