TAG := latest
IMAGE_NAME := docker.io/alikov/trail

.PHONY: test autotest clean uberjar build-image push-image

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
	docker build -t $(IMAGE_NAME):$(TAG) .

push-image: build-image
	docker push $(IMAGE_NAME):$(TAG)
