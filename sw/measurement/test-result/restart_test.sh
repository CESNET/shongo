#!/bin/bash

FOLDER=`dirname $0`
COUNT=10

$FOLDER/../launcher.sh -l test-case/test-restart.xml -p fuse -D"count=$COUNT" > $FOLDER/restart_fuse.dat
$FOLDER/../launcher.sh -l test-case/test-restart.xml -p mule -D"count=$COUNT" > $FOLDER/restart_mule.dat

# TODO: Make correction of restarting of Jade agents
# $FOLDER/../launcher.sh -l test-case/test-restart.xml -p jade -D"count=$COUNT" > $FOLDER/restart_jade.dat
