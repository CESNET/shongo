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
        'update-reservation-request' => {
            desc => 'Allocate reservation for given reservation request (if it is in allocation failed)',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        update_reservation_request($id);
                    }
                } else {
                    update_reservation_request();
                }
            }
        },
        'list-reservation-requests' => {
            desc => 'List summary of all existing reservation requests',
            options => 'technology=s search=s resource=s',
            args => '[-technology <technologies>][-search <search>][-resource <resource-ids>]',
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
            options => 'user=s technology=s resource=s',
            args => '[-user=*|<user-id> -resource=<resource-id>]',
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
            return $response;
        }
        return undef;
    };

    if ( defined($response) ) {
        my $reservation_request = Shongo::ClientCli::API::ReservationRequestAbstract->from_hash($response);
        if ( defined($reservation_request) ) {
            my $new_id = $reservation_request->modify($attributes, $options);
            if ( defined($new_id) ) {
                console_print_info("Reservation request '%s' successfully modified to '%s'.", $id, $new_id);
            }
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

sub update_reservation_request()
{
    my ($id) = @_;
    $id = select_reservation_request($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Reservation.updateReservationRequest',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        console_print_info("Reservation request '$id' has been updated.");
    }
}

sub list_reservation_requests()
{
    my ($options) = @_;
    my $request = {};
    if ( defined($options->{'search'}) ) {
        $request->{'search'} = $options->{'search'};
    }
    if ( defined($options->{'technology'}) ) {
        $request->{'specificationTechnologies'} = [];
        foreach my $technology (split(/,/, $options->{'technology'})) {
            $technology =~ s/(^ +)|( +$)//g;
            push(@{$request->{'specificationTechnologies'}}, $technology);
        }
    }
    if ( defined($options->{'resource'}) ) {
        $request->{'specificationResourceIds'} = [];
        foreach my $resource (split(/,/, $options->{'resource'})) {
            $resource =~ s/(^ +)|( +$)//g;
            push(@{$request->{'specificationResourceIds'}}, $resource);
        }
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Reservation.listReservationRequests', $request);
    if ( !defined($response) ) {
        return
    }
    my $SpecificationType = {
        'RESOURCE' => 'Resource',
        'ROOM' => 'Room',
        'PERMANENT_ROOM' => 'Permanent Room',
        'USED_ROOM' => 'Used Room',
        'ALIAS' => 'Alias'
    };
    my $table = {
        'columns' => [
            {'field' => 'id',              'title' => 'Identifier'},
            {'field' => 'dateTime',        'title' => 'Created'},
            {'field' => 'user',            'title' => 'User'},
            {'field' => 'specification',   'title' => 'Type'},
            {'field' => 'technology',      'title' => 'Technology'},
            {'field' => 'allocationState', 'title' => 'Allocation'},
            {'field' => 'executableState', 'title' => 'Executable'},
            {'field' => 'description',     'title' => 'Description'},
            {'field' => 'auxData',         'title' => 'Auxiliary Data'},
        ],
        'data' => []
    };
    foreach my $reservation_request (@{$response->{'items'}}) {
        my $specification = 'Other';
        if ( defined($reservation_request->{'specificationType'}) && defined($SpecificationType->{$reservation_request->{'specificationType'}}) ) {
            $specification = $SpecificationType->{$reservation_request->{'specificationType'}};
        }
        if ( $reservation_request->{'specificationType'} eq 'ROOM') {
            $specification .= ' (';
            $specification .= $reservation_request->{'roomParticipantCount'};
            if ( $reservation_request->{'roomHasRecordingService'}) {
                $specification .= ', recorded';
            }
            $specification .= ')';
        }
        elsif ( $reservation_request->{'specificationType'} eq 'USED_ROOM' ) {
            $specification .= ' (';
            $specification .= $reservation_request->{'roomName'};
            $specification .= ', ';
            $specification .= $reservation_request->{'roomParticipantCount'};
            if ( $reservation_request->{'roomHasRecordingService'}) {
                $specification .= ', recorded';
            }
            $specification .= ')';
        }
        elsif ( $reservation_request->{'specificationType'} eq 'PERMANENT_ROOM' ) {
            $specification .= ' (';
            $specification .= $reservation_request->{'roomName'};
            $specification .= ')';
        }
        my $technologies = '';
        foreach my $technology (@{$reservation_request->{'specificationTechnologies'}}) {
            if ( length($technologies) > 0 ) {
                $technologies .= ', ';
            }
            $technologies .= $Shongo::Common::Technology->{$technology};
        }
        push(@{$table->{'data'}}, {
            'id' => $reservation_request->{'id'},
            'dateTime' => [$reservation_request->{'dateTime'}, datetime_format($reservation_request->{'dateTime'})],
            'user' => [$reservation_request->{'userId'}, $application->format_user($reservation_request->{'userId'})],
            'specification' => [$reservation_request->{'specificationType'}, $specification],
            'technology' => $technologies,
            'allocationState' => Shongo::ClientCli::API::ReservationRequest::format_state($reservation_request->{'allocationState'}),
            'executableState' => Shongo::ClientCli::API::ReservationRequest::format_state($reservation_request->{'executableState'}),
            'description' => $reservation_request->{'description'},
            'auxData' => $reservation_request->{'auxData'},
        });
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
        if ( $reservation_request->{'class'} eq 'ReservationRequestSet' ) {
            my $response = Shongo::ClientCli->instance()->secure_hash_request('Reservation.listReservationRequests',{
                'parentReservationRequestId' => RPC::XML::string->new($id)
            });
            $reservation_request->{'reservationRequests'} = $response->{'items'};
        }
        if ( defined($reservation_request) ) {
            console_print_text($reservation_request);
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
    my $response = Shongo::ClientCli->instance()->secure_hash_request('Reservation.listReservations', {
        'reservationRequestId' => $id
    });
    if ( !defined($response) ) {
        return;
    }
    $response = $response->{'items'};
    if (get_collection_size($response) == 0) {
        return;
    }
    my $array = [];
    if ( Shongo::ClientCli::is_scripting() ) {
        foreach my $reservationXml (@{$response}) {
            my $reservation = Shongo::ClientCli::API::Reservation->from_hash($reservationXml);
            push(@{$array}, $reservation->to_hash());
        }
    }
    else {
        foreach my $reservationXml (@{$response}) {
            my $reservation = Shongo::ClientCli::API::Reservation->from_hash($reservationXml);
            $reservation->fetch_child_reservations(1);
            push(@{$array}, $reservation);
        }
    }
    console_print_text($array);
}

sub list_reservations()
{
    my ($options) = @_;
    my $request = {};
    if ( defined($options->{'resource'}) ) {
        $request->{'resourceIds'} = [$options->{'resource'}];
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Reservation.listReservations', $request);
    if ( !defined($response) ) {
        return
    }
    my $table = {
        'columns' => [
            {'field' => 'id',         'title' => 'Identifier'},
            {'field' => 'type',       'title' => 'Type'},
            {'field' => 'slot',       'title' => 'Slot'},
            {'field' => 'resourceId', 'title' => 'Resource'},
        ],
        'data' => []
    };
    foreach my $reservation (@{$response->{'items'}}) {
        push(@{$table->{'data'}}, {
            'id' => $reservation->{'id'},
            'type' => $reservation->{'type'},
            'slot' => [$reservation->{'slot'}, interval_format($reservation->{'slot'})],
            'resourceId' => $reservation->{'resourceId'},
        });
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
            console_print_text($reservation);
        }
    }
}

1;