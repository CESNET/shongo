#
# Authorization and authentication functions.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Authorization;

use strict;
use warnings;

use LWP;
use LWP::UserAgent;
use URI;
use URI::Escape;
use URI::QueryParam;
use JSON;
use Shongo::Common;
use Shongo::Console;

# Dummy redirect url which is
my $CLIENT_USER_AGENT = 'Shongo Command-Line Client/1.0';
my $CLIENT_ID = 'test-console-client';
my $CLIENT_URL = 'https://dummy/';
my $AUTHENTICATION_SERVER = 'https://hroch.cesnet.cz/phpid-server/oic/';

#
# Convert has to escaped string
#
sub escape_hash
{
    my %hash = @_;
    my @pairs;
    for my $key (keys %hash) {
        push @pairs, join "=", map { uri_escape($_) } $key, $hash{$key};
    }
    return join "&", @pairs;
}

#
# Performs authorization
#
# @return access token
#
sub authorize
{
    # Generate random state identifier (against XSRF attacks)
    my $state = join "", map { unpack "H*", chr(rand(256)) } 1..10;

    # Create user agent
    my $user_agent = LWP::UserAgent->new();
    $user_agent->agent($CLIENT_USER_AGENT);
    $user_agent->cookie_jar({});

    # Start authorization
    my $url = URI->new($AUTHENTICATION_SERVER . 'authorize');
    console_print_debug("Connecting to '$url' for authorization...");
    $url->query_form(
        'client_id'     => $CLIENT_ID,
        'redirect_uri'  => $CLIENT_URL,
        'scope'         => 'openid',
        'response_type' => 'code',
        'state'         => $state,
        'prompt'        => 'login'
    );
    my $response = $user_agent->simple_request(HTTP::Request->new(GET => $url));

    # Authorization redirect loop
    my $authorization_code = undef;
    while (defined($response) && $response->is_redirect) {
        $url = $response->header('location');
        if (!defined($url)) {
            console_print_error("Missing location for redirection: " . $response->as_string);
            $response = undef;
        }
        elsif ($url =~ /^$CLIENT_URL/) {
            $url = URI->new($url);
            my $code = $url->query_param('code');
            my $newState = $url->query_param('state');
            if (!($newState eq $state)) {
                console_print_error("Failed to verify authorization state ($newState != $state), XSRF attack?");
            }
            $authorization_code = $code;
            $response = undef;
        }
        else {
            console_print_debug("Redirecting to '$url'...");
            $response = $user_agent->simple_request(HTTP::Request->new(GET => $url));
        }
    }
    if (!defined($authorization_code)) {
        console_print_error("Retrieving authorization code failed!");
        return;
    }
    console_print_debug("Authorization code: $authorization_code");

    # Retrieve console_print_debug token
    console_print_debug("Retrieving access token for authorization code '$authorization_code'...");
    my $request = HTTP::Request->new(POST => $AUTHENTICATION_SERVER . 'token');
    $request->content_type('application/x-www-form-urlencoded');
    $request->content(escape_hash(
        'client_id' => $CLIENT_ID,
        'redirect_uri' => $CLIENT_URL,
        'grant_type' => 'authorization_code',
        'code' => $authorization_code,
    ));
    $response = $user_agent->simple_request($request);
    my $response_data = decode_json($response->content);
    if (!$response->is_success) {
        console_print_error("Error: $response_data->{'error'}. $response_data->{'error_description'}");
        console_print_error("Retrieving access token failed!");
        return;
    }
    my $access_token = $response_data->{'access_token'};
    console_print_debug("Access token: $access_token");

    return $access_token;
}

#
# Retrieve user information
#
# @param $access_token
# @return user information
#
sub user_info
{
    my ($access_token) = @_;
    if (!defined($access_token)) {
        return;
    }

    # Create user agent
    my $user_agent = LWP::UserAgent->new();
    $user_agent->agent($CLIENT_USER_AGENT);

    # Setup request url
    my $url = URI->new($AUTHENTICATION_SERVER . 'userinfo');
    console_print_debug("Retrieving user information for access token '$access_token'...");
    $url->query_form('schema' => 'openid');

    # Request user information
    my $request = HTTP::Request->new(GET => $url);
    $request->header('Authorization' => "Bearer $access_token");
    my $response = $user_agent->simple_request($request);
    my $response_data = decode_json($response->content);
    if (!$response->is_success) {
        console_print_error("Error: $response_data->{'error'}. $response_data->{'error_description'}");
        console_print_error("Retrieving user information failed!");
        return undef;
    }
    return $response_data;
}

1;