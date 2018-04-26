TAG := latest
IMAGE_NAME := docker.io/alikov/trail-behave

.PHONY: build push

build:
	docker build -t $(IMAGE_NAME):$(TAG) .

push: build
	docker push $(IMAGE_NAME):$(TAG)
