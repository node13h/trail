#!/usr/bin/env bash

set -euo pipefail

if ! [[ -x ./reset.sh && -r ./endpoints ]]; then
    echo 'Please deploy a stack to run tests against'
    exit 1
fi

source ./endpoints

pipenv run behave \
       -D reset_script=./reset.sh \
       -D pg_address="$PG_ADDRESS" \
       -D pg_port="$PG_PORT" \
       -D pg_password=hunter2 \
       -D app_base_url="$APP_BASE_URL"
