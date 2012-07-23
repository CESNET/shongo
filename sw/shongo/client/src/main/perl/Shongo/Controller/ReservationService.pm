#
# Reservation class - Management of reservations.
#
package Shongo::Controller::ReservationService;

use strict;
use warnings;
use Text::Table;

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

sub select_reservation($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the reservation', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
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
    $identifier = select_reservation($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $reservation_request = Shongo::Controller::API::ReservationRequest->new()->from_xml($result);
        if ( defined($reservation_request) ) {
            $reservation_request->modify();
        }
    }
}

sub delete_reservation()
{
    my ($identifier) = @_;
    $identifier = select_reservation($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    Shongo::Controller->instance()->secure_request(
        'Reservation.deleteReservationRequest',
        RPC::XML::string->new($identifier)
    );
}

sub list_reservations()
{
    my $response = Shongo::Controller->instance()->secure_request(
        'Reservation.listReservationRequests'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Type', \' | ', 'Name', \' | ', 'Purpose', \' | ', 'Earliest Slot', \' |');
    foreach my $reservation_request (@{$response->value()}) {
        my $slot;
        if ( $reservation_request->{'earliestSlot'} =~ m/(.*)\/(.*)/ ) {
            $slot = sprintf("%s, %s", format_datetime($1), $2);
        }
        $table->add(
            $reservation_request->{'identifier'},
            $Shongo::Controller::API::ReservationRequest::Type->{$reservation_request->{'type'}},
            $reservation_request->{'name'},
            $Shongo::Controller::API::ReservationRequest::Purpose->{$reservation_request->{'purpose'}},
            $slot
        );
    }
    console_print_table($table);
}

sub get_reservation()
{
    my ($identifier) = @_;
    $identifier = select_reservation($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $reservation_request = Shongo::Controller::API::ReservationRequest->new()->from_xml($result);
        if ( defined($reservation_request) ) {
            printf("\n%s\n", $reservation_request->to_string());
        }
    }
}

1;