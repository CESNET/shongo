#!/usr/bin/perl
#
# Shongo web client
#
package main;

use strict;
use warnings;

# Setup lib directory
use FindBin;
use lib "$FindBin::Bin/../main/perl";
use lib "$FindBin::Bin/../../../client-common/src/main/perl";

# Get directories
use File::Spec::Functions;
use File::Basename;
my $directory = File::Spec::Functions::rel2abs(File::Basename::dirname($0));
my $config_directory = $directory . '/../../..';
my $resources_directory = $directory . '/../resources';

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

# Catch errors
use CGI::Carp qw(fatalsToBrowser);
BEGIN {
    sub carp_error {
        my $error = shift;
        my $error_application = Shongo::Web::Application->new($cgi, $template);
        $error_application->error_action($error);
    }
    CGI::Carp::set_die_handler( \&carp_error );
}

# Initialize logger
my $logger_file = $config_directory . '/data/log/client-web.log';
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
    $configuration = XMLin($resources_directory . '/default.cfg.xml', KeyAttr => {}, ForceArray => []);
    my $configuration_filename = $config_directory . '/client-web.cfg.xml';
    if ( -e $configuration_filename ) {
        $configuration = Hash::Merge::Simple::merge($configuration, XMLin($configuration_filename));
    }
    $session->param('configuration', $configuration);
}

# Initialize application
my $application = Shongo::ClientWeb->new($cgi, $template, $session);
$application->load_configuration($configuration);

# Run application and catch response
my $response = '';
{
    open(CAUGHT_OUTPUT, '>', \$response);
    select CAUGHT_OUTPUT;

    $application->run($ARGV[0]);

    select STDOUT;
}

# If response doesn't contains headers, add default headers
if ( !($response =~ /^(Status|Content-Type|Location)/) ) {
    $application->render_headers();
}
# Print response
if ( length($response) > 0 ) {
    print($response);
} else {
    $response = $application->render_page_content();
}
