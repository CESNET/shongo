#
# Reservation class - Management of reservations.
#
package Shongo::Controller::ReservationService;

use strict;
use warnings;
use Switch;
use Text::Table;
use DateTime::Format::ISO8601;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::ReservationRequest;

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'create-reservation' => {
            desc => 'Create a new reservation request',
            options => 'type=s name=s purpose=s slot=s@ person=s@ resource=s@',
            args => "[-type] [-name] [-purpose] [-slot] [-person] [-resource]",
            method => sub {
                my ($shell, $params, @args) = @_;
                create_reservation($params->{'options'});
            }
        },
        'modify-reservation' => {
            desc => 'Modify an existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_reservation($args[0]);
            }
        },
        'delete-reservation' => {
            desc => 'Delete an existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_reservation($args[0]);
            }
        },
        'list-reservations' => {
            desc => 'List summary of all existing reservations',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_reservations($params->{'options'});
            }
        },
        'get-reservation' => {
            desc => 'Get existing reservation',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_reservation($args[0]);
            }
        }
    });
}

sub create_reservation()
{
    my ($attributes) = @_;

    my $identifier = Shongo::Controller::API::ReservationRequest->new()->create($attributes);
    if ( defined($identifier) ) {
        console_print_info("Reservation request '%s' successfully created.", $identifier);
    }
}

sub modify_reservation()
{
    my ($identifier) = @_;

    if (defined($identifier) == 0) {
        console_print_error("You must specify 'identifier' for the reservation to be modified.\n");
        return;
    }

    print("[TODO] Modify reservation with id=$identifier\n");
}

sub delete_reservation()
{
    my ($identifier) = @_;

    $identifier = console_read('Identifier of the reservation', 0, 'shongo:.+:\\d', $identifier);
    if ( defined($identifier) ) {
        Shongo::Controller->instance()->request(
            'Reservation.deleteReservationRequest',
            RPC::XML::struct->new(),
            $identifier
        );
    }
}

sub list_reservations()
{
    my $response = Shongo::Controller->instance()->request(
        'Reservation.listReservationRequests',
        RPC::XML::struct->new()
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Type', \' | ', 'Name', \' | ', 'Purpose', \' | ', 'Earliest Slot', \' |');
    foreach my $reservation_request (@{$response->value()}) {
        my $slot;
        if ( $reservation_request->{'earliestSlot'} =~ m/(.*)\/(.*)/ ) {
            my $dateTime = DateTime::Format::ISO8601->parse_datetime($1);
            $slot = sprintf("%s %02d:%02d, %s", $dateTime->ymd, $dateTime->hour, $dateTime->minute, $2);
        }
        $table->add(
            $reservation_request->{'identifier'},
            $reservation_request->{'type'},
            $reservation_request->{'name'},
            $reservation_request->{'purpose'},
            $slot
        );
    }
    print $table->rule( '-', '+'), $table->title, $table->rule( '-', '+'), $table->body, $table->rule( '-', '+');
}

sub get_reservation()
{
    my ($identifier) = @_;

    $identifier = console_read('Identifier of the reservation', 0, 'shongo:.+:\\d', $identifier);
    if ( defined($identifier) ) {
        my $result = Shongo::Controller->instance()->request(
            'Reservation.getReservationRequest',
            RPC::XML::struct->new(),
            $identifier
        );
        if ( !$result->is_fault ) {
            my $reservation_request = Shongo::Controller::API::ReservationRequest->new()->from_xml($result);
            if ( defined($reservation_request) ) {
                printf("\n%s\n", $reservation_request->to_string());
            }
        }
    }
}


1;