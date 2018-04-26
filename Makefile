DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail
STATE_FILE := stack.state
ADDRESS_FILE := app.address
APP_INSTANCE_URL = http://$(shell cat $(ADDRESS_FILE)):8080


.PHONY: test autotest clean uberjar build-image push-image kubernetes-up kubernetes-down integration-test wait-for-http

test:
	lein midje

autotest:
	lein midje :autotest

clean:
	lein clean

target/server.jar:
	lein uberjar

uberjar: target/server.jar

build-image: uberjar
	docker build -t $(DOCKER_REPOSITORY):$(DOCKER_TAG) .

push-image: build-image
	docker push $(DOCKER_REPOSITORY):$(DOCKER_TAG)

$(STATE_FILE):
	./kubernetes/stack.sh --state-file $(STATE_FILE) --app-image $(DOCKER_REPOSITORY):$(DOCKER_TAG) --wait up

$(ADDRESS_FILE): $(STATE_FILE)
	ip=$$(./kubernetes/stack.sh --state-file $(STATE_FILE) service-ip) && printf '%s\n' "$$ip" >$(ADDRESS_FILE)

kubernetes-up: $(STATE_FILE) $(ADDRESS_FILE)

kubernetes-down:
	if [ -f $(STATE_FILE) ]; then ./kubernetes/stack.sh --state-file $(STATE_FILE) down && rm -f -- $(ADDRESS_FILE); fi

wait-for-http:
	./scripts/wait_for_http.sh "$(APP_INSTANCE_URL)"

integration-test: wait-for-http
	behave -D app_base_url="$(APP_INSTANCE_URL)" behave/features
