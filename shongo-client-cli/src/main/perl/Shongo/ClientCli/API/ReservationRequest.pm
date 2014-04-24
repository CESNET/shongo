#
# Reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ReservationRequest;
use base qw(Shongo::ClientCli::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Specification;

# Enumeration of state
our $State = ordered_hash(
    'NOT_ALLOCATED' => {'title' => 'Not Allocated', 'color' => 'yellow'},
    'ALLOCATED' => {'title' => 'Allocated', 'color' => 'green'},
    'ALLOCATION_FAILED' => {'title' => 'Allocation Failed', 'color' => 'red'},
    'NOT_STARTED' => {'title' => 'Not Started', 'color' => 'yellow'},
    'STARTED' => {'title' => 'Started', 'color' => 'green'},
    'STARTING_FAILED' => {'title' => 'Starting Failed', 'color' => 'red'},
    'STOPPED' => {'title' => 'Finished', 'color' => 'blue'},
    'STOPPING_FAILED' => {'title' => 'Stopping Failed', 'color' => 'red'},
);

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::ReservationRequestAbstract->new(@_);
    bless $self, $class;

    $self->set_object_class('ReservationRequest');
    $self->set_object_name('Reservation Request');

    $self->add_attribute('slot', {
        'title' => 'Requested Slot',
        'type' => 'interval',
        'complex' => 1,
        'required' => 1
    });
    $self->add_attribute('reservationId', {
        'title' => 'Reservation',
        'editable' => 0
    });
    $self->add_attribute('allocationState', {
        'title' =>'Current State',
        'format' => sub {
            my $state = '[' . format_state($self->{'allocationState'}) . ']';
            if ( defined($state) ) {
                if ( defined($self->get('allocationState')) && $self->get('allocationState') eq 'ALLOCATED' ) {
                    my $reservations = '';
                    foreach my $reservation_id (@{$self->{'reservationIds'}}) {
                        if ( length($reservations) > 0 ) {
                            $reservations .= ', ';
                        }
                        $reservations .= $reservation_id;
                    }
                    $state .= sprintf(" (" . colored("reservations", $Shongo::ClientCli::API::Object::COLOR) . ": %s)", $reservations);
                }
                my $color = 'blue';
                if ( defined($self->get('allocationState')) && $self->get('allocationState') eq 'ALLOCATION_FAILED' ) {
                    $color = 'red';
                }
                my $state_report = $self->{'allocationStateReport'};
                $state_report = format_report($state_report, get_term_width() - 23);
                $state .= "\n" . colored($state_report, $color);
                return $state;
            }
            return undef;
        },
        'read-only' => 1
    });

    return $self;
}

#
# @return state
#
sub format_state
{
    my ($state) = @_;
    if ( !defined($state) ) {
        return undef;
    }
    my $output = $State->{$state}->{'title'};
    $output = colored($output, $State->{$state}->{'color'});
    return $output;
}

1;