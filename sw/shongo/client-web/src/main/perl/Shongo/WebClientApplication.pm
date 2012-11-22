#
# Web client application.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::WebClientApplication;
use base qw(Shongo::Web::Application);

use strict;
use warnings;
use RPC::XML;
use RPC::XML::Client;
use Shongo::Common;

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

    return $self;
}

#
# Connect to to url
#
# @param controller_url
#
sub set_controller_url
{
    my ($self, $controller_url) = @_;
    $self->{'controller-url'} = $controller_url;
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

1;