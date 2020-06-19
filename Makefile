DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail

NS = trail-dev

MODE := local

DEV_STACK_STATE_FILE = $(NS)-dev-stack.state
DEV_STACK_PG_PORT := 5432

APP_PORT := 3000
APP_STATE_FILE = $(NS)-app.state
RESET_SQL_FILE = ./e2e/$(NS)-reset.sql

export RELEASE_REMOTE := origin

.PHONY: all

all:
	true


.PHONY: local-dev-stack local-dev-stack-down dev-stack dev-stack-down local-app local-app-down app app-down

$(DEV_STACK_STATE_FILE):
	$(MAKE) $(MODE)-dev-stack

dev-stack: $(DEV_STACK_STATE_FILE)
dev-stack-down:
	$(MAKE) $(MODE)-dev-stack-down

local-dev-stack:
	./scripts/local-dev-stack.sh start $(NS) $(DEV_STACK_STATE_FILE) \
	  $(DEV_STACK_PG_PORT)

local-dev-stack-down:
	./scripts/local-dev-stack.sh stop $(NS) $(DEV_STACK_STATE_FILE)

$(APP_STATE_FILE) $(RESET_SQL_FILE):
	$(MAKE) $(MODE)-app

app: $(APP_STATE_FILE)
app-down:
	$(MAKE) $(MODE)-app-down

local-app: $(DEV_STACK_STATE_FILE) target/server.jar
	./scripts/local-app.sh start $(NS) $(APP_STATE_FILE) $(DEV_STACK_STATE_FILE) \
	  $(RESET_SQL_FILE) $(APP_PORT)

local-app-down:
	./scripts/local-app.sh stop $(NS) $(APP_STATE_FILE) $(RESET_SQL_FILE)


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
