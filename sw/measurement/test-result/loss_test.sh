#!/bin/bash

FOLDER=`dirname $0`
COUNT=2

$FOLDER/../launcher.sh -l test-case/test-loss.xml -p jxta -D"count=$COUNT" > $FOLDER/loss_jxta.dat
#$FOLDER/../launcher.sh -l test-case/test-loss.xml -p jade -D"count=$COUNT" > $FOLDER/loss_jade.dat
#$FOLDER/../launcher.sh -l test-case/test-loss.xml -p fuse -D"count=$COUNT" > $FOLDER/loss_fuse.dat
#$FOLDER/../launcher.sh -l test-case/test-loss.xml -p mule -D"count=$COUNT" > $FOLDER/loss_mule.dat
