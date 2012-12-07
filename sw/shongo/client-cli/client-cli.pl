#!/usr/bin/perl
#
# Shongo command line client
#
package main;

use strict;
use warnings;

# Setup lib directory
use FindBin;
use lib "$FindBin::Bin/src/main/perl";
use lib "$FindBin::Bin/../client-common/src/main/perl";

use Getopt::Long;
use Shongo::Console;
use Shongo::Common;
use Shongo::ClientCli;
use Shongo::ClientCli::Shell;

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
      "    -scripting             Switch to scripting mode\n" .
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
my $scripting = 0;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'connect:s' => \$connect,
    'testing-access-token' => \$testing_access_token,
    'scripting' => \$scripting,
    'cmd=s@' => \$cmd,
    'file=s' => \$file
) or usage('Invalid commmand line options.');
if ( $help == 1) {
    usage();
    exit(0);
}

my $controller = Shongo::ClientCli->instance();
$controller->set_scripting($scripting);

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
        if ( !$controller->is_scripting() ) {
            $controller->status();
        }
    } else {
        exit(-1);
    }
}

# load history
my $history_file = get_home_directory() . '/.shongo_client';
history_load($history_file);

# Create shell
my $shell = Shongo::ClientCli::Shell->new();

# Run single commands
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
    $controller->set_scripting(0);
    $shell->run();
    $controller->set_scripting($scripting);
}

# save history
history_save($history_file);

# Disconnect from controller
if ( $controller->is_connected() ) {
    $controller->disconnect();
}

exit(0);
