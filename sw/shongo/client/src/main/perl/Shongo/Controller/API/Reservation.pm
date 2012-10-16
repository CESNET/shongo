#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Reservation;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Compartment;

#
# Reservation types
#
our $Type = ordered_hash(
    'ResourceReservation' => 'Resource Reservation',
    'VirtualRoomReservation' => 'Virtual Room Reservation',
    'AliasReservation' => 'Alias Reservation',
    'CompartmentReservation' => 'Compartment Reservation',
    'ExistingReservation' => 'Existing Reservation'
);

#
# Create a new instance of reservation
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# Fetch child reservations
#
sub fetch_child_reservations
{
    my ($self, $recursive) = @_;

    if ( defined($self->{'childReservationIdentifiers'}) && @{$self->{'childReservationIdentifiers'}} > 0) {
        $self->{'childReservations'} = [];
        foreach my $reservation (@{$self->{'childReservationIdentifiers'}}) {
            my $result = Shongo::Controller->instance()->secure_request(
                'Reservation.getReservation',
                RPC::XML::string->new($reservation)
            );
            if ( $result->is_fault ) {
                return;
            }
            my $reservationXml = $result->value();
            my $reservation = Shongo::Controller::API::Reservation->new($reservationXml->{'class'});
            $reservation->from_xml($reservationXml);
            if ( $recursive ) {
                $reservation->fetch_child_reservations($recursive);
            }
            push(@{$self->{'childReservations'}}, $reservation);
        }
    }
}

# @Override
sub get_name
{
    my ($self) = @_;
    if ( defined($self->{'class'}) && exists $Type->{$self->{'class'}} ) {
        return $Type->{$self->{'class'}};
    } else {
        return "Reservation";
    }
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Identifier', $self->{'identifier'});
    $attributes->{'add'}('Slot', format_interval($self->{'slot'}));

    switch ($self->{'class'}) {
        case 'ResourceReservation' {
            $attributes->{'add'}('Resource', sprintf("%s (%s)",
                $self->{'resourceName'},
                $self->{'resourceIdentifier'}
            ));
        }
        case 'VirtualRoomReservation' {
            $attributes->{'add'}('Resource', sprintf("%s (%s)",
                $self->{'resourceName'},
                $self->{'resourceIdentifier'}
            ));
            $attributes->{'add'}('Port Count', $self->{'portCount'});
        }
        case 'AliasReservation' {
            $attributes->{'add'}('Resource', sprintf("%s (%s)",
                $self->{'resourceName'},
                $self->{'resourceIdentifier'}
            ));
            $attributes->{'add'}('Alias', $self->{'alias'});
        }
        case 'CompartmentReservation' {
            my $compartment = Shongo::Controller::API::Compartment->new();
            $compartment->from_xml($self->{'compartment'});
            $attributes->{'add'}('Compartment', $compartment);
        }
        case 'ExistingReservation' {
            my $reservation = Shongo::Controller::API::Reservation->new();
            $reservation->from_xml($self->{'reservation'});
            $attributes->{'add'}('Reservation', $reservation);
        }
    }
    if ( defined($self->{'childReservations'}) && @{$self->{'childReservations'}} > 0) {
        my $collection = $attributes->{'add_collection'}("Child Reservations");
        foreach my $reservation (@{$self->{'childReservations'}}) {
            $collection->{'add'}($reservation);
        }
    }
    elsif ( defined($self->{'childReservationIdentifiers'}) && @{$self->{'childReservationIdentifiers'}} > 0) {
        my $collection = $attributes->{'add_collection'}("Child Reservations");
        foreach my $reservation (@{$self->{'childReservationIdentifiers'}}) {
            $collection->{'add'}($reservation);
        }
    }
}

#
# @return short description of reservation
#
sub to_string_short
{
    my ($self) = @_;
    my $name = $self->get_name();
    $name =~ s/ Reservation//g;

    # get attributes for this object
    my $attributes = Shongo::Controller::API::Object::create_attributes();
    $self->get_attributes($attributes);

    my $ignore = {'identifier' => 1, 'slot' => 1, 'resource' => 1};
    my $string = '';
    foreach my $attribute (@{$attributes->{'attributes'}}) {
        my $value = $attribute->{'value'};
        if ( ref($value) ) {
            if ( $value->can('to_string_short') ) {
                $value = $value->to_string_short();
            } else {
                $value = undef;
            }
        }
        if( !$ignore->{lc($attribute->{'name'})} && defined($value) && length($value) > 0 ) {
            if ( length($string) > 0 ) {
                $string .= ", ";
            }
            $string .= lc($attribute->{'name'}) . ': ' . $value;
        }
    }

    if ( length($string) > 0 ) {
        $string = $name . ' (' . $string . ')';
    } else {
        $string = $name;
    }

    return $string;
}

1;