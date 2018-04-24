#!/usr/bin/env bash

set -au
set -o pipefail

kubeconfig () {
    cat <<EOF
apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: "${KUBERNETES_API_URL}
  name: kubecluster
contexts:
- context:
    cluster: kubecluster
    user: user
  name: kubecluster
current-context: kubecluster
kind: Config
preferences: {}
users:
- name: user
  user:
    as-user-extra: {}
    password: "${KUBERNETES_API_PASSWORD}"
    username: "${KUBERNETES_API_USER}"
EOF
}

kubectl --kubeconfig=<(kubeconfig) "$@"
