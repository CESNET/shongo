#!/bin/bash

BIN=$(dirname $0)

CONTROLLER=127.0.0.1

function check_controller
{
    RESULT=$($BIN/client-cli.sh src --connect $CONTROLLER --root --scripting --cmd "status" 2> /dev/null)
    if echo $RESULT | grep -s '"status"\s*:\s*"AVAILABLE"' > /dev/null; then
        echo "OK (controller available at '$CONTROLLER' for domain '`echo $RESULT | sed -s 's/^.\+"name" : "\([^"]\+\)".\+$/\1/'`')"
        exit 0
    else
        echo "FAILED (controller not available at $CONTROLLER)"
        exit 2
    fi
}

function check_connector
{
    echo TODO: check connector
    exit 3
}

function check_client_web
{
    echo TODO: check client web
    exit 3
}

case $1 in
    connector)
        check_connector
        ;;
    client_web)
        check_client_web
        ;;
    *)
        check_controller
        ;;
esac