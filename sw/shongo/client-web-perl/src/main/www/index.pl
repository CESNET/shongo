#!/usr/bin/perl
#
# Shongo web client
#
package main;

use strict;
use warnings;

# Setup lib directory
use File::Basename;
my $script_directory;
BEGIN {
    $script_directory = dirname( __FILE__ );
}
use lib "$script_directory/../perl";

# Get directories
use File::Spec::Functions;
use File::Basename;
my $directory = File::Spec::Functions::rel2abs(File::Basename::dirname($0));
my $resources_directory = $directory . '/../resources';
my $current_directory = $directory . '/../../..';
if ( exists($ENV{'SHONGO_CLIENT_CURRENT_DIR'}) ) {
    $current_directory = $ENV{'SHONGO_CLIENT_CURRENT_DIR'};
}

use CGI;
use CGI::Session;
use Template;
use Shongo::Common;
use Shongo::ClientWeb;
use XML::Simple;
use Hash::Merge::Simple;
use Log::Log4perl;

# Initialize CGI
my $cgi = CGI->new();
my $session = CGI::Session->new(undef, $cgi, {Directory => '/tmp'});
$session->expire('+15m');

# Initialize templates
my $template = Template->new({
    INCLUDE_PATH  => $resources_directory,
    ENCODING => 'utf8'
});

# Initialize logger
my $logger_file = $current_directory . '/log/client-web.log';
my $logger_configuration = "";
$logger_configuration .= "log4perl.rootLogger = DEBUG,  FILE\n";
$logger_configuration .= "log4perl.appender.FILE = Log::Log4perl::Appender::File\n";
$logger_configuration .= "log4perl.appender.FILE.filename = $logger_file\n";
$logger_configuration .= "log4perl.appender.FILE.layout = PatternLayout\n";
$logger_configuration .= "log4perl.appender.FILE.layout.ConversionPattern = %p %d [%F:%L] %n %c: %m%n \n";
Log::Log4perl::init(\$logger_configuration);
my $logger = Log::Log4perl->get_logger('cz.cesnet.shongo.client-web');

# Load configuration
my $configuration = $session->param('configuration');
if ( !defined($configuration) ) {
    $logger->debug('Loading configuration...');
    $configuration = XMLin($resources_directory . '/default.cfg.xml');
    my $configuration_filename = $current_directory . '/client-web.cfg.xml';
    if ( -e $configuration_filename ) {
        $configuration = Hash::Merge::Simple::merge($configuration, XMLin($configuration_filename));
    }
    $session->param('configuration', $configuration);
}

# Catch errors
use CGI::Carp qw(fatalsToBrowser);
BEGIN {
    my $processing_error = 0;
    sub carp_error {
        my $error = shift;
        my $error_application = Shongo::Web::Application->new($cgi, $template);
        if ( $processing_error ) {
            select STDOUT;
            $error_application->render_headers();
            print "Unexpected Error: " . $error . "\n";
            exit(0);
        }
        $processing_error = 1;

        use Devel::StackTrace;
        my $stack_trace = '';
        my $index = 0;
        foreach my $frame (Devel::StackTrace->new()->frames) {
            if ( !($frame->{'subroutine'} eq 'Devel::StackTrace::new') ) {
                $index++;
                my $filename = $frame->{'filename'};
                $filename =~ s/^\/.+\/main\/perl\///g;
                $filename =~ s/^\/.+\/src\/public\///g;
                $filename =~ s/^\/.+\/perl\d\///g;
                $filename =~ s/^\/.+\/perl\/\d+(.\d+)+\///g;
                $stack_trace .= sprintf("%d. %s (%d) %s()\n",
                    $index,
                    $filename,
                    $frame->{'line'},
                    $frame->{'subroutine'}
                );
            }
        }

        if ( defined($configuration->{'administrator'}) ) {
            var_dump($configuration);
            use Email::Sender::Simple qw(sendmail);
            use Email::Sender::Transport::SMTP;

            my $transport = Email::Sender::Transport::SMTP->new(
                host => $configuration->{'smtp'}->{'host'},
                port => $configuration->{'smtp'}->{'port'}
            );
            my $message = Email::Simple->create(
                header => [
                    From    => $configuration->{'smtp'}->{'sender'},
                    To      => $configuration->{'administrator'},
                    Subject => $configuration->{'smtp'}->{'subject-prefix'} . 'Web Client Error',
                ],
                body => $error . "\n\n Stacktrace:\n\n" . $stack_trace,
            );
            sendmail($message, { transport => $transport });
        }

        $error_application->error_action($error);
    }
    CGI::Carp::set_die_handler( \&carp_error );
}

# Initialize application
my $application = Shongo::ClientWeb->new($cgi, $template, $session);
$application->load_configuration($configuration);
$application->add_action('changelog', sub {
    open my $file, $current_directory . '/CHANGELOG' or die "Could not open changelog: $!";
    my $changelog = '';
    $changelog .= "<ul>";
    while( my $line = <$file>)  {
        if ( $line =~ /(\d+\.\d+\.\d+) \((.+)\)/ ) {
            $changelog .= "</ul>";
            $changelog .= "<strong>$1</strong> ($2)";
            $changelog .= "<ul>";
        }
        elsif ( $line =~ /\*(.+)/ ) {
            $changelog .= "<li>$1<br>";
        }
        else {
            $changelog .= $line;
        }
    }
    $changelog .= "</ul>";


    $application->render_page('Changelog', 'changelog.html', {
        'changelog' => $changelog,
    });
});

# Run application and catch response
my $response = '';
{
    open(CAUGHT_OUTPUT, '>', \$response);
    binmode CAUGHT_OUTPUT, ':utf8';
    select CAUGHT_OUTPUT;

    # Print history before
    #var_dump('history before');var_dump($session->param('history'));

    $application->run($ARGV[0]);

    select STDOUT;
}

# If response doesn't contains headers, add default headers
if ( !($response =~ /^(Status|Content-Type|Location)/) ) {
    $application->render_headers();
}

# Print history after
#var_dump('history after');var_dump($session->param('history'));

# Print response
if ( length($response) > 0 ) {
    print($response);
} else {
    $response = $application->render_page_content();
}
