DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail
STATE_FILE := stack.namespace
APP_INSTANCE_URL = http://$(shell cd kubernetes; bash stack.sh --namespace "$$(cat $(STATE_FILE))" service-ip):8080

.PHONY: test autotest clean uberjar build-image push-image kubernetes-up kubernetes-down integration-test

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

kubernetes-up:
	cd kubernetes; \
	! [[ -f $(STATE_FILE) ]] && bash stack.sh --app-image $(DOCKER_REPOSITORY):$(DOCKER_TAG) --wait up >$(STATE_FILE)

kubernetes-down:
	cd kubernetes; \
	bash stack.sh --namespace $$(cat $(STATE_FILE)) down && rm -f $(STATE_FILE)

integration-test:
	bash scripts/wait_for_http.sh "$(APP_INSTANCE_URL)"; \
	cd behave; \
	behave -D app_base_url="$(APP_INSTANCE_URL)"
