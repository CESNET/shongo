#
# Web client application.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb;
use base qw(Shongo::Web::Application);

use strict;
use warnings;
use RPC::XML;
use RPC::XML::Client;
use Shongo::Common;
use Shongo::ClientWeb::WebAuthorization;

#
# Single instance of ClientWeb class.
#
my $singleInstance;

#
# Create a new instance of application.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = Shongo::Web::Application->new(@_);
    bless $self, $class;

    if ( defined($singleInstance) ) {
        die("ClientWeb can be instantiated only once.");
    }
    $singleInstance = $self;

    $self->{'authorization'} = Shongo::ClientWeb::WebAuthorization->new();
    $self->add_action('index', sub { $self->index_action(); });
    $self->add_action('sign-in', sub { $self->sign_in_action(); });
    $self->add_action('sign-out', sub { $self->sign_out_action(); });
    $self->add_controller(Shongo::H323SipController->new($self));
    $self->add_controller(Shongo::AdobeConnectController->new($self));

    return $self;
}

#
# @return single instance of the ClientWeb
# @static
#
sub instance
{
    if ( !defined($singleInstance) ) {
        die("ClientWeb hasn't been instantiated yet.");
    }
    return $singleInstance;
}

#
# Connect to to url
#
# @param controller_url
#
sub load_configuration
{
    my ($self, $configuration) = @_;
    $self->{'controller-url'} = $configuration->{'controller'};
}

#
# Connect to to url
#
# @param controller_url
#
sub check_connected
{
    my ($self) = @_;
    if ( defined($self->{'controller-client'}) ) {
        return;
    }

    if ( !defined($self->{'controller-url'}) ) {
        $self->error_action("Controller url isn't specified.");
    }
    $self->{'controller-client'} = RPC::XML::Client->new($self->{'controller-url'});
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
    $self->check_connected();

    my $response = $self->{'controller-client'}->send_request($method, @args);
    if ( !ref($response) ) {
        $self->error_action("Failed to send request to controller!\n" . $response);
        return undef;
    }
    if ( $response->is_fault() ) {
        my $message = $response->string();
        if ( $message =~ /<message>(.*)<\/message>/ ) {
            $message = $1;
        }
        $self->error_action(sprintf("Server failed to perform request!\nFault %d: %s",
            $response->code, $message));
    }
    return $response->value();
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
    my $securityToken = RPC::XML::string->new('1e3f174ceaa8e515721b989b19f71727060d0839');
    return $self->request(
        $method,
        $securityToken,
        @args
    );
}

# @Override
sub run
{
    my ($self, $location) = @_;

    my $code = $self->{'cgi'}->param('code');
    my $state = $self->{'cgi'}->param('state');
    if ( defined($code) && defined($state) ) {
        my $session_state = $self->{'session'}->param('authorization.state');
        if ( !defined($session_state) || $state ne $session_state ) {
            $self->error_action("Parameter 'state' has wrong value!");
        }
        $self->{'session'}->clear(['authorization.state']);
        my $access_token = $self->{'authorization'}->authentication_token($code);
        my $user_info = $self->{'authorization'}->user_info($access_token);
        $self->{'session'}->param('user', {
            'access_token' => $access_token,
            'id' => $user_info->{'id'},
            'name' => $user_info->{'name'}
        });
        $self->redirect();
        return;
    }

    $self->SUPER::run($location);
}

#
# Main action handler
#
sub index_action
{
    my ($self) = @_;
    $self->render_page('Shongo', 'index.html');
}

#
# Sign-in action handler
#
sub sign_in_action
{
    my ($self) = @_;
    $self->{'session'}->param('authorization.state', $self->{'authorization'}->get_state());
    $self->{'authorization'}->authentication_authorize();
}

#
# Sign-out action handler
#
sub sign_out_action
{
    my ($self) = @_;
    $self->{'session'}->clear(['user']);
    $self->redirect();
}

1;