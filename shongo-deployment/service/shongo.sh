#!/bin/bash

ORIGINAL_DIR=$(pwd)

cd $(dirname $0)

case "$1" in
    start)
        ./shongo-controller.sh start
        ./shongo-connector.sh start
        ;;
    stop)
        ./shongo-connector.sh stop
        ./shongo-controller.sh stop
        ;;
    restart)
        cd $ORIGINAL_DIR
        $0 stop
        $0 start
        ;;
    status)
        ./shongo-connector.sh status
        ./shongo-controller.sh status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac