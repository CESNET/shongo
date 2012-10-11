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
use Shongo::Controller::Shell;

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
      "    -help                  Show this usage information\n" .
      "    -connect=URL           Connect to a controller\n" .
      "    -testing-access-token  Use testing access token for authentication\n" .
      "    -cmd=COMMAND           Perform given command in controller\n" .
      "    -file=FILE             Perform commands from file in controller\n"
   );
   exit(0);
}

# Parse command line
my $connect;
my $cmd;
my $file;
my $testing_access_token = 0;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'connect:s' => \$connect,
    'testing-access-token' => \$testing_access_token,
    'cmd=s@' => \$cmd,
    'file=s' => \$file
) or usage('Invalid commmand line options.');
if ( $help == 1) {
    usage();
    exit(0);
}

my $controller = Shongo::Controller->instance();

# Set testing access token
if ($testing_access_token) {
    $controller->{'access_token'} = '1e3f174ceaa8e515721b989b19f71727060d0839';
}

# Connect to controller
if ( defined($connect) ) {
    if ( $connect eq '') {
        $connect = 'http://127.0.0.1:8181';
    }
    if ( $controller->connect($connect)) {
        $controller->status();
    } else {
        exit(-1);
    }
}

# load history
my $history_file = get_home_directory() . '/.shongo_client';
history_load($history_file);

# Create shell
my $shell = Shongo::Controller::Shell->new();

# Run single command
if ( defined($cmd) ) {
    foreach my $item (@{$cmd}) {
        $shell->command($item);
    }
}
# Run command from file
elsif ( defined($file) ) {
    open(FILE, $file) || die "Error openning file $file: $!\n";
    my @lines = <FILE>;
    foreach my $line (@lines) {
        $line =~ s/\s+$//;
        $shell->command($line);
    }
    close(FILE);
}
# Run shell
else {
    $shell->run();
}

# save history
history_save($history_file);

# Disconnect from controller
if ( $controller->is_connected() ) {
    $controller->disconnect();
}

exit(0);
