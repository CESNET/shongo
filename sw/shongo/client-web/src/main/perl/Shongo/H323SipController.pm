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

my $ReservationRequestPurpose = {
    'SCIENCE' => 'Science',
    'EDUCATION' => 'Education'
};

sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new('h323-sip', @_);
    bless $self, $class;

    return $self;
}

sub index_action
{
    my ($self) = @_;
    $self->redirect('list');
}

sub list_action
{
    my ($self) = @_;
    my $requests = $self->{'application'}->secure_request('Reservation.listReservationRequests');
    foreach my $request (@{$requests}) {
        $request->{'purpose'} = $ReservationRequestPurpose->{$request->{'purpose'}};
        if ( $request->{'earliestSlot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = format_datetime($1);
            $request->{'duration'} = format_period($2);
        }
    }
    $self->render_page('List of existing H323/SIP reservation requests', 'h323-sip/list.html', {
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
    $request->{'purpose'} = $ReservationRequestPurpose->{$request->{'purpose'}};
    $self->render_page('Detail of existing H323/SIP reservation request', 'h323-sip/detail.html', {
        'request' => $request
    });
}

sub delete_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $confirmed = $self->get_param('confirmed');
    if ( defined($confirmed) ) {
        my $request = $self->{'application'}->secure_request('Reservation.deleteReservationRequest', $id);
        $self->redirect('list');
    }
    else {
        $self->render_page('Delete existing H323/SIP reservation request', 'h323-sip/delete.html', {
            'id' => $id
        });
    }
}

1;