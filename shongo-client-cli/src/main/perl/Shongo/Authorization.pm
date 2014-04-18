#
# Authorization and authentication.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Authorization;

use strict;
use warnings;

use IO::Socket::SSL 1.56; # At leaset version 1.56 because of SNI support (see http://pkgs.fedoraproject.org/cgit/perl-IO-Socket-SSL.git/plain/perl-IO-Socket-SSL.spec?p=perl-IO-Socket-SSL.git;a=blob_plain;f=perl-IO-Socket-SSL.spec)
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
    my ($state, $client_id, $redirect_uri, $secret) = @_;
    my $self = {};
    bless $self, $class;

    if ( !defined($state) ) {
        # Generate random state identifier (against XSRF attacks)
        $state = join "", map { unpack "H*", chr(rand(256)) } 1..10;
    }

    $self->{'state'} = $state;
    $self->{'url'} = '';
    $self->{'client_id'} = '';
    $self->{'client_secret'} = '';
    $self->{'redirect_uri'} = '';

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
# @param $url
#
sub set_url()
{
    my ($self, $url) = @_;
    $self->{'url'} = $url;
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
# @param $client_id
#
sub set_client_id()
{
    my ($self, $client_id) = @_;
    $self->{'client_id'} = $client_id;
}

#
# @return $client_secret
#
sub get_client_secret()
{
    my ($self) = @_;
    return $self->{'client_secret'};
}

#
# @param $secret
#
sub set_client_secret()
{
    my ($self, $secret) = @_;
    $self->{'client_secret'} = $secret;
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
# @param $redirect_uri
#
sub set_redirect_uri()
{
    my ($self, $redirect_uri) = @_;
    $self->{'redirect_uri'} = $redirect_uri;
}

#
# @return user agent
#
sub get_user_agent()
{
    my ($self) = @_;
    my $user_agent = LWP::UserAgent->new(ssl_opts => { verify_hostname => 1 });
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
    my $url = URI->new($self->{'url'} . '/authn/oic/authorize');
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
    my $url = URI->new($self->{'url'} . '/authn/oic/authorize');
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
    my $request = HTTP::Request->new(POST => $self->get_url() . '/authn/oic/token');
    $request->content_type('application/x-www-form-urlencoded');
    if ( defined($self->{'client_secret'}) ) {
        my $secret_string = $self->{'client_secret'};
        $request->header('Authorization' => "secret auth=$secret_string");
    }
    $request->content($self->escape_hash(
        'client_id' => $self->get_client_id(),
        'redirect_uri' => $self->get_redirect_uri(),
        'grant_type' => 'authorization_code',
        'code' => $authorization_code,
    ));
    my $user_agent = $self->get_user_agent();
    my $response = $user_agent->simple_request($request);
    if (!$response->is_success) {
        if ( $response->content =~ /^[{]/) {
            my $response_data = decode_json($response->content);
            $self->error("$response_data->{'error'}. $response_data->{'error_description'}\n"
                . "Retrieving access token failed!");
        } else {
            $self->error($response->content);
        };
        return;
    }
    my $response_data = decode_json($response->content);
    my $access_token = $response_data->{'access_token'};
    return $access_token;
}

#
# Retrieve user information by $access_token
#
# @param $access_token
# @return user information
#
sub get_user_information
{
    my ($self, $access_token) = @_;
    if (!defined($access_token)) {
        $self->error("Access token must be passed.");
        return;
    }

    # Setup request url
    my $url = URI->new($self->{'url'} . '/authn/oic/userinfo');

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