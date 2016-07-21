#!/usr/bin/perl
#
# Shongo command line client
#
package main;

use strict;
use warnings;
use utf8;

use Getopt::Long;
use Shongo::Console;
use Shongo::Common;
use Shongo::ClientCli;
use Shongo::ClientCli::Shell;
use Sys::Hostname::FQDN qw (fqdn);

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
      "    -help                          Show this usage information\n" .
      "    -connect=<URL>                 Connect to a specified controller\n" .
      "    -ssl                           Explicitly use SSL connection\n" .
      "    -ssl-unverified                Do not verify hostname for SSL connection\n" .
      "    -token=<TOKEN>                 Use specified access token for authentication\n" .
      "    -scripting                     Switch to scripting mode\n" .
      "    -cmd=<COMMAND>                 Perform given command in controller\n" .
      "    -file=<FILE>                   Perform commands from file in controller\n"
   );
   exit(0);
}

# Parse command line
my $connect;
my $ssl;
my $ssl_unverified;
my $cmd;
my $file;
my $root_access_token = undef;
my $access_token = undef;
my $scripting = 0;
my $help = 0;
Getopt::Long::GetOptions(
    'help' => \$help,
    'connect:s' => \$connect,
    'ssl' => \$ssl,
    'ssl-unverified' => \$ssl_unverified,
    'token=s' => \$access_token,
    'scripting' => \$scripting,
    'cmd=s@' => \$cmd,
    'file=s' => \$file
) or usage('Invalid commmand line options.');
if ( $help == 1) {
    usage();
}

my $controller = Shongo::ClientCli->instance();
$controller->set_scripting($scripting);
$controller->set_ssl($ssl, $ssl_unverified);

# Parse configuration file
my $configuration_file = 'shongo-client-cli.cfg.xml';
if (-e $configuration_file) {
    my $configuration = XML::Twig->new(
        twig_handlers => {
            'configuration/security/server' => sub {
                my ($twig, $node) = @_;
                $controller->{'authorization'}->set_url('https://' . $node->text);
            },
            'configuration/security/client-id' => sub {
                my ($twig, $node) = @_;
                $controller->{'authorization'}->set_client_id($node->text);
            },
            'configuration/security/client-secret' => sub {
                my ($twig, $node) = @_;
                $controller->{'authorization'}->set_client_secret($node->text);
            },
            'configuration/security/redirect-uri' => sub {
                my ($twig, $node) = @_;
                $controller->{'authorization'}->set_redirect_uri($node->text);
            },
        }
    );
    $configuration->parsefile($configuration_file);
}

# Set specified access token
if (defined($access_token)) {
    $controller->{'client'}->set_access_token($access_token);
}
# Set root access token
else {
    my $root_access_token_file = 'root.access-token';
    open(FILE, $root_access_token_file) || die "Error openning file $root_access_token_file: $!\n";
    my @lines = <FILE>;
    my $root_access_token = $lines[0];
    close(FILE);
    $controller->{'client'}->set_access_token($root_access_token);
}

if ( $scripting eq 0 ) {
    print 'Shongo Command-Line Client ${shongo.version}';
    print "\n";
}

if ( !defined($connect) || $connect eq '' ) {
    my $hostname;
    if ($ssl) {
        $hostname = fqdn();
    } else {
        $hostname = '127.0.0.1';
    }
    $connect = 'http://' . $hostname . ':8181';
}
if ( $controller->connect($connect, $ssl)) {
    if ( !$controller->is_scripting() ) {
        $controller->status();
    }
} else {
    exit(-1);
}

# load history
my $history_file = get_home_directory() . '/.shongo_client';
history_load($history_file);

# Create shell
my $shell = Shongo::ClientCli::Shell->new(defined($cmd) || defined($file) || $scripting);

# Run command from argument
if ( defined($cmd) ) {
    foreach my $item (@{$cmd}) {
        $shell->command($item);
    }
}

# Run command from file
elsif ( defined($file) || $scripting ) {
    # Load lines from file or standard input
    my @lines;
    if ( defined($file) ) {
        open(FILE, $file) || die "Error openning file $file: $!\n";
        @lines = <FILE>;
        close(FILE);
    }
    else {
        @lines = <STDIN>;
    }

    # Process lines
    my $command = '';
    my $object_parsing = 0;
    foreach my $line (@lines) {
LINE:
        # check if $line is empty
        if ( !defined($line) ) {
            $line = '';
        }
        if ( $line =~ /^\s*(#.+)?\s*$/ ) {
            # execute not empty command
            if ( !($command =~ /^\s*$/) ) {
                $shell->command($command);
                $command = '';
            }
        }
        # check if not empty $line should be appended to object parsing
        elsif ( $object_parsing ) {
            # check if $line is object ending
            if ( $line =~ /^\s*}\s*$/ ) {
                while ( $line =~ /^\s*}\s*$/ ) {
                    $command .= '}';
                    $line = <STDIN>;
                    if ( !defined($line) ) {
                        $line = '';
                    }
                }
                # execute object command
                $shell->command($command);
                $command = '';
                $object_parsing = 0;
                goto LINE;
            }
            else {
                $line =~ s/^\s+//;
                $line =~ s/\s+$//;
                $command .= $line;
            }
        }
        # check if $line is object command begin
        elsif ( $line =~ /^.*(=)?\s*{.*$/ ) {
            $line =~ s/^\s+//;
            $line =~ s/\s+$//;
            # check if $line contains object command end
            if ( $line =~ /^[^=]*(=)?\s*{[^=]*}[^=]*$/ ) {
                # execute object command
                $shell->command($line);
            }
            else {
                $command .= $line;
                $object_parsing = 1;
            }
        }
        else {
            $line =~ s/^\s+//;
            $line =~ s/\s+$//;
            $shell->command($line);
        }

    }
    # execute last not empty command
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
