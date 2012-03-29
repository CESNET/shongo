#!/bin/bash

###########################################################
# Kill process and all its children recursive
###########################################################

pidkill()
{
    for child in $(ps -o pid --no-headers --ppid $1); do
        pidkill $child
    done
    echo "Killing $1"
    kill -9 $1
}

pidkill $1
