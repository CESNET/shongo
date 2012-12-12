#
# Controller class - Manages XML-RPC connection to controller.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli;

use strict;
use warnings;

use RPC::XML;
use RPC::XML::Client;
use XML::Twig;
use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::CliAuthorization;
use Shongo::ClientCli::API::Object;

#
# Single instance of ClientCli class.
#
my $singleInstance;

#
# Get single instance of Controller class.
#
sub instance
{
    unless (defined($singleInstance)) {
        my $class = shift;
        my $self = {};
        $singleInstance = bless $self, $class;
        $singleInstance->{'scripting'} = 0;
        $singleInstance->{'authorization'} = Shongo::ClientCli::CliAuthorization->new();
    }
    return $singleInstance;
}

#
# Enable/disable scripting mode.
#
# @param $scripting  enable/disable scripting mode (0|1).
#
sub set_scripting()
{
    my ($self, $scripting) = @_;
    $self->{'scripting'} = $scripting;
}

#
# @return true if the scripting mode is enabled
#
sub is_scripting()
{
    my ($self, $scripting) = @_;
    if ( !ref($self) ) {
        $self = instance();
    }
    return $self->{'scripting'};
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
                    my $controller = Shongo::ClientCli->instance();
                    if ( defined($controller->{"_url"}) ) {
                        $url = $controller->{"_url"};
                    } else {
                        console_print_error("You must specify <URL> when connecting to controller.\n");
                        return;
                    }
                }
                Shongo::ClientCli->instance()->connect($url);
            },
        },
        "disconnect" => {
            desc => "Disconnect from a controller.",
            proc => sub {
                Shongo::ClientCli->instance()->disconnect();
            }
        },
        "status" => {
            desc => "Show status and information about connected controller.",
            proc => sub {
                Shongo::ClientCli->instance()->status();
            }
        },
        "authenticate" => {
            desc => "Perform user authentication",
            method => sub {
                my $controller = Shongo::ClientCli->instance();
                if ( defined($controller->authenticate()) ) {
                    $controller->user_info($controller->{'access_token'});
                }
            }
        },
        "user-info" => {
            desc => "Show authenticated user information",
            method => sub {
                my $controller = Shongo::ClientCli->instance();
                $controller->user_info();
            }
        },
    });
}

#
# @param $user_id user-id of the user
# @return user info formatted to string for user with given $user_id
#
sub format_user
{
    my ($self, $user_id) = @_;
    return $user_id;
}

#
# Authenticate user
#
sub authenticate()
{
    my ($self) = @_;
    $self->{'access_token'} = $self->{'authorization'}->authentication_authorize();
    return $self->{'access_token'};
}

#
# Show current authenticated user information
#
sub user_info()
{
    my ($self) = @_;
    console_print_debug("Retrieving user information for access token '%s'...", $self->{'access_token'});
    my $user_info = $self->{'authorization'}->user_info($self->{'access_token'});
    if (!defined($user_info)) {
        return;
    }

    my $object = Shongo::ClientCli::API::Object->new();
    $object->set_object_name('Authenticated User Information');
    $object->add_attribute('Access Token', {}, $self->{'access_token'});
    $object->add_attribute('Id', {}, $user_info->{'id'});
    $object->add_attribute('Identity', {}, $user_info->{'original_id'});
    $object->add_attribute('Name', {}, $user_info->{'name'});
    $object->add_attribute('Email', {}, $user_info->{'email'});
    console_print_text($object);
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

    console_print_debug("Connecting to controller at '$url'...");

    my $client = RPC::XML::Client->new($url);
    my $response = $client->send_request("Common.getController");
    if ( ref($response) ) {
        $self->{"_client"} = $client;
        console_print_debug("Successfully connected to the controller!");
        return 1;
    } else {
        console_print_error("Failed to connect to controller! Is the controller running?");
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
        console_print_error("Client is not connected to any controller!");
        return;
    }
    undef $self->{"_client"};
    console_print_debug("Successfully disconnected from the controller!");
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
        console_print_error("Client is not connected to any controller!");
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
        console_print_error("Failed to send request to controller!\n" . $response);
        return RPC::XML::fault->new(0, "Failed to send request!");;
    }
    if ( $response->is_fault() ) {
        my $message = $response->string();
        if ( $message =~ /<message>(.*)<\/message>/ ) {
            $message = $1;
        }
        console_print_error("Server failed to perform request!\nFault %d: %s",
            $response->code, $message);
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
    if ( !$self->check_connected() ) {
        return RPC::XML::fault->new(0, "Not connected!");
    }
    my $securityToken = RPC::XML::struct->new();
    if (!defined($self->{'access_token'})) {
        console_print_debug("User is not authenticated, starting authentication...");
        $self->authenticate();
    }
    if (defined($self->{'access_token'})) {
        $securityToken = RPC::XML::string->new($self->{'access_token'});
    }
    return $self->request(
        $method,
        $securityToken,
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
        console_print_error("Client is not connected to any controller!");
        return;
    }

    my $response = $self->request("Common.getController");
    if ( !ref($response) || $response->is_fault() ) {
        return;
    }
    if ( !($response->{"class"}->value eq "Controller") ) {
        console_print_error("Server hasn't return Controller object!");
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