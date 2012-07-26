#
# Controller class - Manages XML-RPC connection to controller.
#
package Shongo::Controller;

use strict;
use warnings;

use RPC::XML;
use RPC::XML::Client;
use XML::Twig;
use Shongo::Console;

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
# Populate shell by options for controller.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        "connect" => {
            desc => "Connect to a controller. You must specify an <URL>.",
            maxargs => 1,
            args => sub { return ['127.0.0.1', 'localhost']; },
            proc => sub {
                my $url = $_[0];
                if (defined($url) == 0) {
                    my $controller = Shongo::Controller->instance();
                    if ( defined($controller->{"_url"}) ) {
                        $url = $controller->{"_url"};
                    } else {
                        console_print_error("You must specify <URL> when connecting to controller.\n");
                        return;
                    }
                }
                Shongo::Controller->instance()->connect($url);
            },
        },
        "disconnect" => {
            desc => "Disconnect from a controller.",
            proc => sub {
                Shongo::Controller->instance()->disconnect();
            }
        },
        "status" => {
            desc => "Show status and information about connected controller.",
            proc => sub {
                Shongo::Controller->instance()->status();
            }
        },
    });
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
    my $response = $client->send_request("Common.getController");
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
        console_print_error("Client is not connected to any controller!\n");
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
# Checks whether client is connected to controller
#
sub check_connected()
{
    my ($self) = @_;
    if ( !$self->is_connected() ) {
        console_print_error("Client is not connected to any controller!\n");
        return 0;
    }
    return 1;
}

#
# Send request to Controller XML-RPC server.
#
# @param... Arguments for XML-RPC request
# @param method
# @return response
#
sub request()
{
    my ($self, $method, @args) = @_;
    if ( !$self->check_connected() ) {
        return RPC::XML::fault->new(0, "Not connected!");
    }
    my $response = $self->{"_client"}->send_request($method, @args);
    if ( !ref($response) ) {
        console_print_error("Failed to send request to controller!\n" . $response . "\n");
        return RPC::XML::fault->new(0, "Failed to send request!");;
    }
    if ( $response->is_fault() ) {
        console_print_error("Server failed to perform request!\nFault %d: %s",
            $response->code, $response->string);
    }
    return $response;
}

#
# Send request to Controller XML-RPC server with auto fill first security token parameter.
#
# @param method
# @param... Arguments for XML-RPC request
# @return response
#
sub secure_request()
{
    my ($self, $method, @args) = @_;
    return $self->request(
        $method,
        RPC::XML::struct->new('class' => 'SecurityToken'),
        @args
    );
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

    my $response = $self->request("Common.getController");
    if ( !ref($response) || $response->is_fault() ) {
        return;
    }
    if ( !($response->{"class"}->value eq "Controller") ) {
        print("[ERROR] Server hasn't return Controller object!\n");
        return;
    }
    printf("+----------------------------------------------------------------------+\n");
    printf("| Connected to the following controller:                               |\n");
    printf("| -------------------------------------------------------------------- |\n");
    printf("| URL:                 %-47s |\n", $self->{'_url'});
    printf("| Domain Name:         %-47s |\n", $response->{"domain"}->{"name"}->value);
    printf("| Domain Organization: %-47s |\n", $response->{"domain"}->{"organization"}->value);
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