DOCKER_TAG := $(shell cat VERSION)
DOCKER_REPOSITORY := docker.io/alikov/trail

DEPLOYMENT_ID = trail-dev

MODE := local

DEV_STACK_STATE_FILE = $(DEPLOYMENT_ID)-dev-stack.state
DEV_STACK_PG_PORT := 5432

APP_PORT := 3000
APP_STATE_FILE = $(DEPLOYMENT_ID)-app.state
RESET_SQL_FILE = ./e2e/$(DEPLOYMENT_ID)-reset.sql

export RELEASE_REMOTE := origin

.PHONY: all

all:
	true


.PHONY: dev-stack dev-stack-down app app-down

$(DEV_STACK_STATE_FILE):
	./scripts/$(MODE)-dev-stack.sh start $(DEV_STACK_STATE_FILE) $(DEPLOYMENT_ID) \
	  $(DEV_STACK_PG_PORT)

dev-stack: $(DEV_STACK_STATE_FILE)
dev-stack-down:
	./scripts/$(MODE)-dev-stack.sh stop $(DEV_STACK_STATE_FILE)

$(APP_STATE_FILE) $(RESET_SQL_FILE): target/server.jar
	./scripts/$(MODE)-app.sh start $(APP_STATE_FILE) $(DEPLOYMENT_ID) $(DEV_STACK_STATE_FILE) \
	  $(RESET_SQL_FILE) $(APP_PORT)

app: $(APP_STATE_FILE)
app-down:
	./scripts/local-app.sh stop $(APP_STATE_FILE)


.PHONY: e2e-test

e2e-test: $(DEV_STACK_STATE_FILE) $(APP_STATE_FILE)
	pipenv run ./scripts/e2e-test.sh $(DEV_STACK_STATE_FILE) $(APP_STATE_FILE) $(RESET_SQL_FILE)


.PHONY: test autotest clean uberjar build-image push-image release-start release-finish

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

release-start: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} start $${RELEASE_VERSION:+--version "$${RELEASE_VERSION}"}

release-finish: test
	pipenv run lase --version-file resources/VERSION $${RELEASE_REMOTE:+--remote "$${RELEASE_REMOTE}"} finish
