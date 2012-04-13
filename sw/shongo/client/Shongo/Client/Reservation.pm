#
# Reservation class - Management of reservations.
#
package Shongo::Client::Reservation;

use strict;
use warnings;

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
            help => 'Create a new reservation',
            opts => 'type=s',
            exec => sub {
                my ($shell, %p) = @_;
                create_reservation(%p);
            },
        },
        'reservation modify' => {
            help => 'Modify an existing reservation',
            opts => 'id=i',
            exec => sub {
                my ($shell, %p) = @_;
                modify_reservation(%p);
            },
        },
        'reservation delete' => {
            help => 'Delete an existing reservation',
            opts => 'id=i',
            exec => sub {
                my ($shell, %p) = @_;
                delete_reservation($p{"id"});
            },
        }
    );
    $shell->populate(@tree);
}

#
# Create a new reservation.
#
# @param hash map of attributes, the type must be presented
#
sub create_reservation()
{
    my (%attributes) = @_;

    my $type = $attributes{"type"};
    if (defined($type) == 0) {
        print("[ERROR] You must specify 'type' for a new reservation.\n");
        return;
    }

    print("[TODO] Create reservation with type=${type}.\n");
}

#
# Modify an existing reservation.
#
# @param hash map of attributes, the id must be presented
#
sub modify_reservation()
{
    my (%attributes) = @_;

    my $id = $attributes{"id"};
    if (defined($id) == 0) {
        print("[ERROR] You must specify 'id' for the reservation to be modified.\n");
        return;
    }

    print("[TODO] Modify reservation with id=$id\n");
}

#
# Delete an existing reservation.
#
# @param id
#
sub delete_reservation()
{
    my ($id) = @_;
    if (defined($id) == 0) {
        print("[ERROR] You must specify 'id' for the reservation to be deleted.\n");
        return;
    }

    print("TODO: Delete reservation with id=$id\n");
}

1;