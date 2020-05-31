DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail

E2E_APP_PORT = 3001
E2E_PG_PORT = 5433
E2E_NS = trail-e2e-supporting-services

export RELEASE_REMOTE := origin

.PHONY: test autotest clean uberjar build-image push-image wait-for-http compose-up compose-down compose-ps release-start release-finish e2e-test e2e-endpoints-up e2e-endpoints-down

e2e-endpoints-up: target/server.jar
	./local-e2e-endpoints.sh start behave/endpoints behave/reset.sql $(E2E_APP_PORT) $(E2E_PG_PORT) $(E2E_NS)

e2e-endpoints-down:
	./local-e2e-endpoints.sh stop behave/endpoints behave/reset.sql $(E2E_APP_PORT) $(E2E_PG_PORT) $(E2E_NS)

e2e-test:
	cd behave && ./run-tests.sh

test:
	lein midje

autotest:
	lein midje :autotest

clean:
	lein clean

mrproper: clean
	-pipenv --rm

develop:
	pipenv sync --dev

update-deps:
	pipenv update

target/server.jar:
	lein uberjar

uberjar: target/server.jar

build-image: uberjar
	podman build -t $(DOCKER_REPOSITORY):$(DOCKER_TAG) .

push-image: build-image
	podman push $(DOCKER_REPOSITORY):$(DOCKER_TAG)

wait-for-http:
	./scripts/wait_for_http.sh "$(APP_INSTANCE_URL)"

compose-up: target/server.jar
	podman-compose up --build -d

compose-down:
	podman-compose down
	podman volume rm trail_postgres-trail-data

compose-ps:
	podman-compose ps

release-start: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} start $${RELEASE_VERSION:+--version "$${RELEASE_VERSION}"}

release-finish: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} finish
