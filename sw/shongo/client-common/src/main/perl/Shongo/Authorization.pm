#
# Authorization and authentication.
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
use JSON;
use Shongo::Common;

#
# Create a new instance of authorization.
#
# @static
#
sub new
{
    my $class = shift;
    my ($client_id, $redirect_uri, $state) = @_;
    my $self = {};
    bless $self, $class;

    if ( !defined($client_id) || !defined($redirect_uri) ) {
        die("Arguments 'client_id' and 'redirect_uri' must be passed.");
    }
    if ( !defined($state) ) {
        # Generate random state identifier (against XSRF attacks)
        $state = join "", map { unpack "H*", chr(rand(256)) } 1..10;
    }

    $self->{'url'} = 'https://hroch.cesnet.cz/phpid-server/oic/';
    $self->{'client_id'} = $client_id;
    $self->{'redirect_uri'} = $redirect_uri;
    $self->{'state'} = $state;

    return $self;
}

#
# @return authorization server url
#
sub get_url()
{
    my ($self) = @_;
    return $self->{'url'};
}

#
# @return client_id
#
sub get_client_id()
{
    my ($self) = @_;
    return $self->{'client_id'};
}

#
# @return redirect_uri
#
sub get_redirect_uri()
{
    my ($self) = @_;
    return $self->{'redirect_uri'};
}

#
# @return user agent
#
sub get_user_agent()
{
    my ($self) = @_;
    my $user_agent = LWP::UserAgent->new();
    $user_agent->agent('Shongo Client');
    return $user_agent;
}

#
# @return state variable
#
sub get_state()
{
    my ($self) = @_;
    $self->{'state'};
}

#
# @return url for authentication
#
sub get_authorize_url()
{
    my ($self) = @_;
    my $url = URI->new($self->{'url'} . 'authorize');
    $url->query_form(
        'client_id'     => $self->{'client_id'},
        'redirect_uri'  => $self->{'redirect_uri'},
        'scope'         => 'openid',
        'response_type' => 'code',
        'state'         => $self->{'state'},
        'prompt'        => 'login'
    );
    return $url->as_string();
}

#
# @return url for authentication
#
sub get_token_url()
{
    my ($self) = @_;
    my $url = URI->new($self->{'url'} . 'authorize');
    $url->query_form(
        'client_id'     => $self->{'client_id'},
        'redirect_uri'  => $self->{'redirect_uri'},
        'scope'         => 'openid',
        'response_type' => 'code',
        'state'         => $self->{'state'},
        'prompt'        => 'login'
    );
    return $url->as_string();
}

#
# Print error
#
# @error
#
sub error
{
    my ($self, $error) = @_;
    die($error);
}

#
# Authorize.
#
# @return authorization code or in case of web application perform redirect
#
sub authentication_authorize
{
    die("This method must be overriden by the implementation!");
}

#
# Retrieve access token from $authorization_code
#
# @param $authorization_code
# @return access_token
#
sub authentication_token
{
    my ($self, $authorization_code) = @_;
    my $request = HTTP::Request->new(POST => $self->get_url() . 'token');
    $request->content_type('application/x-www-form-urlencoded');
    $request->content($self->escape_hash(
        'client_id' => $self->get_client_id(),
        'redirect_uri' => $self->get_redirect_uri(),
        'grant_type' => 'authorization_code',
        'code' => $authorization_code,
    ));
    my $user_agent = $self->get_user_agent();
    my $response = $user_agent->simple_request($request);
    my $response_data = decode_json($response->content);
    if (!$response->is_success) {
        error("$response_data->{'error'}. $response_data->{'error_description'}\n"
            . "Retrieving access token failed!");
        return;
    }
    my $access_token = $response_data->{'access_token'};
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
    my ($self, $access_token) = @_;
    if (!defined($access_token)) {
        self->error("Access token must be passed.");
        return;
    }

    # Setup request url
    my $url = URI->new($self->{'url'} . 'userinfo');
    $url->query_form('schema' => 'openid');

    # Request user information
    my $request = HTTP::Request->new(GET => $url);
    $request->header('Authorization' => "Bearer $access_token");
    my $user_agent = $self->get_user_agent();
    my $response = $user_agent->simple_request($request);
    my $response_data = decode_json($response->content);
    if (!$response->is_success) {
        $self->error("Error: $response_data->{'error'}. $response_data->{'error_description'}\n"
            . "Retrieving user information failed!");
        return undef;
    }

    $response_data->{'name'} = $response_data->{'given_name'} . ' ' . $response_data->{'family_name'};
    return $response_data;
}

#
# Convert hash to escaped string
#
# @param %hash
# @return hash converted to string and all values are uri_escaped
#
sub escape_hash
{
    my ($self, %hash) = @_;
    my @pairs;
    for my $key (keys %hash) {
        push @pairs, join "=", map { uri_escape($_) } $key, $hash{$key};
    }
    return join "&", @pairs;
}

1;