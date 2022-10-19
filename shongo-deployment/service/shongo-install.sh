#!/bin/bash

SERVICE_DIR="/etc/init.d"
LOG_DIR="/var/log/shongo"
PID_DIR="/var/run/shongo"

cd $(dirname $0)/../
DEPLOYMENT_DIR=$(pwd)

if [ ! -d "$SERVICE_DIR" ]; then
    mkdir -p $SERVICE_DIR
fi

# Parse arguments
SHONGO_CONTROLLER=false
SHONGO_CONNECTOR=false
for argument in "$@"
do
    case "$argument" in
        shongo-controller)
            SHONGO_CONTROLLER=true
            ;;
        shongo-connector)
            SHONGO_CONNECTOR=true
            ;;
    esac
done

# No arguments
if [[ $# -eq 0 ]] ; then
    echo Installing all shongo services...
    SHONGO_CONTROLLER=true
    SHONGO_CONNECTOR=true
fi

################################################################################
#
# Install shongo-controller service
#
if [ "$SHONGO_CONTROLLER" = true ] ; then

echo Installing shongo-controller...
cat > $SERVICE_DIR/shongo-controller <<EOF
#!/bin/bash

### BEGIN INIT INFO
# Provides:          shongo-controller
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs
# Should-Start:      postgresql
# Should-Stop:       postgresql
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: shongo-controller
# Description:       shongo main backend application
### END INIT INFO

NAME=shongo-controller
BIN="java -Dfile.encoding=UTF-8 -jar $DEPLOYMENT_DIR/../shongo-controller/target/shongo-controller-:VERSION:.jar --daemon"
BIN_STARTED="Controller successfully started"
DEPLOYMENT_DIR="$DEPLOYMENT_DIR"
LOG_DIR="$LOG_DIR"
PID_DIR="$PID_DIR"

source $DEPLOYMENT_DIR/service/shongo-service.sh
EOF
chmod a+x $SERVICE_DIR/shongo-controller
update-rc.d shongo-controller defaults 90 11

fi

################################################################################
#
# Install shongo-connector service
#
if [ "$SHONGO_CONNECTOR" = true ] ; then

echo Installing shongo-connector...
cat > $SERVICE_DIR/shongo-connector <<EOF
#!/bin/bash

### BEGIN INIT INFO
# Provides:          shongo-connector
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs
# Should-Start:      shongo-controller
# Should-Stop:       shongo-controller
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: shongo-connector
# Description:       shongo device driver application
### END INIT INFO

NAME=shongo-connector
BIN="java -Dfile.encoding=UTF-8 -jar $DEPLOYMENT_DIR/../shongo-connector/target/shongo-connector-:VERSION:.jar --daemon"
BIN_STARTED="Connector successfully started"
DEPLOYMENT_DIR="$DEPLOYMENT_DIR"
LOG_DIR="$LOG_DIR"
PID_DIR="$PID_DIR"

source $DEPLOYMENT_DIR/service/shongo-service.sh
EOF
chmod a+x $SERVICE_DIR/shongo-connector
update-rc.d shongo-connector defaults 91 10

fi

################################################################################
#
# Install shongo all services
#
cat > $SERVICE_DIR/shongo <<EOF
#!/bin/bash

### BEGIN INIT INFO
# Provides:          shongo
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: shongo
# Description:       all installed shongo applications
### END INIT INFO

case "\$1" in
    start)
        if [ "$SHONGO_CONTROLLER" = true ] ; then
            $SERVICE_DIR/shongo-controller start
        fi
        if [ "$SHONGO_CONNECTOR" = true ] ; then
            $SERVICE_DIR/shongo-connector start
        fi
        ;;
    stop)
        if [ "$SHONGO_CONTROLLER" = true ] ; then
            $SERVICE_DIR/shongo-controller stop
        fi
        if [ "$SHONGO_CONNECTOR" = true ] ; then
            $SERVICE_DIR/shongo-connector stop
        fi
        ;;
    force-stop)
        if [ "$SHONGO_CONTROLLER" = true ] ; then
            $SERVICE_DIR/shongo-controller force-stop
        fi
        if [ "$SHONGO_CONNECTOR" = true ] ; then
            $SERVICE_DIR/shongo-connector force-stop
        fi
        ;;
    restart)
        \$0 stop
        \$0 start
        ;;
    status)
        if [ "$SHONGO_CONTROLLER" = true ] ; then
            $SERVICE_DIR/shongo-controller status
        fi
        if [ "$SHONGO_CONNECTOR" = true ] ; then
            $SERVICE_DIR/shongo-connector status
        fi
        ;;
    *)
        echo "Usage: \$0 {start|stop|force-stop|restart|status}"
        exit 1
        ;;
esac
EOF
chmod a+x $SERVICE_DIR/shongo