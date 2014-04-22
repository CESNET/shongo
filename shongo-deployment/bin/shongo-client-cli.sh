#!/bin/bash
#
# Run Shongo command-line client application.
#

cd `dirname $0`/../
DIR=../shongo-client-cli

if [ "$1" = "test" ]
then
    shift
    perl -I$DIR/src/main/perl $DIR/src/test/perl/client-cli-test.pl "$@"
else
    if [ "$1" = "src" ]
    then
        shift
        perl -I$DIR/src/main/perl $DIR/src/main/perl/client-cli.pl "$@"
    else
        perl -I$DIR/target $DIR/target/client-cli.pl "$@"
    fi
fi