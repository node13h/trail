#!/usr/bin/env bash

set -euo pipefail

URL="$1"
RETRIES="${2:-120}"

declare i=1

msg () {
    >&2 printf '%s\n' "$1"
}

retries_exceeded () {
    msg 'Exceeded number of retries. Giving up.'

    return 1
}


msg "Waiting for functional HTTP(S) at ${URL}"

until curl -k -s --connect-timeout 1 "$URL"; do
    [[ "$i" -lt "$RETRIES" ]] || retries_exceeded

    sleep 1

    i=$((i+1))
done
