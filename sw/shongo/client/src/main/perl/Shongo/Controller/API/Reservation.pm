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

#
# Reservation types
#
our $Type = ordered_hash(
    'ResourceReservation' => 'Resource Reservation',
    'VirtualRoomReservation' => 'Virtual Room Reservation',
    'AliasReservation' => 'Alias Reservation'
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
sub to_string_attributes
{
    my ($self) = @_;

    my $string = '';
    $string .= sprintf("identifier: %s \n", $self->{'identifier'});
    $string .= sprintf("      slot: %s \n", format_interval($self->{'slot'}));
    switch ($self->{'class'}) {
        case 'ResourceReservation' {
            $string .= sprintf("name: %s (%s)\n",
                $self->{'resourceName'},
                $self->{'resourceIdentifier'}
            );
        }
        case 'VirtualRoomReservation' {
            $string .= sprintf("    device: %s (%s)\n",
                            $self->{'resourceName'},
                            $self->{'resourceIdentifier'}
            );
            $string .= sprintf(" portCount: %d\n", $self->{'portCount'});
        }
        case 'AliasReservation' {
            $string .= sprintf("  resource: %s (%s)\n",
                            $self->{'resourceName'},
                            $self->{'resourceIdentifier'}
            );
            $string .= sprintf("     alias: %s\n", $self->{'alias'}->to_string());
        }
    }
    if ( defined($self->{'childReservations'}) && @{$self->{'childReservations'}} > 0) {
        $string .= "child reservations:\n";
        my $index = 0;
        foreach my $reservation (@{$self->{'childReservations'}}) {
            $index++;
            $string .= sprintf(" %d) %s", $index, text_indent_lines($reservation->to_string(), 5, 0));
        }
    }
    elsif ( defined($self->{'childReservationIdentifiers'}) && @{$self->{'childReservationIdentifiers'}} > 0) {
        my $childReservations = '';
        foreach my $reservation (@{$self->{'childReservationIdentifiers'}}) {
            if ( length($childReservations) > 0 ) {
                $childReservations .= ', ';
            }
            $childReservations .= $reservation;
        }
        $string .= 'child reservations: ' . $childReservations . "\n";
    }

    return $string;
}

1;