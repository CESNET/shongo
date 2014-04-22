#!/bin/bash

SERVICE_DIR="/etc/init.d"
LOG_DIR="/var/log/shongo"
PID_DIR="/var/run/shongo"

cd $(dirname $0)/../
DEPLOYMENT_DIR=$(pwd)

if [ ! -d "$SERVICE_DIR" ]; then
    mkdir -p $SERVICE_DIR
fi

################################################################################
#
# Install shongo-controller service
#
cat > $SERVICE_DIR/shongo-controller <<EOF
#!/bin/bash

NAME=shongo-controller
BIN="java -jar $DEPLOYMENT_DIR/../shongo-controller/target/shongo-controller-:VERSION:.jar --daemon"
BIN_STARTED="Controller successfully started"
DEPLOYMENT_DIR="$DEPLOYMENT_DIR"
LOG_DIR="$LOG_DIR"
PID_DIR="$PID_DIR"

source $DEPLOYMENT_DIR/service/shongo-service.sh
EOF
chmod a+x $SERVICE_DIR/shongo-controller

################################################################################
#
# Install shongo-connector service
#
cat > $SERVICE_DIR/shongo-connector <<EOF
#!/bin/bash

NAME=shongo-connector
BIN="java -jar $DEPLOYMENT_DIR/../shongo-connector/target/shongo-connector-:VERSION:.jar --daemon"
BIN_STARTED="Connector successfully started"
DEPLOYMENT_DIR="$DEPLOYMENT_DIR"
LOG_DIR="$LOG_DIR"
PID_DIR="$PID_DIR"

source $DEPLOYMENT_DIR/service/shongo-service.sh
EOF
chmod a+x $SERVICE_DIR/shongo-connector

################################################################################
#
# Install shongo-client-web service
#
cat > $SERVICE_DIR/shongo-client-web <<EOF
#!/bin/bash

NAME=shongo-client-web
BIN="java -jar $DEPLOYMENT_DIR/../shongo-client-web/target/shongo-client-web-:VERSION:.jar --daemon"
BIN_STARTED="ClientWeb successfully started"
DEPLOYMENT_DIR="$DEPLOYMENT_DIR"
LOG_DIR="$LOG_DIR"
PID_DIR="$PID_DIR"

source $DEPLOYMENT_DIR/service/shongo-service.sh
EOF
chmod a+x $SERVICE_DIR/shongo-client-web

################################################################################
#
# Install shongo all services
#
cat > $SERVICE_DIR/shongo <<EOF
#!/bin/bash

case "\$1" in
    start)
        $SERVICE_DIR/shongo-controller start
        $SERVICE_DIR/shongo-connector start
        $SERVICE_DIR/shongo-client-web start
        ;;
    stop)
        $SERVICE_DIR/shongo-controller stop
        $SERVICE_DIR/shongo-connector stop
        $SERVICE_DIR/shongo-client-web stop
        ;;
    restart)
        \$0 stop
        \$0 start
        ;;
    *)
        echo "Usage: \$0 {start|stop|restart}"
        exit 1
        ;;
esac
EOF
chmod a+x $SERVICE_DIR/shongo