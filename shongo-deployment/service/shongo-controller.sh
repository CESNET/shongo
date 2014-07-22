#!/bin/bash

NAME=shongo-controller
BIN="java -Dfile.encoding=UTF-8 -jar ../shongo-controller/target/shongo-controller-:VERSION:.jar --daemon"
BIN_STARTED="Controller successfully started"

source $(dirname $0)/shongo-service.sh