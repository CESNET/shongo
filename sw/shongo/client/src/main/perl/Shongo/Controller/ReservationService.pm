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
            desc => 'List summary of all existing reservation requests',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_reservations($params->{'options'});
            }
        },
        'get-reservation' => {
            desc => 'Get existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_reservation($args[0]);
            }
        },
        'get-allocation' => {
            desc => 'Get allocation for existing reservation request',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_allocation($args[0])
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
        $table->add(
            $reservation_request->{'identifier'},
            $Shongo::Controller::API::ReservationRequest::Type->{$reservation_request->{'type'}},
            $reservation_request->{'name'},
            $Shongo::Controller::API::ReservationRequest::Purpose->{$reservation_request->{'purpose'}},
            format_interval($reservation_request->{'earliestSlot'})
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

sub get_allocation()
{
    my ($identifier) = @_;
    $identifier = select_reservation($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Reservation.listAllocatedCompartments',
        RPC::XML::string->new($identifier)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $index = 0;
    foreach my $allocated_compartment (@{$result->value()}) {
        $index++;
        printf("%d) %s\n", $index, format_interval($allocated_compartment->{'slot'}));
        foreach my $allocated_resource (@{$allocated_compartment->{'allocatedResources'}}) {
            my $class = $allocated_resource->{'class'};
            print("   -");
            if ( $class eq 'AllocatedVirtualRoom') {
                printf("%s (%s) VirtualRoom(portCount: %d)", $allocated_resource->{'resourceName'},
                    $allocated_resource->{'resourceIdentifier'}, $allocated_resource->{'portCount'});
            } else {
                printf("%s (%s)", $allocated_resource->{'resourceName'}, $allocated_resource->{'resourceIdentifier'});
            }
            print("\n");
        }
    }
}

1;