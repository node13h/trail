DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail
STATE_FILE := stack.state
ADDRESS_FILE := app.address
APP_INSTANCE_URL = http://localhost:3000

export RELEASE_REMOTE := origin

.PHONY: test autotest clean uberjar build-image push-image wait-for-http compose-up compose-down compose-ps release-start release-finish

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
	docker build -t $(DOCKER_REPOSITORY):$(DOCKER_TAG) .

push-image: build-image
	docker push $(DOCKER_REPOSITORY):$(DOCKER_TAG)

wait-for-http:
	./scripts/wait_for_http.sh "$(APP_INSTANCE_URL)"

e2e-test: wait-for-http
	cd behave && pipenv sync && pipenv run behave -D app_base_url=$(APP_INSTANCE_URL)

compose-up: uberjar
	docker-compose up --build -d

compose-down:
	docker-compose down -v --rmi local

compose-ps:
	docker-compose ps

release-start: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} start $${RELEASE_VERSION:+--version "$${RELEASE_VERSION}"}

release-finish: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} finish
