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

    $self->{'class'} = 'ReservationRequest';

    return $self;
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    $self->SUPER::on_create($attributes);

    $self->modify_slot();
    $self->modify_specification();
}

# @Override
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    push(@{$actions}, (
        'Modify requested slot' => sub {
            $self->modify_slot();
            return undef;
        },
        'Modify specification' => sub {
            $self->modify_specification();
            return undef;
        }
    ));

    return $self->SUPER::on_modify_loop($actions);
}

#
# Modify slot
#
sub modify_slot()
{
    my ($self) = @_;
    my $start = undef;
    my $duration = undef;
    if ( defined($self->{'slot'}) && $self->{'slot'} =~ m/(.*)\/(.*)/ ) {
        $start = $1;
        $duration = $2;
    }
    $start = console_edit_value("Type a date/time", 1, $Shongo::Common::DateTimePattern, $start);
    $duration = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $duration);
    $self->{'slot'} = $start . '/' . $duration;
}

#
# Modify specification
#
sub modify_specification()
{
    my ($self) = @_;
    my $specification = undef;
    if ( defined($self->{'specification'}) ) {
        $specification = $self->{'specification'}->{'class'};
    }
    $specification = Shongo::Controller::API::Specification::select_type($specification);
    if ( !defined($self->{'specification'}) || !($specification eq $self->{'specification'}->{'class'}) ) {
        $self->{'specification'} = Shongo::Controller::API::Specification->create($specification);
    } else {
        $self->{'specification'}->modify();
    }
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'specification' ) {
        return Shongo::Controller::API::Specification->new($class);
    }
    return $self->SUPER::create_value_instance($class, $attribute);
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

#
# @return report
#
sub get_state_report
{
    my ($self) = @_;
    my $color = 'blue';
    if ( defined($self->{'state'}) && $self->{'state'} eq 'ALLOCATION_FAILED' ) {
        $color = 'red';
    }
    my $state_report = $self->{'stateReport'};
    $state_report = format_report($state_report, get_term_width() - 23);
    return colored($state_report, $color);
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Reservation Request";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Requested Slot', format_interval($self->{'slot'}));
    $attributes->{'add'}('Specification', $self->{'specification'});

    my $state = $self->get_state();
    if ( defined($self->{'state'}) && $self->{'state'} eq 'ALLOCATED' ) {
        $state .= sprintf(" (" . colored("reservation", $Shongo::Controller::API::ObjectOld::COLOR) . ": %s)", $self->{'reservationIdentifier'});
    }
    $attributes->{'add'}('Current State', $state, $self->get_state_report());
}

1;