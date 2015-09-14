#!/bin/bash

ORIGINAL_DIR=$(pwd)

if [[ -z "$DEPLOYMENT_DIR" ]]; then
    cd $(dirname $0)/../
else
    cd $DEPLOYMENT_DIR
fi
DEPLOYMENT_DIR="."

if [[ -z "$NAME" ]]; then
   echo "Variable NAME must be present."
   exit 1
fi
if [[ -z "$BIN" ]]; then
   echo "Variable BIN must be present."
   exit 1
fi
if [[ -z "$BIN_STARTED" ]]; then
   echo "Variable BIN_STARTED must be present."
   exit 1
fi
if [[ -z "$LOG_DIR" ]]; then
    LOG_DIR="$DEPLOYMENT_DIR/service"
fi
if [[ -z "$PID_DIR" ]]; then
    PID_DIR="$DEPLOYMENT_DIR/service"
fi

DATE=$(date +"%Y-%m-%dT%H:%M")
VERSION=`cat $DEPLOYMENT_DIR/../pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`
BIN=$(echo $BIN | sed "s/:VERSION:/$VERSION/g")
LOG_FILE="$LOG_DIR/${NAME}.log"
PID_FILE="$PID_DIR/$NAME.pid"

if [ ! -d "$LOG_DIR" ]; then
    if ! mkdir -p $LOG_DIR; then
        exit 1
    fi
fi
if [ ! -d "$PID_DIR" ]; then
    if ! mkdir -p $PID_DIR; then
        exit 1
    fi
fi

COLOR_RED="\e[0;31m"
COLOR_GREEN="\e[0;32m"
COLOR_YELLOW="\e[0;33m"
COLOR_DEFAULT="\e[0m"

case "$1" in
    start)
        if [ -f $PID_FILE ]; then
            echo -e "[${COLOR_YELLOW}WARN${COLOR_DEFAULT}] The $NAME is already started."
            exit 0
        fi
        if ! touch $PID_FILE; then
            exit 1
        fi
        echo Starting $NAME $VERSION...
        nohup $BIN >> $LOG_FILE 2>&1 &
        # Get pid
        PID="$!"
        # Wait for process to start
        while ps -p $PID > /dev/null && ! grep -q "$BIN_STARTED" $LOG_FILE ; do sleep 1; done;
        # Check whether process is started
        if ps -p $PID > /dev/null;
        then
            echo "$PID" > $PID_FILE
            echo -e "[${COLOR_GREEN}OK${COLOR_DEFAULT}] The $NAME was started".
            exit 0
        else
            rm $PID_FILE
            cat $LOG_FILE
            echo -e "[${COLOR_RED}FAILED${COLOR_DEFAULT}] The $NAME failed to start."
            exit 1
        fi
        ;;
    stop)
        if [ ! -f $PID_FILE ]; then
            echo -e "[${COLOR_YELLOW}WARN${COLOR_DEFAULT}] The $NAME is not started."
            exit 0
        fi
        PID=$(cat $PID_FILE)
        # Stop process
        if ps -p $PID > /dev/null; then
            if ! touch $PID_FILE; then
                exit 1
            fi
            echo Stopping $NAME $VERSION...
            # Stop process
            if ! kill $PID; then
                exit 1
            fi
            # Wait for process to end
            while ps -p $PID > /dev/null; do sleep 1; done;
            echo -e "[${COLOR_GREEN}OK${COLOR_DEFAULT}] The $NAME was stopped."
        else
            echo -e "[${COLOR_YELLOW}WARN${COLOR_DEFAULT}] The $NAME is not started."
        fi
        # Delete pid file
        if ! rm $PID_FILE; then
            exit 1
        fi
        exit 0
        ;;
    force-stop)
        if [ ! -f $PID_FILE ]; then
            echo -e "[${COLOR_YELLOW}WARN${COLOR_DEFAULT}] The $NAME is not started."
            exit 0
        fi
        PID=$(cat $PID_FILE)
        # Stop process
        if ps -p $PID > /dev/null; then
            if ! touch $PID_FILE; then
                exit 1
            fi
            echo Stopping $NAME $VERSION...
            # Stop process
            if ! kill -9 $PID; then
                exit 1
            fi
            # Wait for process to end
            while ps -p $PID > /dev/null; do sleep 1; done;
            echo -e "[${COLOR_GREEN}OK${COLOR_DEFAULT}] The $NAME was killed."
        else
            echo -e "[${COLOR_YELLOW}WARN${COLOR_DEFAULT}] The $NAME is not started."
        fi
        # Delete pid file
        if ! rm $PID_FILE; then
            exit 1
        fi
        exit 0
        ;;
    restart)
        cd $ORIGINAL_DIR
        $0 stop
        $0 start
        ;;
    status)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            if [[ ! -z "$PID" ]] && ps -p $PID > /dev/null; then
                echo -e "[${COLOR_GREEN}STARTED${COLOR_DEFAULT}] The $NAME service is started."
            else
                echo -e "[${COLOR_YELLOW}STARTING${COLOR_DEFAULT}] The $NAME service is starting."

            fi
        else
            echo -e "[${COLOR_RED}NOT_STARTED${COLOR_DEFAULT}] The $NAME service is not started."
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop|force-stop|restart|status}"
        exit 1
        ;;
esac

exit 0
