#!/usr/bin/env bash

set -eu
set -o pipefail

KUBECTL_CMD="${KUBECTL_CMD:-kubectl}"

COMMAND="${1:-}"

APP_IMAGE="${2:-docker.io/alikov/trail:latest}"
export APP_IMAGE

POSTGRES_IMAGE="${3:-docker.io/alikov/postgres96:trail}"
export POSTGRES_IMAGE


usage () {
    cat <<EOF
Usage ${0} COMMAND [APP-IMAGE [POSTGRES-IMAGE]]

Manage the application on the Kubernetes cluster


COMMANDS
        up              Deploy stack on Kubernetes. Will output the created
                        namespace name to STDOUT on success (see example).
        down            Destroy existing stack
        status          Show current deployment status
        help            Show this help text

OPTIONS
        APP-IMAGE       Specify the application Docker image tag to test.
                        Current value: ${APP_IMAGE}
        POSTGRES-IMAGE  Specify the PostgreSQL Docker image tack to use. The
                        image must enable the uuid-ossp extension for the
                        created database.
                        Current value: ${POSTGRES_IMAGE}

ENVIRONMENT VARIABLES
        KUBECTL_CMD     Set this variable to override the kubectl command.
                        Current value: ${KUBECTL_CMD}

EXAMPLE
        ns=\$(${0} up registry.example.com:5000/trail:custom-build-id registry.example.com:5000/postgres96:trail)
        ip=\$(kubectl --namespace "\$ns" get service trail -o json | jq -r '.spec.clusterIP')
        # wait ...
        curl "http://\$ip:8080/swagger.json"

EOF
}

incorrect_usage () {
    local msg="$1"

    {
        printf '%s\n\n' "$msg"
        usage
    } 1>&2

    exit 1
}

[[ -n "$COMMAND" ]] || incorrect_usage 'Command is missing'

NAMESPACE="trail-$(printf '%s' "$APP_IMAGE" | md5sum -b | cut -f 1 -d ' ')"

case "$COMMAND" in
    status)
        "$KUBECTL_CMD" --namespace "$NAMESPACE" get pod trail
        "$KUBECTL_CMD" --namespace "$NAMESPACE" get service trail
        ;;
    up)
        {
            "$KUBECTL_CMD" create namespace "$NAMESPACE"
            "$KUBECTL_CMD" --namespace "$NAMESPACE" create -f <(envsubst < deployment.yaml)
            "$KUBECTL_CMD" --namespace "$NAMESPACE" create -f <(envsubst < service.yaml)
        } 1>&2
        printf '%s\n' "$NAMESPACE"
        ;;
    down)
        {
            "$KUBECTL_CMD" --namespace "$NAMESPACE" delete -f <(envsubst < deployment.yaml)
            "$KUBECTL_CMD" --namespace "$NAMESPACE" delete -f <(envsubst < service.yaml)
            "$KUBECTL_CMD" delete namespace "$NAMESPACE"
        } 1>&2
        ;;
    help)
        usage
        ;;
    *)
        incorrect_usage 'Unknown command'
        ;;
esac
