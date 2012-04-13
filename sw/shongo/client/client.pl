#!/usr/bin/perl
#
# Shongo command line client
#
package main;

use strict;
use warnings;

use Shongo::Client::Controller;
use Shongo::Client::Shell;
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
      "    -url=URL      Set controller url\n" .
      "    -cmd=COMMAND  Perform given command in controller\n" .
      "    -file=FILE    Perform commands from file in controller\n"
   );
   exit(0);
}

# Parse command line
my $url = 'http://localhost:8008';
my $cmd;
my $file;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'url=s' => \$url,
    'cmd=s' => \$cmd,
    'file=s' => \$file
) or usage("Invalid commmand line options.");

# Print help
if ( $help == 1) {
    usage();
    exit(0);
}

# Connect to controller
my $controller = Shongo::Client::Controller->instance();
if ( $controller->connect($url) == 0) {
    exit(-1);
}
$controller->print_info();

# Create shell
my $shell = Shongo::Client::Shell->new();

# Run single command
if ( defined($cmd) ) {
    $shell->command($cmd);
}
# Run commands from file
elsif ( defined($file) ) {
    open(FILE1, $file) || die "Error openning file '$file': $!\n";
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

exit(0);
