#
# Controller for H.323/SIP video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::H323SipController;
use base qw(Shongo::Web::Controller);

use strict;
use warnings;
use Shongo::Common;

#
# Create a new instance of controller.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new('h323-sip', @_);
    bless $self, $class;

    return $self;
}

sub index_action
{
    print "TODO: H.323";
}

my $Purpose = {
    'SCIENCE' => 'Science',
    'EDUCATION' => 'Education'
};

sub process_request
{
    my ($request) = @_;
    $request->{'purpose'} = $Purpose->{$request->{'purpose'}};
}

sub list_action
{
    my ($self) = @_;
    my $requests = $self->{'application'}->secure_request('Reservation.listReservationRequests');
    foreach my $request (@{$requests}) {
        process_request($request);
    }
    $self->render_page('List of existing reservation requests', 'h323-sip/list.html', {
        'requests' => $requests
    });
}

sub create_action
{
    my ($self) = @_;
    $self->render_page('New reservation request', 'h323-sip/create.html');
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->{'application'}->secure_request('Reservation.getReservationRequest', $id);
    process_request($request);
    $self->render_page('Detail of reservation request', 'h323-sip/detail.html', {
        'request' => $request
    });
}

1;