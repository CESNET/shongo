#
# Management of reservations.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::ReservationService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;
use Shongo::Shell;
use Shongo::Controller::API::ReservationRequestAbstract;
use Shongo::Controller::API::Reservation;
use Shongo::Controller::API::Alias;

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'create-reservation-request' => {
            desc => 'Create a new reservation request',
            options => 'confirm',
            args => '[-confirm]',
            method => sub {
                my ($shell, $params, @args) = @_;
                create_reservation_request(Shongo::Shell::parse_attributes($params), $params->{'options'});
            }
        },
        'modify-reservation-request' => {
            desc => 'Modify an existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_reservation_request($args[0]);
            }
        },
        'delete-reservation-request' => {
            desc => 'Delete an existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_reservation_request($args[0]);
            }
        },
        'list-reservation-requests' => {
            desc => 'List summary of all existing reservation requests',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_reservation_requests($params->{'options'});
            }
        },
        'get-reservation-request' => {
            desc => 'Get existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_reservation_request($identifier);
                    }
                } else {
                    get_reservation_request();
                }
            }
        },
        'get-reservation-for-request' => {
            desc => 'Get allocated reservations for existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_reservation_for_request($identifier);
                    }
                } else {
                    get_reservation_for_request();
                }
            }
        },
        'get-reservation' => {
            desc => 'Get existing reservation',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_reservation($identifier);
                    }
                } else {
                    get_reservation();
                }
            }
        },
    });
}

sub select_reservation_request($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the reservation request', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
}

sub create_reservation_request()
{
    my ($attributes, $options) = @_;

    my $identifier = Shongo::Controller::API::ReservationRequestAbstract->create($attributes, $options);
    if ( defined($identifier) ) {
        console_print_info("Reservation request '%s' successfully created.", $identifier);
    }
}

sub modify_reservation_request()
{
    my ($identifier) = @_;
    $identifier = select_reservation_request($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $reservation_request = Shongo::Controller::API::ReservationRequestAbstract->from_hash($result);
        if ( defined($reservation_request) ) {
            $reservation_request->modify();
        }
    }
}

sub delete_reservation_request()
{
    my ($identifier) = @_;
    $identifier = select_reservation_request($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    Shongo::Controller->instance()->secure_request(
        'Reservation.deleteReservationRequest',
        RPC::XML::string->new($identifier)
    );
}

sub list_reservation_requests()
{
    my $response = Shongo::Controller->instance()->secure_request(
        'Reservation.listReservationRequests'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(
        \'| ', 'Identifier',
        \' | ', 'Created',
        \' | ', 'Type',
        \' | ', 'Name',
        #\' | ', 'Purpose',
        \' | ', 'Earliest Slot', \' |'
    );
    my $Type = {
        'NORMAL' => 'Normal',
        'PERMANENT' => 'Permanent'
    };
    foreach my $reservation_request (@{$response->value()}) {
        $table->add(
            $reservation_request->{'identifier'},
            format_date($reservation_request->{'created'}),
            $Type->{$reservation_request->{'type'}},
            $reservation_request->{'name'},
            #$Shongo::Controller::API::ReservationRequest::Purpose->{$reservation_request->{'purpose'}},
            format_interval($reservation_request->{'earliestSlot'})
        );
    }
    console_print_table($table);
}

sub get_reservation_request()
{
    my ($identifier) = @_;
    $identifier = select_reservation_request($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $reservation_request = Shongo::Controller::API::ReservationRequestAbstract->from_hash($result);
        if ( defined($reservation_request) ) {
            console_print_text($reservation_request->to_string());
        }
    }
}

sub get_reservation_for_request()
{
    my ($identifier) = @_;
    $identifier = select_reservation_request($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.listReservations',
        RPC::XML::string->new($identifier)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $reservations = $result->value();
    if (get_collection_size($reservations) == 0) {
        return;
    }
    print("\n");
    my $index = 0;
    foreach my $reservationXml (@{$reservations}) {
        my $reservation = Shongo::Controller::API::Reservation->from_hash($reservationXml);
        $reservation->fetch_child_reservations(1);
        $index++;
        printf(" %d) %s\n", $index, text_indent_lines($reservation->to_string(), 4, 0));
    }
}

sub select_reservation($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the reservation', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
}


sub get_reservation()
{
    my ($identifier) = @_;
    $identifier = select_reservation($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.getReservation',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $reservation = Shongo::Controller::API::Reservation->from_hash($result);
        $reservation->fetch_child_reservations(1);
        if ( defined($reservation) ) {
            console_print_text($reservation->to_string());
        }
    }
}

1;