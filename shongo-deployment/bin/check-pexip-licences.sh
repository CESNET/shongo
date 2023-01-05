#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Expects a configuration file with defined USERNAME and PASSWORD variables
CFG_FILE=$SCRIPT_DIR/check-pexip-licences.cfg
if [[ ! -f "$CFG_FILE" ]]; then
    echo "$CFG_FILE does not exists."
    exit 1
fi
source $CFG_FILE

ENDPOINT="https://pexman.cesnet.cz:443/api/admin/status/v1/licensing/"

CURL=`curl --silent -u ${USERNAME}:${PASSWORD} -X GET ${ENDPOINT}`
RESULT=$CURL

echo $RESULT | grep -q port_count
if [ $? -eq 0 ]
then 
    LICENSE_COUNT=$(echo "$RESULT" | grep port_count | sed 's/^.*\"port_count\": \([0-9]*\).*$/\1/')

    if  [ $LICENSE_COUNT -lt 60 ]; then 
        echo "OK, $((60 - $LICENSE_COUNT)) licences available."
        exit 0
    else
        echo "CRITICAL, $LICENSE_COUNT licences consumed."
        exit 2
    fi
else
    echo "UKNOWN, unable to fetch available license count."
    exit 3
fi

