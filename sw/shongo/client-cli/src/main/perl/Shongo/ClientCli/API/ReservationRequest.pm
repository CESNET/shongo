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
    'NOT_COMPLETE' => {'title' => 'Not Complete', 'color' => 'yellow'},
    'NOT_ALLOCATED' => {'title' => 'Not Allocated', 'color' => 'yellow'},
    'ALLOCATED' => {'title' => 'Allocated', 'color' => 'green'},
    'ALLOCATION_FAILED' => {'title' => 'Allocation Failed', 'color' => 'red'},
    'STARTED' => {'title' => 'Started', 'color' => 'green'},
    'STARTING_FAILED' => {'title' => 'Starting Failed', 'color' => 'red'},
    'FINISHED' => {'title' => 'Finished', 'color' => 'blue'},
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
    $self->add_attribute('state', {
        'title' =>'Current State',
        'format' => sub {
            my $state = $self->get_state();
            if ( defined($state) ) {
                if ( defined($self->get('state')) && $self->get('state') eq 'ALLOCATED' ) {
                    $state .= sprintf(" (" . colored("reservation", $Shongo::ClientCli::API::Object::COLOR) . ": %s)", $self->{'reservationId'});
                }
                my $color = 'blue';
                if ( defined($self->get('state')) && $self->get('state') eq 'ALLOCATION_FAILED' ) {
                    $color = 'red';
                }
                my $state_report = $self->{'stateReport'};
                $state_report = format_report($state_report, get_term_width() - 23);
                $state .= "\n" . colored($state_report, $color);
                return $state;
            }
            return undef;
        },
        'read-only' => 1
    });
    $self->add_attribute_preserve('reservationId');
    $self->add_attribute_preserve('stateReport');

    return $self;
}

#
# @return state
#
sub get_state
{
    my ($self) = @_;
    if ( !defined($self->{'state'}) ) {
        return undef;
    }
    my $state = $State->{$self->{'state'}}->{'title'};
    $state = colored($state, $State->{$self->{'state'}}->{'color'});
    return '[' . $state . ']';
}

1;