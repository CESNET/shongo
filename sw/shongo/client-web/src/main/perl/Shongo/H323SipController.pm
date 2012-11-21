#
# Alias for video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::H323SipController;
use base qw(Shongo::Web::Controller);

use strict;
use warnings;
use Shongo::Common;

#
# Create a new instance of object.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new(@_);
    bless $self, $class;

    return $self;
}

sub index_action
{
    print "TODO: H.323";
}

sub list_action
{
    my ($self) = @_;
    $self->{application}->render_page('List of existing reservation requests', 'h323-sip/reservation-request-list.html');
}

sub create_action
{
    my ($self) = @_;
    $self->{application}->render_page('New reservation request', 'h323-sip/reservation-request-create.html');
}

sub detail_action
{
    my ($self) = @_;
    $self->{application}->render_page('Detail of reservation request', 'h323-sip/reservation-request-detail.html');
}

1;