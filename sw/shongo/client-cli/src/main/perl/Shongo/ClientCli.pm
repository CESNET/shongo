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
use Shongo::ClientCommon;
use Shongo::ClientCli::CliAuthorization;
use Shongo::ClientCli::API::Object;

#
# Shongo::ClientCli singleton.
#
my $singleInstance;

#
# @return Shongo::ClientCli singleton
#
sub instance
{
    unless (defined($singleInstance)) {
        my $class = shift;
        my $self = {};
        $singleInstance = bless $self, $class;
        $singleInstance->{'scripting'} = 0;
        $singleInstance->{'authorization'} = Shongo::ClientCli::CliAuthorization->new();

        # Initialize client
        $singleInstance->{'client'} = Shongo::ClientCommon->new();
        $singleInstance->{'client'}->{'on-error'} = sub {
            my ($error) = @_;
            console_print_error($error);
        };
        $singleInstance->{'client'}->{'on-fault'} = sub {
            my ($fault) = @_;
            my $message = $fault->string();
            if ( $message =~ /^{"message":"([^"]*)"/ ) {
                $message = $1;
            }
            console_print_error("Server failed to perform request!\nFault %d: %s", $fault->code, $message);
        };
        $singleInstance->{'client'}->{'on-get-access-token'} = sub {
            return $self->authenticate();
        };
    }
    return $singleInstance;
}

#
# @return Shongo::ClientCommon
#
sub client()
{
    return instance()->{'client'}
}

#
# @param $url
#
sub set_authorization_url()
{
    my ($self, $url) = @_;
    $self->{'authorization'}->set_url($url);
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
            method => sub {
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
            method => sub {
                Shongo::ClientCli->instance()->disconnect();
            }
        },
        "status" => {
            desc => "Show status and information about connected controller.",
            method => sub {
                Shongo::ClientCli->instance()->status();
            }
        },
        "authenticate" => {
            desc => "Perform user authentication",
            method => sub {
                my ($shell, $params, @args) = @_;
                my $controller = Shongo::ClientCli->instance();
                if ( defined($controller->authenticate(@args)) ) {
                    $controller->get_authenticated_user();
                }
            }
        },
        "get-authenticated-user" => {
            desc => "Show authenticated user information",
            method => sub {
                my $controller = Shongo::ClientCli->instance();
                $controller->get_authenticated_user();
            }
        }
    });
}

#
# Authenticate user
#
sub authenticate()
{
    my ($self, $data) = @_;
    my $access_token = $self->{'authorization'}->authentication_authorize($data);
    $self->{'client'}->set_access_token($access_token);
    return $access_token;
}

#
# Show current authenticated user information
#
sub get_authenticated_user()
{
    my ($self) = @_;
    my $access_token = $self->{'client'}->get_access_token();
    console_print_debug("Retrieving user information for access token '%s'...", $access_token);
    my $user = $self->{'authorization'}->get_user_information($access_token);
    if (!defined($user)) {
        return;
    }

    my $object = Shongo::ClientCli::API::Object->new();
    $object->set_object_name('Authenticated User Information');
    $object->add_attribute('Access Token', {}, $self->{'access_token'});
    $object->add_attribute('Id', {}, $user->{'id'});
    $object->add_attribute('Identity', {}, $user->{'original_id'});
    $object->add_attribute('Name', {}, $user->{'name'});
    $object->add_attribute('Email', {}, $user->{'email'});
    console_print_text($object);
}

#
# @see Shongo::ClientCommon::connect
#
sub connect()
{
    my ($self, $url) = @_;

    $url = $self->{'client'}->update_url($url);

    console_print_debug("Connecting to controller at '$url'...");

    $self->{'client'}->connect($url);

    my $response = $self->{'client'}->request("Common.getController");
    if ( ref($response) ) {
        console_print_debug("Successfully connected to the controller!");
        return 1;
    } else {
        console_print_error("Failed to connect to controller! Is the controller running?");
        return 0;
    }
}

#
# @see Shongo::ClientCommon::disconnect
#
sub disconnect()
{
    my ($self) = @_;
    if ( $self->{'client'}->disconnect() ) {
        console_print_debug("Successfully disconnected from the controller!");
    }
}

#
# @see Shongo::ClientCommon::is_connected
#
sub is_connected()
{
    my ($self) = @_;
    return $self->{'client'}->is_connected();
}

#
# @see Shongo::ClientCommon::request
#
sub request()
{
    my ($self, @arguments) = @_;
    return $self->{'client'}->request(@arguments);
}

#
# @see Shongo::ClientCommon::secure_request
#
sub secure_request()
{
    my ($self, @arguments) = @_;
    return $self->{'client'}->secure_request(@arguments);
}

#
# Print Controller XML-RPC server info
#
sub status()
{
    my ($self) = @_;
    if ( !$self->{'client'}->check_connected() ) {
        return;
    }

    my $response = $self->request("Common.getController");
    if ( !defined($response) ) {
        return;
    }
    if ( !($response->{"class"} eq "Controller") ) {
        console_print_error("Server hasn't return Controller object!");
        return;
    }
    printf("+----------------------------------------------------------------------+\n");
    printf("| Connected to the following controller:                               |\n");
    printf("| -------------------------------------------------------------------- |\n");
    printf("| URL:                 %-47s |\n", $self->{'client'}->get_url());
    printf("| Domain Name:         %-47s |\n", $response->{"domain"}->{"name"});
    printf("| Domain Organization: %-47s |\n", $response->{"domain"}->{"organization"});
    printf("+----------------------------------------------------------------------+\n");
}

#
# @param $user_id user-id of the user
# @return user info formatted to string for user with given $user_id
#
sub format_user
{
    my ($self, $user_id, $long) = @_;

    if ( !defined($user_id) ) {
        return undef;
    }

    my $user_information = $self->{'client'}->get_user_information($user_id);
    my $name = '<not-exists>';
    if ( defined($user_information) ) {
        $name = $user_information->{'firstName'};
        if ( defined($user_information->{'lastName'}) ) {
            $name .= ' ' . $user_information->{'lastName'};
        }
    }
    if ( $long ) {
        return "$name (id: $user_id)";
    }
    else {
        return "$name ($user_id)";
    }
}

#
# @param $user_id
# return true if user exists, false otherwise
#
sub user_exists
{
    my ($self, $user_id) = @_;
    my $user_information = $self->{'client'}->get_user_information($user_id);
    return defined($user_information);
}

1;