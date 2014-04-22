#!/bin/bash

SERVICE_DIR="/etc/init.d"

cd $(dirname $0)/../

if [ -d "$SERVICE_DIR" ]; then
    rm $SERVICE_DIR/shongo-controller
    rm $SERVICE_DIR/shongo-connector
    rm $SERVICE_DIR/shongo-client-web
    rm $SERVICE_DIR/shongo
fi