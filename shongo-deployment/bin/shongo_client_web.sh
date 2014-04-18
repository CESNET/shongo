#!/bin/bash
#
# Run Shongo web client application.
#

cd `dirname $0`/../
DIR=../shongo-client-web
VERSION=`cat ../pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`

java -jar $DIR/target/shongo-client-web-$VERSION.jar "$@"