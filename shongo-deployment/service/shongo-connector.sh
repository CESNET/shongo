#!/bin/bash

NAME=shongo-connector
BIN="java -Dfile.encoding=UTF-8 -jar ../shongo-connector/target/shongo-connector-:VERSION:.jar --daemon"
BIN_STARTED="Connector successfully started"

source $(dirname $0)/shongo-service.sh