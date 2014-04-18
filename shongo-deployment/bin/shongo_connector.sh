#!/bin/bash
#
# Run Shongo connector application.
#
cd `dirname $0`/../
DIR=../shongo-connector
VERSION=`cat ../pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`

java -jar $DIR/target/shongo-connector-$VERSION.jar "$@"
