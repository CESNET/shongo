#!/bin/bash

NAME=shongo-controller
BIN="java -jar ../shongo-controller/target/shongo-controller-:VERSION:.jar --daemon"
BIN_STARTED="Controller successfully started"

source $(dirname $0)/shongo-service.sh