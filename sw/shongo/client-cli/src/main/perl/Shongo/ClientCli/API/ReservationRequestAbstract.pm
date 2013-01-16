#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ReservationRequestAbstract;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::ReservationRequest;
use Shongo::ClientCli::API::ReservationRequestSet;
use Shongo::ClientCli::API::PermanentReservationRequest;

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('AbstractReservationRequest');
    $self->set_object_name('Reservation Request');
    $self->add_attribute('id', {
        'title' => 'Identifier',
        'editable' => 0
    });
    $self->add_attribute('userId', {
        'title' => 'Owner',
        'format' => sub { return Shongo::ClientCli->instance()->format_user(@_, 1); },
        'editable' => 0
    });
    $self->add_attribute('created', {
        'type' => 'datetime',
        'editable' => 0
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
        return Shongo::ClientCli::API::ReservationRequest->new();
    }
    elsif ($class eq 'ReservationRequestSet') {
        return Shongo::ClientCli::API::ReservationRequestSet->new();
    }
    elsif ($class eq 'PermanentReservationRequest') {
        return Shongo::ClientCli::API::PermanentReservationRequest->new();
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