DOCKER_TAG := latest
DOCKER_REPOSITORY := docker.io/alikov/trail
APP_INSTANCE_URL = http://$(shell cd kubernetes; bash stack.sh --namespace "$$(cat stack.namespace)" service-ip):8080

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

kubernetes-up: push-image
	cd kubernetes; \
	! [[ -f stack.namespace ]] && bash stack.sh --app-image $(DOCKER_REPOSITORY):$(DOCKER_TAG) --wait up >stack.namespace

kubernetes-down:
	cd kubernetes; \
	bash stack.sh --namespace $$(cat stack.namespace) down && rm -f stack.namespace

integration-test:
	bash scripts/wait_for_http.sh "$(APP_INSTANCE_URL)"; \
	cd behave; \
	behave -D app_base_url="$(APP_INSTANCE_URL)"
