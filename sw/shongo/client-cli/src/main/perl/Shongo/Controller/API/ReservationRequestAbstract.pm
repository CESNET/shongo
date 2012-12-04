#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestAbstract;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::ReservationRequest;
use Shongo::Controller::API::ReservationRequestSet;
use Shongo::Controller::API::PermanentReservationRequest;

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('AbstractReservationRequest');
    $self->set_object_name('Reservation Request');
    $self->add_attribute('identifier', {
        'editable' => 0
    });
    $self->add_attribute('created', {
        'type' => 'datetime',
        'editable' => 0
    });
    $self->add_attribute('name', {
        'required' => 1
    });
    $self->add_attribute('description');

    return $self;
}

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    my $class = $attributes->{'class'};
    if ( !defined($class) ) {
        $class = console_read_enum('Select type of reservation request', ordered_hash(
            'ReservationRequest' => 'Single Reservation Request',
            'ReservationRequestSet' => 'Set of Reservation Requests',
            'PermanentReservationRequest' => 'Permanent Reservation Request'
        ));
    }
    if ($class eq 'ReservationRequest') {
        return Shongo::Controller::API::ReservationRequest->new();
    }
    elsif ($class eq 'ReservationRequestSet') {
        return Shongo::Controller::API::ReservationRequestSet->new();
    }
    elsif ($class eq 'PermanentReservationRequest') {
        return Shongo::Controller::API::PermanentReservationRequest->new();
    }
    die("Unknown reservation type type '$class'.");
}

# @Override
sub on_modify_confirm
{
    my ($self) = @_;

    return 1;
}

1;