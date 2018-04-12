#!/usr/bin/env bash

set -eu
set -o pipefail

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

KUBECTL_CMD="${KUBECTL_CMD:-kubectl}"

declare -x APP_IMAGE="docker.io/alikov/trail:latest"
# Postgres with uuid-ossp enabled
declare -x POSTGRES_IMAGE="docker.io/alikov/postgres96:trail"

TIMEOUT=120

is_true () {
    [[ "$1" = "TRUE" ]]
}

incorrect_usage () {
    local msg="$1"

    {
        printf '%s\n\n' "$msg"
        usage
    } 1>&2

    exit 1
}

pod_json () {
    local namespace="$1"
    local pod="$2"

    "$KUBECTL_CMD" --namespace "$namespace" get pod "$pod" -o json
}

service_json () {
    local namespace="$1"
    local service="$2"

    "$KUBECTL_CMD" --namespace "$namespace" get service "$service" -o json
}

service_ip () {
    local namespace="$1"
    local service="$2"

    service_json "$namespace" "$service" | jq -r '.spec.clusterIP'
}

pod_is_ready () {
    local namespace="$1"
    local pod="$2"

    pod_json "$namespace" "$pod" | jq -e 'reduce .status.containerStatuses[].ready as $item (true; . and $item)' >/dev/null
}

wait_for_pod () {
    local namespace="$1"
    local pod="$2"

    local i=1

    while ! pod_is_ready "$namespace" "$pod" && [[ "$i" -lt "$TIMEOUT" ]]; do
        sleep 1
        i=$((i+1))
    done
}

bring_up () {
    local namespace

    namespace=$(uuidgen)

    {
        "$KUBECTL_CMD" create namespace "$namespace"
        "$KUBECTL_CMD" --namespace "$namespace" create -f <(envsubst < "${SCRIPT_DIR}/deployment.yaml")
        "$KUBECTL_CMD" --namespace "$namespace" create -f <(envsubst < "${SCRIPT_DIR}/service.yaml")
    } 1>&2

    printf '%s\n' "$namespace"
}

tear_down () {
    local namespace="$1"

    {
        "$KUBECTL_CMD" --namespace "$namespace" delete -f <(envsubst < "${SCRIPT_DIR}/deployment.yaml")
        "$KUBECTL_CMD" --namespace "$namespace" delete -f <(envsubst < "${SCRIPT_DIR}/service.yaml")
        "$KUBECTL_CMD" delete namespace "$namespace"
    } 1>&2
}

usage () {
    cat <<EOF
Usage ${0} OPTIONS COMMAND

Manage the application on the Kubernetes cluster

OPTIONS
        --app-image

COMMANDS
        up              Deploy stack on Kubernetes. Will output the created
                        an UUID-like namespace name to STDOUT on
                        success (see example).
        down            Destroy existing stack. Requires --namespace
        pod             Output JSON pod object. Requires --namespace
        service         Output JSON service object. Requires --namespace
        service-ip      Return the cluster IP address of the service object.
                        Requires --namespace
        help            Show this help text

OPTIONS
        --namespace     Namespace name (returned by the 'up' command)
        --app-image     Specify the application Docker image tag to test.
                        Current value: ${APP_IMAGE}
        --postgres-image
                        Specify the PostgreSQL Docker image tack to use. The
                        image must enable the uuid-ossp extension for the
                        created database.
                        Current value: ${POSTGRES_IMAGE}

ENVIRONMENT VARIABLES
        KUBECTL_CMD     Set this variable to override the kubectl command.
                        Current value: ${KUBECTL_CMD}

EXAMPLE
        ns=\$(${0} --wait up)
        ip=\$(${0} --namespace "\$ns" service-ip)
        curl "http://\$ip:8080/swagger.json"

EOF
}

main () {
    local namespace=
    local command=
    local wait=FALSE

    while [[ "$#" -gt 0 ]]; do
        case "$1" in
            --namespace)
                namespace="$2"
                shift
                ;;
            --app-image)
                APP_IMAGE="$2"
                shift
                ;;
            --postgres-image)
                POSTGRES_IMAGE="$2"
                shift
                ;;
            --wait)
                wait=TRUE
                ;;
            *)
                command="$1"
                ;;
        esac
        shift
    done

    [[ -n "$command" ]] || incorrect_usage 'Command is missing'

    if [[ -z "$namespace" ]] && ! [[ "$command" =~ up|help ]]; then
        incorrect_usage "Please set the namespace"
    fi

    case "$command" in
        service)
            service_json "$namespace" app
            ;;
        service-ip)
            service_ip "$namespace" app
            ;;
        pod)
            pod_json "$namespace" stack
            ;;
        up)
            namespace=$(bring_up)

            if is_true "$wait"; then
                wait_for_pod "$namespace" stack
            fi

            printf '%s\n' "$namespace"
            ;;
        down)
            tear_down "$namespace"
            ;;
        help)
            usage
            ;;
        *)
            incorrect_usage "Unknown command"
            ;;
    esac
}


if [[ -n "${BASH_SOURCE[0]:-}" && "${0}" = "${BASH_SOURCE[0]}" ]]; then
    main "$@"
fi
