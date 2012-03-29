#!/bin/bash

FOLDER=`dirname $0`
COUNT=2

$FOLDER/../launcher.sh -l test-case/test-latency.xml -p jxta -D"count=$COUNT" > $FOLDER/latency_jxta.dat
$FOLDER/../launcher.sh -l test-case/test-latency.xml -p fuse -D"count=$COUNT" > $FOLDER/latency_fuse.dat
$FOLDER/../launcher.sh -l test-case/test-latency.xml -p mule -D"count=$COUNT" > $FOLDER/latency_mule.dat
$FOLDER/../launcher.sh -l test-case/test-latency.xml -p jade -D"count=$COUNT" > $FOLDER/latency_jade.dat
