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
    $self->add_attribute('name');
    $self->add_attribute('description');

    return $self;
}

# @Override
sub on_create_confirm
{
    my ($self) = @_;
    console_print_info("Creating reservation request...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Reservation.createReservationRequest',
        $self->to_xml()
    );
    if ( !$response->is_fault() ) {
        return $response->value();
    }
    return undef;
}

# @Override
sub on_modify_confirm
{
    my ($self) = @_;
    console_print_info("Modifying reservation request...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Reservation.modifyReservationRequest',
        $self->to_xml()
    );
    if ( $response->is_fault() ) {
        return 0;
    }
    return 1;
}

1;