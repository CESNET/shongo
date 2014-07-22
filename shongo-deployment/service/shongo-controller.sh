#!/bin/bash

NAME=shongo-controller
BIN="java -jar ../shongo-controller/target/shongo-controller-:VERSION:.jar --daemon -Dfile.encoding=UTF-8"
BIN_STARTED="Controller successfully started"

source $(dirname $0)/shongo-service.sh