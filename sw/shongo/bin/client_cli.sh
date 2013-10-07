#!/bin/bash
#
# Run Shongo command-line client application.
#

cd `dirname $0`/../

if [ "$1" = "test" ]
then
    shift
    perl -Iclient-cli/src/main/perl client-cli/src/test/perl/client-cli-test.pl "$@"
else
    if [ "$1" = "src" ]
    then
        shift
        perl -Iclient-cli/src/main/perl client-cli/src/main/perl/client-cli.pl "$@"
    else
        perl -Iclient-cli/target client-cli/target/client-cli.pl "$@"
    fi
fi