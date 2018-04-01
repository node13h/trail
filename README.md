# trail - IP history management API

FIXME

## Usage

### Run the application locally

`lein ring server`

### Run the tests

`lein midje`

### Packaging and running as standalone jar

```
lein do clean, ring uberjar
java -jar target/server.jar
```

### Packaging as war

`lein ring uberwar`

## License

Copyright (C) 2018 Sergej Alikov <sergej.alikov@gmail.com>

GNU Affero General Public License v3.0
