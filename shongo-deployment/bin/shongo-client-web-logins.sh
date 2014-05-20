#!/bin/bash

# Parse arguments
while test $# -gt 0
do
    case "$1" in
        --unique)
            UNIQUE=1
            ;;
        --help)
            cat <<EOF
usage: $0 [options] [domain]
    -help     Show this usage information
    -unique   Show only unique user names
EOF
            exit 0
            ;;
        *)
            DOMAIN="$1"
            ;;
    esac
    shift
done

# Prepare variables
TMP_FILE=$(mktemp)
LOG="cat /var/log/shongo/shongo-client-web*"
PATTERN_USER="id: \(.\+\),.*name: \([^ ]\+\)\( \([^ ]\+\)\)\?)"
if [[ $UNIQUE ]]; then
    PATTERN="s/.*$PATTERN_USER.*/\4 \2 (\1)/g"
else
    PATTERN="s/\([0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}\):.*$PATTERN_USER.*/\1: \5 \3 (\2)/g"
fi

# Grep shongo-client-web logs to get logins
if [[ ! -z $DOMAIN ]]; then
    ssh $DOMAIN -l root "$LOG"
else
    $LOG
fi \
| grep authenticated \
| sed "$PATTERN" \
> $TMP_FILE

# Show requested results
if [[ $UNIQUE ]]; then
    cat $TMP_FILE | sort | uniq
else
    cat $TMP_FILE
fi
rm $TMP_FILE