#!/bin/bash
#
# Run Shongo command-line client application.
#

DIR=`dirname $0`/../../shongo-client-cli

# If no token argument is passed, load the root.access-token
if [[ ! "$@" == *-token* ]]; then
  ROOT_ACCESS_TOKEN=$(cat `dirname $0`/../root.access-token)
  if [[ $ROOT_ACCESS_TOKEN == "" ]]; then
    exit 1
  fi
  ARGUMENTS="-token $ROOT_ACCESS_TOKEN"
fi

if [ "$1" = "test" ]
then
    shift
    ARGUMENTS="$@"
    perl -I$DIR/src/main/perl $DIR/src/test/perl/client-cli-test.pl $ARGUMENTS "$@"
else
    if [ "$1" = "src" ]
    then
        shift
        perl -I$DIR/src/main/perl $DIR/src/main/perl/client-cli.pl $ARGUMENTS "$@"
    else
        perl -I$DIR/target $DIR/target/client-cli.pl $ARGUMENTS "$@"
    fi
fi
