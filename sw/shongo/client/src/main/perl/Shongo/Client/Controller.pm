#
# Controller class - Manages XML-RPC connection to controller.
#
package Shongo::Client::Controller;

use strict;
use warnings;

use RPC::XML;
use RPC::XML::Client;
use XML::Twig;

#
# Single instance of Controller class.
#
my $singleInstance;

#
# Get single instance of Controller class.
#
sub instance
{
    unless (defined $singleInstance) {
        my $class = shift;
        my $self = {};
        $singleInstance = bless $self, $class;
    }
    return $singleInstance;
}

#
# Connect to Controller XML-RPC server.
#
# @param url
#
sub connect()
{
    my ($self, $url) = @_;
    $self->{"_url"} = $url;
    $self->{"_name"} = "";
    $self->{"_description"} = "";

    print("Connecting to controller at '$url'...\n");
    my $client = RPC::XML::Client->new($url);
    $self->{"_client"} = $client;
    my $response = $self->{"_client"}->send_request("Common.getControllerInfo");
    if ( ref($response) ) {
        print("Successfully connected to controller!\n");

        die "Server hasn't return ControllerInfo object!\n" unless ($response->{"class"}->value eq "ControllerInfo");
        $self->{"_name"} = $response->{"name"}->value;
        $self->{"_description"} = $response->{"description"}->value;
        return 1;
    } else {
        print("Failed to connect to controller! Is the controller running?\n");
        return 0;
    }
}

#
# Send request to Controller XML-RPC server.
#
# @param... Arguments for XML-RPC request
#
sub request()
{
    my ($self, %args) = @_;
    print("Sending request...\n");
    my $response = $self->{"_client"}->send_request("hello");
    if ( ref($response) == 0 ) {

    } else {
        print($response . "\n");
        return '';
    }
    return $response;
}

#
# Print Controller XML-RPC server info
#
sub print_info()
{
    my ($self) = @_;
    printf("+----------------------------------------------------------------------+\n");
    printf("| Shongo Controller Command-Line Client                                |\n");
    printf("+----------------------------------------------------------------------+\n");
    printf("| URL:         %-55s |\n", $self->{'_url'});
    printf("| Name:        %-55s |\n", $self->{'_name'});
    printf("| Description: %-55s |\n", substr($self->{'_description'}, 0, 55));
    printf("+----------------------------------------------------------------------+\n");
}

#
# Print XML-RPC response in readable form
#
# @param response
#
sub print_response()
{
    my ($self, $response) = @_;
    my $xml = XML::Twig->new(pretty_print => 'indented');
    $xml->parse($response->as_string());
    return $xml->print();
}

1;