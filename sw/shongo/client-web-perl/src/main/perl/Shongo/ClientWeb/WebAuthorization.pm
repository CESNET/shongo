#
# Authorization and authentication functions.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb::WebAuthorization;
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

#
# Create a new instance of CLI authorization.
#
# @static
#
sub new()
{
    my $class = shift;
    my ($state) = @_;
    my $self = Shongo::Authorization->new($state);
    bless $self, $class;

    return $self;
}

#
# Load configuration
#
# @param $configuration
#
sub load_configuration
{
    my ($self, $configuration) = @_;

    $configuration = $configuration->{'security'};

    # Setup authorization
    $self->set_url($configuration->{'server'});
    $self->set_client_id($configuration->{'client-id'});
    $self->set_redirect_uri($configuration->{'redirect-uri'});
    $self->set_secret($configuration->{'secret'});
}

# @Override
sub error
{
    my ($self, $error) = @_;
    my $application = Shongo::ClientWeb->instance();
    $application->error_action($error);
}

#
# Performs authorization
#
sub authentication_authorize
{
    my ($self) = @_;
    my $application = Shongo::ClientWeb->instance();
    $application->redirect($self->get_authorize_url(), undef, 1);
}

1;