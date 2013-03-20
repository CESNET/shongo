#!/usr/bin/perl
#
# Shongo command line client
#
package main;

use strict;
use warnings;

# Setup lib directory
use FindBin;

use Getopt::Long;
use Shongo::Console;
use Shongo::Common;
use Shongo::ClientCli;
use Shongo::ClientCli::Shell;

# Check readline version
if ( !defined(Term::ReadLine->Features->{'setHistory'}) ) {
    console_print_error('Term::ReadLine doesn\'t support setHistory!');
    console_print_error('Please install Term::ReadLine::Gnu (e.g., by \'sudo apt-get install libterm-readline-gnu-perl\'!');
    exit(0);
}

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
      "    -help                        Show this usage information\n" .
      "    -connect=URL                 Connect to a controller\n" .
      "    -testing-access-token        Use testing access token for authentication\n" .
      "    -authentication-server=HOST  Use given authentication server\n" .
      "    -scripting                   Switch to scripting mode\n" .
      "    -cmd=COMMAND                 Perform given command in controller\n" .
      "    -file=FILE                   Perform commands from file in controller\n"
   );
   exit(0);
}

# Parse command line
my $connect;
my $cmd;
my $file;
my $authentication_server = undef;
my $testing_access_token = undef;
my $scripting = 0;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'connect:s' => \$connect,
    'testing-access-token:s' => \$testing_access_token,
    'authentication-server:s' => \$authentication_server,
    'scripting' => \$scripting,
    'cmd=s@' => \$cmd,
    'file=s' => \$file
) or usage('Invalid commmand line options.');
if ( $help == 1) {
    usage();
}

if ( !defined($authentication_server) ) {
    $authentication_server = 'shongo-auth.cesnet.cz';
}

my $controller = Shongo::ClientCli->instance();
$controller->set_scripting($scripting);
$controller->set_authorization_url('https://' . $authentication_server);

# Set testing access token
if (defined($testing_access_token)) {
    if ( $testing_access_token eq '' ) {
        $testing_access_token = '1e3f174ceaa8e515721b989b19f71727060d0839';
    }
    $controller->{'client'}->set_access_token($testing_access_token);
}

if ( $scripting eq 0 ) {
    print 'Shongo Command-Line Client ${shongo.version}';
    print "\n";
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

# Run command from argument
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
# Run from standard input
elsif ( $scripting ) {
    my $command = '';
    while ( my $line = <STDIN> ) {
        if ( $line =~ /^\s*$/ ) {
            if ( !($command =~ /^\s*$/) ) {
                $shell->command($command);
                $command = '';
            }
        }
        else {
            $line =~ s/^\s+//;
            $line =~ s/\s+$//;
            $command .= $line;
        }
    }
    if ( !($command =~ /^\s*$/) ) {
        $shell->command($command);
    }
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