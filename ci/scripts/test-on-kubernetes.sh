#!/usr/bin/env bash

set -eu
set -o pipefail

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

KUBERNETES_API_URL="${KUBERNETES_API_URL:-https://localhost:6443}"
KUBERNETES_API_USER="${KUBERNETES_API_USER:-admin}"
KUBERNETES_API_PASSWORD="${KUBERNETES_API_PASSWORD:-}"

APP_IMAGE="$1"


kubeconfig () {
    cat <<EOF
apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: ${KUBERNETES_API_URL}
  name: kubecluster
contexts:
- context:
    cluster: kubecluster
    user: admin
  name: kubecluster
current-context: kubecluster
kind: Config
preferences: {}
users:
- name: admin
  user:
    as-user-extra: {}
    password: "${KUBERNETES_API_PASSWORD}"
    username: "${KUBERNETES_API_USER}"
EOF
}

kubectl_cmd () {
    kubectl --kubeconfig=<(kubeconfig) "$@"
}

export -f kubectl_cmd
export -f kubeconfig

export KUBECTL_CMD=kubectl_cmd

# TODO
# - up
# - test
# - down

"${SCRIPT_DIR}/../kubernetes/stack.sh" status "${APP_IMAGE}"
