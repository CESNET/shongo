#!/bin/bash
#
# Run Shongo web client application.
#

cd `dirname $0`/..
VERSION=`cat pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`

java -jar client-web/target/client-web-$VERSION.jar "$@"