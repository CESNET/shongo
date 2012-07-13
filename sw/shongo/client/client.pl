#!/usr/bin/perl
#
# Shongo command line client
#
package main;

# Setup lib directory
BEGIN {
    use File::Spec::Functions qw(rel2abs);
    use File::Basename qw(dirname);
    my $path   = rel2abs( $0 );
    our $directory = dirname( $path );
}
use lib $directory . '/src/main/perl';

use strict;
use warnings;

# Common usages of Shongo modules
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller;
use Shongo::Shell;

# Common usages of foreign modules
use Getopt::Long;

#
# Print usage
#
sub usage {
   my $message = $_[0];
   if (defined $message && length $message) {
      $message .= "\n"
         unless $message =~ /\n$/;
   } else {
    $message = '';
   }

   my $command = $0;
   $command =~ s#^.*/##;

   print STDERR (
      $message,
      "usage: $command [options]\n" .
      "    -help         Show this usage information\n" .
      "    -connect=URL  Connect to a controller\n" .
      "    -cmd=COMMAND  Perform given command in controller\n" .
      "    -file=FILE    Perform commands from file in controller\n"
   );
   exit(0);
}

# Parse command line
my $connect;
my $cmd;
my $file;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'connect:s' => \$connect,
    'cmd=s' => \$cmd,
    'file=s' => \$file
) or usage('Invalid commmand line options.');
if ( $help == 1) {
    usage();
    exit(0);
}

my $controller = Shongo::Controller->instance();

# Connect to controller
if ( defined($connect) ) {
    if ( $connect eq '') {
        $connect = 'http://127.0.0.1:8181';
    }
    if ( $controller->connect($connect)) {
        $controller->status();
    }
}

# Create shell
my $shell = Shongo::Shell->new();

# Run single command
if ( defined($cmd) ) {
    $shell->command($cmd);
}
# Run command from file
elsif ( defined($file) ) {
    open(FILE1, $file) || die "Error openning file $file: $!\n";
    my @lines = <FILE1>;
    my $line;
    foreach $line (@lines) {
        $line =~ s/\s+$//;
        $shell->command($line);
    }
}
# Run shell
else {
    $shell->run();
}

# Disconnect from controller
if ( $controller->is_connected() ) {
    $controller->disconnect();
}

exit(0);
