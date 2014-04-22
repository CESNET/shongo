#!/bin/bash

NAME=shongo-connector
BIN="java -jar ../shongo-connector/target/shongo-connector-:VERSION:.jar --daemon"
BIN_STARTED="Connector successfully started"

source $(dirname $0)/shongo-service.sh