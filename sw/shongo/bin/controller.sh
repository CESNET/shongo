#!/bin/bash

cd `dirname $0`/../
VERSION=`cat pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`

case $1 in
    reservation-requests)
        shift
        for id in $(bin/client-cli.sh src --connect $1 --root --scripting --cmd "list-reservation-requests" | jsawk -n "out(this.id)"); do
            bin/client-cli.sh src --connect $1 --root --scripting --cmd "get-reservation-request $id"
        done;

        ;;
    *)
        java -jar controller/target/controller-$VERSION.jar "$@"
        ;;
esac
