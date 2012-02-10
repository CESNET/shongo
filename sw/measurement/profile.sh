#!/bin/bash

###########################################################
# Get info about process and all its children recursive
###########################################################

pidtree()
{
    local list=
    echo -n $1
    for child in $(ps -o pid --no-headers --ppid $1); do
        children=$(echo `pidtree $child`)
        list="$list,$children"    
    done
    echo -n $list
}

ps --pid="`pidtree $1`" -o "pid,thcount,pcpu,rss,comm"
