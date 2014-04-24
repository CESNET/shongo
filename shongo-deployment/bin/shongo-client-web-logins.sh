#!/bin/bash

LOG="cat /var/log/shongo/shongo-client-web*"

if [[ ! -z $1 ]]; then
    ssh shongo-test1 -l root "$LOG"
else
    $LOG
fi \
| grep authenticated \
| sed 's/\([0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}\):.*id: \(.\+\),.*name: \(.\+\)).*/\1: \3 (\2)/g'