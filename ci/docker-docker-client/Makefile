TAG := latest
IMAGE_NAME := docker.io/alikov/docker-client

.PHONY: build push

build:
	docker build -t $(IMAGE_NAME):$(TAG) .

push: build
	docker push $(IMAGE_NAME):$(TAG)
