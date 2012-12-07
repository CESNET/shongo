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
    my $self = Shongo::Authorization->new(
        'cz.cesnet.shongo.client-web-local',
        'http://127.0.0.1:8182/',
        $state
    );
    bless $self, $class;

    return $self;
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