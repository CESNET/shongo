#
# Common controller for all video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb::CommonController;
use base qw(Shongo::Web::Controller);

use strict;
use warnings;
use Shongo::Common;

our $ReservationRequestPurpose = {
    'SCIENCE' => 'Science',
    'EDUCATION' => 'Education'
};

our $ReservationRequestState = {
    'NOT_ALLOCATED' => 'not allocated',
    'NOT_COMPLETE' => 'not allocated',
    'COMPLETE' => 'not allocated',
    'ALLOCATED' => 'allocated',
    'ALLOCATION_FAILED' => 'allocation failed'
};

sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new(@_);
    bless $self, $class;

    return $self;
}

# @Override
sub pre_dispatch
{
    my ($self, $action) = @_;
    my $application = Shongo::ClientWeb->instance();
    if ( !defined($application->get_user()) ) {
        $application->redirect('/sign-in');
        return 0;
    }
    return $self->SUPER::pre_dispatch($action);
}

sub index_action
{
    my ($self) = @_;
    $self->redirect('list');
}

sub list_reservation_requests
{
    my ($self, $title, $technologies) = @_;
    my $requests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies
    });
    foreach my $request (@{$requests}) {
        my $state_class = lc($request->{'state'});
        $state_class =~ s/_/-/g;
        $request->{'purpose'} = $ReservationRequestPurpose->{$request->{'purpose'}};
        $request->{'stateClass'} = $state_class;
        $request->{'state'} = $ReservationRequestState->{$request->{'state'}};
        if ( $request->{'earliestSlot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = format_datetime($1);
            $request->{'duration'} = format_period($2);
        }
    }
    $self->render_page($title, 'common/list.html', {
        'requests' => $requests
    });
}

1;