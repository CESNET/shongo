#
# Management of reservations.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::ReservationService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;
use Shongo::Shell;
use Shongo::ClientCli::API::ReservationRequestAbstract;
use Shongo::ClientCli::API::Reservation;
use Shongo::ClientCli::API::Alias;

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
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    create_reservation_request($attributes, $params->{'options'});
                }
            }
        },
        'modify-reservation-request' => {
            desc => 'Modify an existing reservation request',
            args => '[id] [<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    modify_reservation_request($args[0], $attributes, $params->{'options'});
                }
            }
        },
        'delete-reservation-request' => {
            desc => 'Delete an existing reservation request',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        delete_reservation_request($id);
                    }
                } else {
                    delete_reservation_request();
                }
            }
        },
        'list-reservation-requests' => {
            desc => 'List summary of all existing reservation requests',
            options => 'user=s technology=s',
            args => '[-user=*|<user-id>] [-technology]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_reservation_requests($params->{'options'});
            }
        },
        'get-reservation-request' => {
            desc => 'Get existing reservation request',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_reservation_request($id);
                    }
                } else {
                    get_reservation_request();
                }
            }
        },
        'get-reservation-for-request' => {
            desc => 'Get allocated reservations for existing reservation request',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_reservation_for_request($id);
                    }
                } else {
                    get_reservation_for_request();
                }
            }
        },
        'list-reservations' => {
            desc => 'List existing reservations',
            options => 'user=s technology=s',
            args => '[-user=*|<user-id>] [-technology]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_reservations($params->{'options'});
            }
        },
        'get-reservation' => {
            desc => 'Get existing reservation',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_reservation($id);
                    }
                } else {
                    get_reservation();
                }
            }
        },
    });
}

sub select_reservation_request
{
    my ($id, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'id'}) ) {
        $id = $attributes->{'id'};
    }
    $id = console_read_value('Identifier of the reservation request', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}

sub create_reservation_request()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($reservation_request) = @_;
        console_print_info("Creating reservation request...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Reservation.createReservationRequest',
            $reservation_request->to_xml()
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $id = Shongo::ClientCli::API::ReservationRequestAbstract->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Reservation request '%s' successfully created.", $id);
    }
}

sub modify_reservation_request()
{
    my ($id, $attributes, $options) = @_;
    $id = select_reservation_request($id, $attributes);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($id)
    );

    $options->{'on_confirm'} = sub {
        my ($reservation_request) = @_;
        console_print_info("Modifying reservation request...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Reservation.modifyReservationRequest',
            $reservation_request->to_xml()
        );
        if ( defined($response) ) {
            return $reservation_request->{'id'};
        }
        return undef;
    };

    if ( defined($response) ) {
        my $reservation_request = Shongo::ClientCli::API::ReservationRequestAbstract->from_hash($response);
        if ( defined($reservation_request) ) {
            $reservation_request->modify($attributes, $options);
        }
    }
}

sub delete_reservation_request()
{
    my ($id) = @_;
    $id = select_reservation_request($id);
    if ( !defined($id) ) {
        return;
    }
    Shongo::ClientCli->instance()->secure_request(
        'Reservation.deleteReservationRequest',
        RPC::XML::string->new($id)
    );
}

sub list_reservation_requests()
{
    my ($options) = @_;
    my $filter = {};
    if ( defined($options->{'technology'}) ) {
        $filter->{'technology'} = [];
        foreach my $technology (split(/,/, $options->{'technology'})) {
            $technology =~ s/(^ +)|( +$)//g;
            push(@{$filter->{'technology'}}, $technology);
        }
    }
    if ( defined($options->{'user'}) ) {
        $filter->{'userId'} = $options->{'user'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Reservation.listReservationRequests', $filter);
    if ( !defined($response) ) {
        return
    }
    my $table = Text::Table->new(
        \'| ', 'Identifier',
        \' | ', 'User',
        \' | ', 'Created',
        \' | ', 'Type',
        \' | ', 'Description',
        \' | ', 'Earliest Slot', \' |'
    );
    my $Type = {
        'ReservationRequestSummary.ResourceType' => 'Resource',
        'ReservationRequestSummary.RoomType' => 'Room',
        'ReservationRequestSummary.AliasType' => 'Alias'
    };
    foreach my $reservation_request (@{$response}) {
        my $type = 'Other';
        if ( defined($reservation_request->{'type'}) && defined($reservation_request->{'type'}->{'class'}) ) {
            $type = $Type->{$reservation_request->{'type'}->{'class'}};
        }
        $table->add(
            $reservation_request->{'id'},
            $application->format_user($reservation_request->{'userId'}),
            datetime_format($reservation_request->{'created'}),
            $type,
            $reservation_request->{'description'},
            interval_format($reservation_request->{'earliestSlot'})
        );
    }
    console_print_table($table);
}

sub get_reservation_request()
{
    my ($id) = @_;
    $id = select_reservation_request($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Reservation.getReservationRequest',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        my $reservation_request = Shongo::ClientCli::API::ReservationRequestAbstract->from_hash($response);
        if ( defined($reservation_request) ) {
            console_print_text($reservation_request->to_string());
        }
    }
}

sub get_reservation_for_request()
{
    my ($id) = @_;
    $id = select_reservation_request($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Reservation.listReservations', {
        'reservationRequestId' => $id,
        'userId' => '*'
    });
    if ( !defined($response) ) {
        return;
    }
    if (get_collection_size($response) == 0) {
        return;
    }
    print("\n");
    my $index = 0;
    foreach my $reservationXml (@{$response}) {
        my $reservation = Shongo::ClientCli::API::Reservation->from_hash($reservationXml);
        $reservation->fetch_child_reservations(1);
        $index++;
        printf(" %d) %s\n", $index, text_indent_lines($reservation->to_string(), 4, 0));
    }
}

sub list_reservations()
{
    my ($options) = @_;
    my $filter = {};
    if ( defined($options->{'technology'}) ) {
        $filter->{'technology'} = [];
        foreach my $technology (split(/,/, $options->{'technology'})) {
            $technology =~ s/(^ +)|( +$)//g;
            push(@{$filter->{'technology'}}, $technology);
        }
    }
    if ( defined($options->{'user'}) ) {
        $filter->{'userId'} = $options->{'user'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Reservation.listReservations', $filter);
    if ( !defined($response) ) {
        return
    }
    my $table = Text::Table->new(
        \'| ', 'Identifier',
        \' | ', 'User',
        \' | ', 'Type',
        \' | ', 'Slot', \' |'
    );
    foreach my $reservation (@{$response}) {
        $table->add(
            $reservation->{'id'},
            $application->format_user($reservation->{'userId'}),
            $Shongo::ClientCli::API::Reservation::Type->{$reservation->{'class'}},
            interval_format($reservation->{'slot'})
        );
    }
    console_print_table($table);
}

sub select_reservation($)
{
    my ($id) = @_;
    $id = console_read_value('Identifier of the reservation', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}


sub get_reservation()
{
    my ($id) = @_;
    $id = select_reservation($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Reservation.getReservation',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        my $reservation = Shongo::ClientCli::API::Reservation->from_hash($response);
        $reservation->fetch_child_reservations(1);
        if ( defined($reservation) ) {
            console_print_text($reservation->to_string());
        }
    }
}

1;