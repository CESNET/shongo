#!/bin/bash

NAME=shongo-client-web
BIN="java -Dfile.encoding=UTF-8 -jar ../shongo-client-web/target/shongo-client-web-:VERSION:.jar --daemon"
BIN_STARTED="ClientWeb successfully started"

source $(dirname $0)/shongo-service.sh