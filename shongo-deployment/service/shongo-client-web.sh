#!/bin/bash

NAME=shongo-client-web
BIN="java -jar ../shongo-client-web/target/shongo-client-web-:VERSION:.jar --daemon"
BIN_STARTED="ClientWeb successfully started"

source $(dirname $0)/shongo-service.sh