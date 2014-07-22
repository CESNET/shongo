#!/bin/bash

NAME=shongo-connector
BIN="java -jar ../shongo-connector/target/shongo-connector-:VERSION:.jar --daemon -Dfile.encoding=UTF-8"
BIN_STARTED="Connector successfully started"

source $(dirname $0)/shongo-service.sh