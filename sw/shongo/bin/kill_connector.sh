#!/bin/bash
#
# Kill running Shongo connector.
#

PID=$(ps -eo pid,cmd | grep [c]onnector- | sed -e 's/\([0-9]\+\).\+/\1/g')
if [ -n "$PID" ]; then
  echo "Killing connector at PID [$PID]..."
  kill -9 $PID
fi
