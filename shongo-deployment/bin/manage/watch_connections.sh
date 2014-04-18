#!/bin/bash
#
# Watch TCP connections on localhost.
#

watch 'netstat -natp | grep -E "java|Proto" | grep -v -E ":::|127.0.0.1"'
