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
# Populate shell by options for managing controller connection.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    my @tree = (
        'connect' => {
            help => 'Connect to a controller. You must specify an <URL>.',
            exec => sub {
                my ($shell, %p) = @_;
                my $url = $p{ARGV}[0];
                if (defined($url) == 0) {
                    my $controller = Shongo::Client::Controller->instance();
                    if ( defined($controller->{"_url"}) ) {
                        $url = $controller->{"_url"};
                    } else {
                        print("[ERROR] You must specify <URL> when connecting to controller.\n");
                        return;
                    }
                }
                Shongo::Client::Controller->instance()->connect($url);
            },
        },
        'disconnect' => {
            help => 'Disconnect from a controller.',
            exec => sub {
                Shongo::Client::Controller->instance()->disconnect();
            },
        },
        'status' => {
            help => 'Show status and information about connected controller.',
            exec => sub {
                Shongo::Client::Controller->instance()->status();
            },
        }
    );
    $shell->populate(@tree);
}

#
# Connect to Controller XML-RPC server.
#
# @param url
#
sub connect()
{
    my ($self, $url) = @_;

    # Append default port if not presented
    if ( !($url =~ /.+:[0-9]+$/ ) ) {
        $url = $url . ":8181";
    }
    # Prepend http:// if not presented
    if ( !($url =~ /^http:\/\// ) ) {
        $url = 'http://' . $url;
    }

    $self->{"_url"} = $url;

    print("Connecting to controller at '$url'...\n");
    my $client = RPC::XML::Client->new($url);
    my $response = $client->send_request("Common.getControllerInfo");
    if ( ref($response) ) {
        $self->{"_client"} = $client;
        print("Successfully connected to the controller!\n");
        return 1;
    } else {
        print("Failed to connect to controller! Is the controller running?\n");
        return 0;
    }
}

#
# Disconnect from Controller XML-RPC server.
#
sub disconnect()
{
    my ($self) = @_;
    if ( !defined($self->{"_client"}) ) {
        print("[ERROR] Client is not connected to any controller!\n");
        return;
    }
    undef $self->{"_client"};
    print("Successfully disconnected from the controller!\n");
}

#
# Checks whether client is connected to controller
#
sub is_connected()
{
    my ($self) = @_;
    if ( !defined($self->{"_client"}) ) {
        return 0;
    }
    return 1;
}

#
# Send request to Controller XML-RPC server.
#
# @param... Arguments for XML-RPC request
# @return response
#
sub request()
{
    my ($self, @args) = @_;
    my $response = $self->{"_client"}->send_request(@args);
    if ( !ref($response) ) {
        print("[ERROR] Failed to send request to controller!\n" . $response . "\n");
        return;
    }
    if ( $response->is_fault() ) {
        print("[ERROR] Server failed to perform request!\n" . $response->string . "\n");
    }
    return $response;
}

#
# Print Controller XML-RPC server info
#
sub status()
{
    my ($self) = @_;
    if ( !defined($self->{"_client"}) ) {
        print("[ERROR] Client is not connected to any controller!\n");
        return;
    }

    my $response = $self->request("Common.getControllerInfo");
    if ( !ref($response) || $response->is_fault() ) {
        return;
    }
    if ( !($response->{"class"}->value eq "ControllerInfo") ) {
        print("[ERROR] Server hasn't return ControllerInfo object!\n");
        return;
    }
    printf("+----------------------------------------------------------------------+\n");
    printf("| Connected to following controller:                                   |\n");
    printf("| -------------------------------------------------------------------- |\n");
    printf("| URL:         %-55s |\n", $self->{'_url'});
    printf("| Name:        %-55s |\n", $response->{"name"}->value);
    printf("| Description: %-55s |\n", substr($response->{"description"}->value, 0, 55));
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