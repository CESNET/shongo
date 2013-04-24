#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Reservation;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

#
# Reservation types
#
our $Type = ordered_hash(
    'Reservation' => 'Reservation',
    'ResourceReservation' => 'Resource Reservation',
    'RoomReservation' => 'Room Reservation',
    'ValueReservation' => 'Value Reservation',
    'AliasReservation' => 'Alias Reservation',
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
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_name('Reservation');
    $self->set_object_class('Reservation');
    $self->add_attribute('id', {'title' => 'Identifier'});
    $self->add_attribute('parentReservationId', {'title' => 'Parent'});
    $self->add_attribute('reservationRequestId', {'title' => 'Request'});
    $self->add_attribute('slot', {
        'type' => 'interval'
    });
    $self->add_attribute('childReservations', {
        'type' => 'collection',
        'title' => 'Child Reservations',
        'display' => 'newline',
        'order' => 2
    });
    $self->add_attribute('childReservationIds', {
        'type' => 'collection',
        'title' => 'Child Reservation Identifiers',
        'order' => 2
    });
    $self->add_attribute('executable', {
        'order' => 1,
        'format' => sub {
            my ($executable, $options) = @_;
            if ( !defined($executable) ) {
                return undef;
            }
            if ( defined($options->{'sub-call'}) ) {
                return $executable->{'id'};
            }
            else {
                return $executable->to_string();
            }
        }
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
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceId'});
                }
            });
        }
        case 'RoomReservation' {
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceId'});
                }
            });
            $self->add_attribute('licenseCount', {
                'title' => 'License Count'
            });
        }
        case 'ValueReservation' {
            $self->add_attribute('value');
        }
        case 'AliasReservation' {
            $self->add_attribute('resource', {
                'format' => sub () {
                    sprintf("%s (%s)", $self->{'resourceName'}, $self->{'resourceId'});
                }
            });
            $self->add_attribute('aliases', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Alias',
                    'class' => 'Shongo::ClientCli::API::Alias',
                    'short' => 1
                }
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

    if ( defined($self->{'childReservationIds'}) && @{$self->{'childReservationIds'}} > 0) {
        my $child_reservations = [];
        foreach my $reservation (@{$self->{'childReservationIds'}}) {
            my $response = Shongo::ClientCli->instance()->secure_request(
                'Reservation.getReservation',
                RPC::XML::string->new($reservation)
            );
            if ( !defined($response) ) {
                return;
            }
            my $reservation = Shongo::ClientCli::API::Reservation->from_hash($response);
            if ( $recursive ) {
                $reservation->fetch_child_reservations($recursive);
            }
            push(@{$child_reservations}, $reservation);
        }
        $self->set('childReservations', $child_reservations);
        $self->set('childReservationIds', undef);
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

    my $ignore = {'id' => 1, 'slot' => 1, 'resource' => 1};
    my $string = '';
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute_value = $self->get($attribute_name);
        my $attribute_title = $self->get_attribute_title($attribute_name);
        if ( ref($attribute_value) ) {
            $attribute_value = undef;
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