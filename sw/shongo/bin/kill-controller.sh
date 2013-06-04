#!/bin/bash
#
# Kill running Shongo controller.
#

PID=$(ps -eo pid,cmd | grep [c]ontroller- | sed -e 's/\([0-9]\+\).\+/\1/g')
if [ -n "$PID" ]; then
  echo "Killing controller at PID [$PID]..."
  kill -9 $PID
fi
