#
# Reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::ReservationRequestNormal);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Specification;

# Enumeration of state
our $State = ordered_hash(
    'NOT_COMPLETE' => 'Not Complete',
    'COMPLETE' => 'Not Allocated',
    'ALLOCATED' => 'Allocated',
    'ALLOCATION_FAILED' => 'Allocation Failed'
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
    my $self = Shongo::Controller::API::ReservationRequestNormal->new(@_);
    bless $self, $class;

    $self->set_object_class('ReservationRequest');
    $self->set_object_name('Reservation Request');

    $self->add_attribute('slot', {
        'title' => 'Requested Slot',
        'type' => 'interval',
        'complex' => 1
    });
    $self->add_attribute('specification', {
        'complex' => 1,
        'modify' => sub {
            my ($specification) = @_;
            my $class = undef;
            if ( defined($specification) ) {
                $class = $specification->{'class'};
            }
            $class = Shongo::Controller::API::Specification::select_type($class);
            if ( !defined($specification) || !($class eq $specification->get_object_class()) ) {
                $specification = Shongo::Controller::API::Specification->new();
                $specification->create({'class' => $class});
            } else {
                $specification->modify(1);
            }
            return $specification;
        }
    });
    $self->add_attribute('state', {
        'title' =>'Current State',
        'format' => sub {
            my $state = $self->get_state();
            if ( defined($self->get('state')) && $self->get('state') eq 'ALLOCATED' ) {
                $state .= sprintf(" (" . colored("reservation", $Shongo::Controller::API::Object::COLOR) . ": %s)", $self->{'reservationIdentifier'});
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
    });
    $self->add_attribute_preserve('reservationIdentifier');
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
    my $state = $State->{$self->{'state'}};
    if ( $self->{'state'} eq 'NOT_COMPLETE' ) {
        $state = colored($state, 'yellow')
    }
    elsif ( $self->{'state'} eq 'ALLOCATED' ) {
        $state = colored($state, 'green')
    }
    elsif ( $self->{'state'} eq 'ALLOCATION_FAILED' ) {
        $state = colored($state, 'red')
    }
    else {
        $state = colored($state, 'blue');
    }
    return '[' . $state . ']';
}

1;