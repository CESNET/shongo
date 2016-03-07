#
# Common controller client.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCommon;

use strict;
use warnings;

use Shongo::Common;
use RPC::XML;
use RPC::XML::Client;
use XML::Twig;
use IO::Socket::SSL qw(debug1);

#
# Create a new instance of common client.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = {};
    bless $self, $class;

    $self->{'access-token'} = undef;
    $self->{'controller-client'} = undef;
    $self->{'on-error'} = sub {
        my ($error) = @_;
        die('on-error not overridden.');
    };
    $self->{'on-fault'} = sub {
        my ($error) = @_;
        die('on-fault not overridden.');
    };
    $self->{'on-get-access-token'} = sub {
        die('on-get-access-token not overridden.');
    };
    $self->{'user-cache'} = {};
    $self->{'user-cache-get'} = sub {
        my ($user_id) = @_;
        return $self->{'user-cache'}->{$user_id};
    };
    $self->{'user-cache-put'} = sub {
        my ($user_id, $user_information) = @_;
        $self->{'user-cache'}->{$user_id} = $user_information;
    };
    $self->{'group-cache'} = {};

    # Set RPC::XML encoding
    $RPC::XML::ENCODING = 'utf-8';

    return $self;
}

#
# @param $access_token sets current user access token
#
sub set_access_token()
{
    my ($self, $access_token) = @_;
    $self->{'access-token'} = $access_token;
}

#
# @return current user access token
#
sub get_access_token()
{
    my ($self) = @_;
    return $self->{'access-token'};
}

#
# @param $url
# @return updated $url
#
sub update_url()
{
    my ($self, $url, $ssl) = @_;

    # Append default port if not presented
    if ( !($url =~ /.+:[0-9]+$/ ) ) {
        $url = $url . ":8181";
    }
    # Prepend http:// if not presented
    if ( !($url =~ /^http(s)?:\/\// ) ) {
        $url = 'http://' . $url;
    }
    # Replace http by https if $ssl
    if ( $ssl && $url =~ /^http:\/\// ) {
        $url =~ s/^http/https/g;
    }
    return $url;
}

#
# @return controller url
#
sub get_url()
{
    my ($self) = @_;
    return $self->{'controller-url'};
}

#
# Connect to Controller XML-RPC server.
#
# @param url
#
sub connect()
{
    my ($self, $url, $ssl_unverified) = @_;

    my $ssl_opts;
    if ($ssl_unverified) {
        $ssl_opts = {
            SSL_use_cert => 0,
            SSL_verify_mode => SSL_VERIFY_NONE,
        };
    }
    else {
        $ssl_opts = {
            SSL_verify_mode => SSL_VERIFY_PEER
        };
    }
    $self->{'controller-url'} = $url;
    $self->{'controller-client'} = RPC::XML::Client->new($url,
        useragent => [
            ssl_opts => $ssl_opts,
        ]
    );
}

#
# Disconnect from Controller XML-RPC server.
#
sub disconnect()
{
    my ($self) = @_;
    if ( !$self->check_connected() ) {
        return 0;
    }
    $self->{'controller-client'} = undef;
    return 1;
}

#
# Checks whether client is connected to controller
#
sub is_connected()
{
    my ($self) = @_;
    if ( !defined($self->{"controller-client"}) ) {
        return 0;
    }
    return 1;
}

#
# Connect to to url
#
# @param controller_url
#
sub check_connected
{
    my ($self) = @_;
    if ( $self->is_connected() ) {
        return 1;
    }
    $self->{'on-error'}('Client is not connected to any controller.');
    return 0;
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
        $self->{'on-error'}("Failed to send request to controller!\n" . $response);
        return undef;
    }
    if ( $response->is_fault() ) {
        if ( defined($self->{'on-fault'}) ) {
            $self->{'on-fault'}($response);
        }
        return undef;
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
    my $security_token = RPC::XML::struct->new();
    if ( defined($self->{'access-token'}) ) {
        $security_token = RPC::XML::string->new($self->{'access-token'});
    }
    else {
        $self->{'access-token'} = $self->{'on-get-access-token'}();
        if ( defined($self->{'access-token'}) ) {
            $security_token = RPC::XML::string->new($self->{'access-token'});
        }
    }
    return $self->request(
        $method,
        $security_token,
        @args
    );
}

#
# Send request to Controller XML-RPC server with security token attribute appended to $hash.
#
# @param method
# @param $hash parameter for XML-RPC request
# @return response
#
sub secure_hash_request()
{
    my ($self, $method, $hash) = @_;
    my $security_token = RPC::XML::struct->new();
    if ( defined($self->{'access-token'}) ) {
        $hash->{'securityToken'} = RPC::XML::string->new($self->{'access-token'});
    }
    else {
        $self->{'access-token'} = $self->{'on-get-access-token'}();
        if ( defined($self->{'access-token'}) ) {
            $hash->{'securityToken'} = RPC::XML::string->new($self->{'access-token'});
        }
    }
    return $self->request(
        $method,
        $hash
    );
}

#
# Retrieve user information by $user_id
#
# @param $user_id
# @return user information
#
sub get_user_information()
{
    my ($self, $user_id) = @_;
    my $user_information = $self->{'user-cache-get'}($user_id);
    if ( !defined($user_information) ) {
        my $on_fault = $self->{'on-fault'};
        $self->{'on-fault'} = undef;
        my $response = $self->secure_hash_request('Authorization.listUsers', {
            'userIds' => [RPC::XML::string->new($user_id)]
        });
        $self->{'on-fault'} = $on_fault;
        if ( defined($response) ) {
            $user_information = $response->{'items'}->[0];
            if ( defined($user_information) ) {
                $self->{'user-cache-put'}($user_id, $user_information);
            }
        }
        else {
            # Encode null as empty hash
            $user_information = {};
            $self->{'user-cache-put'}($user_id, $user_information);
            $user_information = undef;
        }
    }
    # Decode empty hash as null
    elsif ( !%{$user_information} ) {
        return undef;
    }
    return $user_information;
}

#
# Retrieve group by $group_id
#
# @param $group_id
# @return group
#
sub get_group()
{
    my ($self, $group_id) = @_;
    my $group = $self->{'group-cache'}->{$group_id};
    if ( !defined($group) ) {
        my $on_fault = $self->{'on-fault'};
        $self->{'on-fault'} = undef;
        my $response = $self->secure_hash_request('Authorization.listGroups', {
            'groupIds' => [RPC::XML::string->new($group_id)]
        });
        $self->{'on-fault'} = $on_fault;
        if ( defined($response) ) {
            $group = $response->{'items'}->[0];
            if ( defined($group) ) {
                $self->{'group-cache'}->{$group_id} = $group;
            }
        }
        else {
            # Encode null as empty hash
            $group = {};
            $self->{'group-cache'}->{$group_id} = $group;
            $group = undef;
        }
    }
    # Decode empty hash as null
    elsif ( !%{$group} ) {
        return undef;
    }
    return $group;
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