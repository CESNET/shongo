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

    $self->set_object_name('Reservation');
    $self->set_object_class('Reservation');
    $self->add_attribute('identifier');
    $self->add_attribute('slot', {
        'type' => 'interval'
    });
    $self->add_attribute('childReservations', {
        'type' => 'collection',
        'title' => 'Child Reservations',
        'display' => 'newline'
    });
    $self->add_attribute('childReservationIdentifiers', {
        'type' => 'collection',
        'title' => 'Child Reservation Identifiers'
    });

    return $self;
}

# @Override
sub on_init
{
    my ($self) = @_;

    my $class = $self->get_object_class();
    if ( !defined($class) ) {
        return;
    }

    if ( exists $Type->{$class} ) {
        $self->set_object_name($Type->{$class});
    }

    switch ($class) {
        case 'ResourceReservation' {
            $self->add_attribute_preserve('resourceName');
            $self->add_attribute_preserve('resourceIdentifier');
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceIdentifier'});
                }
            });
        }
        case 'VirtualRoomReservation' {
            $self->add_attribute_preserve('resourceName');
            $self->add_attribute_preserve('resourceIdentifier');
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceIdentifier'});
                }
            });
            $self->add_attribute('portCount', {
                'title' => 'Port Count'
            });
        }
        case 'AliasReservation' {
            $self->add_attribute_preserve('resourceName');
            $self->add_attribute_preserve('resourceIdentifier');
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceIdentifier'});
                }
            });
            $self->add_attribute('alias');
        }
        case 'CompartmentReservation' {
            $self->add_attribute('compartment', {
                'display' => 'newline'
            });
        }
        case 'ExistingReservation' {
            $self->add_attribute('reservation');
        }
    }
}

#
# Fetch child reservations
#
sub fetch_child_reservations
{
    my ($self, $recursive) = @_;

    if ( defined($self->{'childReservationIdentifiers'}) && @{$self->{'childReservationIdentifiers'}} > 0) {
        my $child_reservations = [];
        foreach my $reservation (@{$self->{'childReservationIdentifiers'}}) {
            my $result = Shongo::Controller->instance()->secure_request(
                'Reservation.getReservation',
                RPC::XML::string->new($reservation)
            );
            if ( $result->is_fault ) {
                return;
            }
            my $reservationXml = $result->value();
            my $reservation = Shongo::Controller::API::Reservation->from_hash($reservationXml);
            if ( $recursive ) {
                $reservation->fetch_child_reservations($recursive);
            }
            push(@{$child_reservations}, $reservation);
        }
        $self->set('childReservations', $child_reservations);
        $self->set('childReservationIdentifiers', undef);
    }
}

#
# @return short description of reservation
#
sub to_string_short
{
    my ($self) = @_;
    my $name = $self->get_object_name();
    $name =~ s/ Reservation//g;

    my $ignore = {'identifier' => 1, 'slot' => 1, 'resource' => 1};
    my $string = '';
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute_value = $self->get($attribute_name);
        my $attribute_title = $self->get_attribute_title($attribute_name);
        if ( ref($attribute_value) ) {
            if ( defined($attribute_value->{'class'}) &&  $attribute_value->{'class'} eq 'Alias' ) {
                $attribute_value = $attribute_value->get('value');
            }
            elsif ( $attribute_value->can('to_string_short') ) {
                $attribute_value = $attribute_value->to_string_short();
            } else {
                $attribute_value = undef;
            }
        }
        if( !$ignore->{$attribute_name} && defined($attribute_value) && length($attribute_value) > 0 ) {
            if ( length($string) > 0 ) {
                $string .= ", ";
            }
            $string .= lc($attribute_title) . ': ' . $attribute_value;
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