cd `dirname $0`/../

if [ "$1" = "src" ]
then
  shift
  perl -I client-common/src/main/perl -I client-cli/src/main/perl client-cli/src/main/perl/client-cli.pl "$@"
else
  perl -I client-cli/target client-cli/target/client-cli.pl "$@"
fi