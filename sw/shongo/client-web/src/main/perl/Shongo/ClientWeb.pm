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
use Shongo::ClientWeb::H323SipController;
use Shongo::ClientWeb::AdobeConnectController;

# Get directory
use File::Spec::Functions;
use File::Basename;
my $directory = File::Spec::Functions::rel2abs(File::Basename::dirname($0));

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

    # We must reuse existing authorization state (because the case when the user click on "Sign in" then he go back
    # and again click on "Sing in" and then login, the authorization server returns the first "state" so it must be same)
    my $state = $self->{'session'}->param('authorization_state');
    $self->{'authorization'} = Shongo::ClientWeb::WebAuthorization->new($state);

    $self->add_action('index', sub { $self->index_action(); });
    $self->add_action('sign-in', sub { $self->sign_in_action(); });
    $self->add_action('sign-out', sub { $self->sign_out_action(); });
    $self->add_controller(Shongo::ClientWeb::H323SipController->new($self));
    $self->add_controller(Shongo::ClientWeb::AdobeConnectController->new($self));

    # Load resources
    my $resources = {};
    open my $in, $directory . "/../resources/text.properties" or die $!;
    while(<$in>) {
        while ( m/(\S+)=(.+)/g ) {
            my @name_parts = split('\.', $1);
            my $name_parts_count = scalar(@name_parts) - 1;
            my $index = 0;
            my $current_key = 'resources';
            my $current_ref = $self->{'template-parameters'};
            foreach my $key (@name_parts) {
                if ( !defined($current_ref->{$current_key}) ) {
                    $current_ref->{$current_key} = {};
                }
                $current_ref = $current_ref->{$current_key};
                $current_key = $key;
            }
            $current_ref->{$current_key} = $2;
        }
    }
    close $in;

    # Setup template utility functions
    $self->{'template-parameters'}->{'util'}->{'tooltip'} = sub {
        my ($id, $text) = @_;
        return "\$('#$id').tooltip({'title': '$text', 'placement': 'right', 'trigger':'focus'});";
    };

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
    if ( defined($configuration->{'authorization'}) ) {
        my $authorization = $configuration->{'authorization'};
        if ( defined($authorization->{'client-id'}) ) {
            $self->{'authorization'}->set_client_id($authorization->{'client-id'});
        }
        if ( defined($authorization->{'redirect-uri'}) ) {
            $self->{'authorization'}->set_redirect_uri($authorization->{'redirect-uri'});
        }
    }
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
        if ( $response =~ /(Connection refused)/ ) {
            $self->not_available_action();
        }
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
    my $user = $self->get_user();
    if ( !defined($user) || !defined($user->{'access_token'}) ) {
        $self->redirect('/sign-in');
        return undef;
    }
    my $securityToken = RPC::XML::string->new($user->{'access_token'});
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

    # If 'code' and 'state' parameters are present, retrieve access token
    my $code = $self->{'cgi'}->param('code');
    my $state = $self->{'cgi'}->param('state');
    if ( defined($code) && defined($state) ) {
        # Check state
        my $session_state = $self->{'session'}->param('authorization_state');
        if ( !defined($session_state) || $state ne $session_state ) {
            # Clear the state because we want to show it
            $self->{'session'}->clear(['authorization_state']);
            $self->error_action("Parameter 'state' has wrong value ($state != $session_state)!");
        }
        # Clear state in session (no longer needed)
        $self->{'session'}->clear(['authorization_state']);

        # Get access token
        my $access_token = $self->{'authorization'}->authentication_token($code);

        # Set user to session
        my $user_info = $self->{'authorization'}->get_user_info($access_token);
        $self->{'session'}->param('user', {
            'access_token' => $access_token,
            'id' => $user_info->{'id'},
            'original_id' => $user_info->{'original_id'},
            'name' => $user_info->{'name'}
        });

        # Redirect to previous page
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
    $self->render_page(undef, 'index.html');
}

#
# Controller offline action
#
sub not_available_action
{
    my ($self) = @_;
    select STDOUT;
    $self->render_headers();
    $self->render_page(undef, 'not-available.html');
    exit(0);
}

#
# @return current signed user
#
sub get_user
{
    my ($self) = @_;
    return $self->{'session'}->param('user');
}

#
# Sign-in action handler
#
sub sign_in_action
{
    my ($self) = @_;
    $self->{'session'}->param('authorization_state', $self->{'authorization'}->get_state());
    $self->{'authorization'}->authentication_authorize();
}

#
# Sign-out action handler
#
sub sign_out_action
{
    my ($self) = @_;
    $self->{'session'}->clear(['user']);
    $self->redirect('/');
}

1;