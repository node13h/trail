#!/usr/bin/env bash

set -eu
set -o pipefail

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

KUBECTL_CMD="${KUBECTL_CMD:-kubectl}"

declare -x APP_IMAGE="docker.io/alikov/trail:latest"
# Postgres with uuid-ossp enabled
declare -x POSTGRES_IMAGE="docker.io/alikov/postgres96:trail"

TIMEOUT=120
STATE_FILE=stack.state

is_true () {
    [[ "$1" = "TRUE" ]]
}

msg () {
    local msg="$1"
    >&2 printf '%s\n' "$msg"
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

    while ! pod_is_ready "$namespace" "$pod"; do
        if [[ "$i" -gt "$TIMEOUT" ]]; then
            msg "Timeout"
            return 1
        fi
        sleep 1
        i=$((i+1))
    done
}

new_namespace () {
    local namespace
    namespace=$(uuidgen)

    "$KUBECTL_CMD" create namespace "$namespace" 1>&2 && printf '%s\n' "$namespace"
}

bring_up () {
    local namespace="$1"

    "$KUBECTL_CMD" --namespace "$namespace" create -f <(envsubst < "${SCRIPT_DIR}/deployment.yaml")
    "$KUBECTL_CMD" --namespace "$namespace" create -f <(envsubst < "${SCRIPT_DIR}/service.yaml")
}

delete_namespace () {
    local namespace="$1"

    "$KUBECTL_CMD" delete namespace "$namespace" 1>&2
}

assert_there_is_no_state () {
    if [[ -e "$STATE_FILE" ]]; then
        msg "State file ${STATE_FILE} already exists"
        return 1
    fi
}

namespace () {
    cat "$STATE_FILE"
}

set_namespace () {
    local namespace="$1"

    cat >"$STATE_FILE" <<< "$namespace"
}

remove_state () {
    rm -f -- "$STATE_FILE"
}

usage () {
    cat <<EOF
Usage ${0} OPTIONS COMMAND

Manage the application on the Kubernetes cluster

OPTIONS
        --app-image IMAGE        Specify an application Docker image to test.
                                 Current value: ${APP_IMAGE}
        --postgres-image IMAGE
                                 Specify a PostgreSQL Docker image to use. The
                                 image must enable the uuid-ossp extension for the
                                 created database.
                                 Current value: ${POSTGRES_IMAGE}
        --state-file PATH        Set location of the state file.
                                 Current value: ${STATE_FILE}

COMMANDS
        up              Deploy a stack on Kubernetes. Will output the created
                        UUID-like namespace name to STDOUT on
                        success (see example).
        down            Destroy the existing stack. Requires --namespace
        pod             Output the JSON pod object. Requires --namespace
        service         Output the JSON service object. Requires --namespace
        service-ip      Return the cluster IP address of the service object.
                        Requires --namespace
        help            Show this help text

ENVIRONMENT VARIABLES
        KUBECTL_CMD     Set this variable to override the kubectl command.
                        Current value: ${KUBECTL_CMD}

EXAMPLE
        ${0} --wait up
        ip=\$(${0} service-ip)
        # At this point the containers are ready, but the application
        # itself might be starting up, therefore you might need to
        # check if service is responding before doing any testing.
        curl "http://\$ip:8080/swagger.json"

EOF
}

main () {
    local namespace=
    local command=
    local wait=FALSE

    while [[ "$#" -gt 0 ]]; do
        case "$1" in
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
            --state-file)
                STATE_FILE="$2"
                shift
                ;;
            *)
                command="$1"
                ;;
        esac
        shift
    done

    [[ -n "$command" ]] || incorrect_usage 'Command is missing'

    case "$command" in
        service)
            service_json "$(namespace)" app
            ;;
        service-ip)
            service_ip "$(namespace)" app
            ;;
        pod)
            pod_json "$(namespace)" stack
            ;;
        up)
            assert_there_is_no_state

            namespace=$(new_namespace)
            set_namespace "$namespace"

            bring_up "$namespace"

            if is_true "$wait"; then
                wait_for_pod "$namespace" stack
            fi

            ;;
        down)
            delete_namespace "$(namespace)"
            remove_state
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
