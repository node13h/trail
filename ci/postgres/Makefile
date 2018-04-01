TAG := trail
IMAGE_NAME := docker.io/alikov/postgres96

.PHONY: build push

build:
	docker build -t $(IMAGE_NAME):$(TAG) .

push: build
	docker push $(IMAGE_NAME):$(TAG)
