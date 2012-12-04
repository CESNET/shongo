#
# Authorization and authentication functions.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::CliAuthorization;
use base qw(Shongo::Authorization);

use strict;
use warnings;

use LWP;
use LWP::Protocol::https;
use URI;
use JSON;
use URI::QueryParam;
use MIME::Base64;
use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of CLI authorization.
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Authorization->new(
        'cz.cesnet.shongo.client-cli',
        'https://client-cli.shongo.cesnet.cz/'
    );
    bless $self, $class;

    return $self;
}

# @Override
sub error
{
    my ($self, $error) = @_;
    console_print_error($error);
}

#
# Performs authorization
#
# @return access token
#
sub authentication_authorize
{
    my ($self) = @_;
    my $username = undef;
    my $password = undef;

    $username = console_read_value('Username', 1);
    $password = console_read_password('Password');
    if ( !defined($password) ) {
        $password = '';
    }

    # Create user agent
    my $user_agent = $self->get_user_agent();
    $user_agent->cookie_jar({});

    # Start authorization
    my $url = $self->get_authorize_url();
    my $url_to_print = $url;
    $url_to_print =~ s/\?.*//g;
    console_print_debug("Connecting to '%s' for authorization...", $url_to_print);
    my $request = HTTP::Request->new(GET => $url);
    my $response = $user_agent->simple_request($request);

    # Prepare Basic authorization code
    my $authorization = $username . ':' . $password;
    $authorization = encode_base64($authorization);

    # Authorization redirect loop
    my $redirect_uri = $self->get_redirect_uri();
    my $authorization_code = undef;
    while (defined($response) && $response->is_redirect) {
        $url = $response->header('location');
        if (!defined($url)) {
            console_print_error("Missing location for redirection: " . $response->as_string);
            $response = undef;
        }
        elsif ($url =~ /^$redirect_uri/) {
            # Check for error
            my $pound = '#';
            if ( $url =~ /${pound}error=/ ) {
                $url =~ s/${pound}error=/\?error=/g;
                $url = URI->new($url);
                console_print_error("Retrieving authorization code failed! " . $url->query_param('error') . ": "
                    . $url->query_param('error_description'));
                return;
            }

            # Process response
            $url = URI->new($url);
            my $code = $url->query_param('code');
            my $newState = $url->query_param('state');
            my $state = $self->get_state();
            if ( !defined($newState) ) {
                $newState = '';
            }
            if (!($newState eq $state)) {
                console_print_error("Failed to verify authorization state ('$newState' != '$state'), XSRF attack?");
            }
            $authorization_code = $code;
            $response = undef;
        }
        else {
            console_print_debug("Redirecting to '$url'...");
            my $request = HTTP::Request->new(GET => $url);
            $request->header('Authorization' => 'Basic ' . $authorization);
            $response = $user_agent->simple_request($request);
        }
    }
    if (!defined($authorization_code)) {
        my $error = 'No response returned!';
        if ( defined($response) ) {
            $error = $response->content;
        }
        console_print_error("Retrieving authorization code failed! " . $error);
        return;
    }
    console_print_debug("Authorization code: $authorization_code");

    # Retrieve access token
    console_print_debug("Retrieving access token for authorization code '$authorization_code'...");
    my $access_token = $self->authentication_token($authorization_code);
    console_print_debug("Access token: $access_token");

    return $access_token;
}

1;