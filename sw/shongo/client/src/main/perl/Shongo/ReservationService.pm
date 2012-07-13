#
# Reservation class - Management of reservations.
#
package Shongo::ReservationService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ReservationRequest;
use Switch;

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    my @tree = (
        'reservation' => 'Management of reservations',
        'reservation create' => {
            help => 'Create a new reservation request',
            opts => 'type=s name=s purpose=s slot=s@ person=s@ resource=s@',
            exec => sub {
                my ($shell, %params) = @_;
                create_reservation(%params);
            }
        },
        'reservation modify' => {
            help => 'Modify an existing reservation request',
            opts => 'id=i',
            exec => sub {
                my ($shell, %p) = @_;
                modify_reservation(%p);
            }
        },
        'reservation delete' => {
            help => 'Delete an existing reservation request',
            opts => 'id=s',
            exec => sub {
                my ($shell, %p) = @_;
                delete_reservation($p{"id"});
            }
        },
        'reservation list' => {
            help => 'List all existing reservations',
            opts => '',
            exec => sub {
                my ($shell, %params) = @_;
                list_reservations(%params);
            }
        }
    );
    $shell->populate(@tree);
}

sub create_reservation()
{
    my (%attributes) = @_;

    my $identifier = Shongo::ReservationRequest->create(%attributes);
    if ( defined($identifier) ) {
        console_print_info("Reservation request '%s' successfully created.", $identifier);
    }
}

sub modify_reservation()
{
    my (%attributes) = @_;

    my $id = $attributes{"id"};
    if (defined($id) == 0) {
        console_print_error("[ERROR] You must specify 'id' for the reservation to be modified.\n");
        return;
    }

    print("[TODO] Modify reservation with id=$id\n");
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
    use Text::Table;
    my  $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Type', \' | ', 'Name', \' | ', 'Purpose', \' | ', 'Earliest Slot', \' |');
    ;
    foreach my $reservation_request (@{$response->value()}) {
         $table->add(
            $reservation_request->{'identifier'},
            $reservation_request->{'type'},
            $reservation_request->{'name'},
            $reservation_request->{'purpose'},
            $reservation_request->{'earliestSlot'}
        );
    }
    print $table->rule( '-', '+'), $table->title, $table->rule( '-', '+'), $table->body, $table->rule( '-', '+');
}

1;