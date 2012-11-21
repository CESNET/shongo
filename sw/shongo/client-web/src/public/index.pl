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

# Get directory with resources
use File::Spec::Functions;
use File::Basename;
my $resources_directory = File::Spec::Functions::rel2abs(File::Basename::dirname($0)) . '/../resources';

use CGI;
use Template;
use Shongo::Common;
use Shongo::Web::Application;
use Shongo::H323SipController;
use Shongo::AdobeConnectController;

# Initialize CGI
my $cgi = CGI->new();
print $cgi->header(
    type => 'text/html'
);

# Initialize templates
my $template = Template->new({
    INCLUDE_PATH  => $resources_directory
});

# Run application
my $application = Shongo::Web::Application->new($cgi, $template);
$application->add_action('index', 'index', sub { index_action(); });
$application->add_controller('h323-sip', Shongo::H323SipController->new($application));
$application->add_controller('adobe-connect', Shongo::AdobeConnectController->new($application));
$application->run();

# Index action
sub index_action
{
    $application->render_page('Shongo', 'index.html');
}
