#!/bin/bash

ORIGINAL_DIR=$(pwd)

cd $(dirname $0)

case "$1" in
    start)
        ./shongo-controller.sh start
        ./shongo-connector.sh start
        ./shongo-client-web.sh start
        ;;
    stop)
        ./shongo-controller.sh stop
        ./shongo-connector.sh stop
        ./shongo-client-web.sh stop
        ;;
    restart)
        cd $ORIGINAL_DIR
        $0 stop
        $0 start
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac