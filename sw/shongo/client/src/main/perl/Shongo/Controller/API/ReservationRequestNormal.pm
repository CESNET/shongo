#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestNormal;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

# Enumeration of reservation request purpose
our $Purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::ReservationRequestAbstract->new(@_);
    bless $self, $class;

    $self->add_attribute('purpose', {
        'type' => 'enum',
        'enum' => $Purpose
    });
    $self->add_attribute('providedReservationIdentifiers', {
        'type' => 'collection',
        'title' => 'Provided reservations',
        'collection-title' => 'provided reservation',
        'collection-add' => sub {
            return console_edit_value("Reservation identifier", 1, $Shongo::Common::IdentifierPattern);
        },
        'complex' => 0,
        'format' => sub {
            my ($providedReservationIdentifier) = @_;
            return sprintf("identifier: %s", $providedReservationIdentifier);
        }
    });
    return $self;
}

1;