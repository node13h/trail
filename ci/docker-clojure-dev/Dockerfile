FROM openjdk:8-jdk-alpine
RUN apk add --no-cache git bash make; \
    wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -O /usr/bin/lein; \
    chmod 755 /usr/bin/lein
