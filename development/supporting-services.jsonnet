function(image, pod="trail-supporting-services", pg_port=5432, pg_password="hunter2") {
    "apiVersion": "v1",
    "kind": "Pod",
    "metadata": {
      "creationTimestamp": "2020-05-23T13:06:06Z",
      "labels": {
        "app": pod
      },
      "name": pod
    },
    "spec": {
      "containers": [
        {
          "env": [
            {
              "name": "POSTGRES_PASSWORD",
              "value": pg_password
            }
          ],
          "image": image,
          "name": "postgresql",
          "ports": [
            {
              "containerPort": 5432,
              "hostPort": pg_port,
              "protocol": "TCP"
            }
          ],
          "resources": {},
          "securityContext": {
            "allowPrivilegeEscalation": true,
            "capabilities": {},
            "privileged": false,
            "readOnlyRootFilesystem": false,
            "seLinuxOptions": {}
          }
        }
      ]
    },
    "status": {}

}
