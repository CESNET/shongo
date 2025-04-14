#!/bin/bash

SERVICE_DIR="/etc/init.d"

cd $(dirname $0)/../

if [ -d "$SERVICE_DIR" ]; then
    if [ -f $SERVICE_DIR/shongo-controller ]; then
        echo Uninstalling shongo-controller...
        $SERVICE_DIR/shongo-controller stop
        rm $SERVICE_DIR/shongo-controller
        update-rc.d shongo-controller remove
    fi
    if [ -f $SERVICE_DIR/shongo-connector ]; then
        echo Uninstalling shongo-connector...
        $SERVICE_DIR/shongo-connector stop
        rm $SERVICE_DIR/shongo-connector
        update-rc.d shongo-connector remove
    fi
    if [ -f $SERVICE_DIR/shongo ]; then
        rm $SERVICE_DIR/shongo
    fi
fi