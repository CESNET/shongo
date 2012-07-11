#
# Reservation class - Management of reservations.
#
package Shongo::Client::Reservation;

use strict;
use warnings;

use Shongo::Client::Dialog;
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
            help => 'Create a new reservation',
            opts => 'type=s purpose=s',
            exec => sub {
                my ($shell, %params) = @_;
                create_reservation(%params);
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

# Enumeration of reservation request type
our %enum_type = ordered_hash('NORMAL' => 'Normal', 'PERMANENT' => 'Permanent');

# Enumeration of reservation request purpose
our %enum_purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');


#
# Print reservation request
#
# @param reservation_request reference to reservation request
#
sub print_reservation_request
{
    my ($reservation_request) = @_;

    print " RESERVATION REQUEST\n";
    print "    Type: $enum_type{$reservation_request->{'type'}}\n";
    print " Purpose: $enum_purpose{$reservation_request->{'purpose'}}\n";
    print_reservation_request_slots($reservation_request);
    print_reservation_request_compartments($reservation_request);
}

#
# Print reservation request slots
#
# @param reservation_request reference to reservation request
#

sub print_reservation_request_slots
{
    my ($reservation_request) = @_;

    print " Requested slots:\n";
    if ( defined($reservation_request->{'slots'}) && $reservation_request->{'slots'} > 0) {
        my $index = 1;
        foreach my $slot ( @{$reservation_request->{'slots'}} ) {
            if ( ref($slot) ) {
                $slot = sprintf("%s/%s", $slot->{'start'}, $slot->{'period'});
            }
            printf("   %d) %s\n", $index, $slot);
            $index++;
        }
    }
    else {
        print "   -- None --\n";
    }
}

#
# Print reservation request compartments
#
# @param reservation_request reference to reservation request
#
sub print_reservation_request_compartments
{
    my ($reservation_request) = @_;

    print " Requested compartments:\n";
    if ( defined($reservation_request->{'compartments'}) && $reservation_request->{'compartments'} > 0) {
    }
    else {
        print "   -- None --\n";
    }
}

#
# Modify reservation request slots
#
# @param reservation_request reference to reservation request
#
sub modify_reservation_request_slots
{
    my ($reservation_request) = @_;

    while ( 1 ) {
        print "\n";
        print_reservation_request_slots($reservation_request);
        print "\n";

        if ( !defined($reservation_request->{'slots'}) ) {
            $reservation_request->{'slots'} = [];
        }

        my $action = dialog_select('Select action', ordered_hash_ref(
            'new-absolute' => 'Add new requested slot by absolute date/time',
            'new-periodic' => 'Add new requested slot by periodic date/time',
            'remove' => 'Remove existing requested slot',
            'stop' => 'Stop modifying requested slots',
        ));
        switch ( $action ) {
            case 'new-absolute' {
                my $dateTime = dialog_get("Type a date/time"); # "\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d"
                if ( defined($dateTime) ) {
                    push($reservation_request->{'slots'}, $dateTime);
                }
            }
            case 'new-periodic' {
                my $dateTime = dialog_get("Type a starting date/time");
                my $period = dialog_get("Type a period");
                if ( defined($dateTime) && defined($period) ) {
                    push($reservation_request->{'slots'}, {'start' => $dateTime, 'period' => $period});
                }
            }
            case 'remove' {
                my $index = dialog_get("Type a number of requested slot", "\\d+");
                printf("TODO: remove slot %d\n", $index);
            }
            else {
                return;
            }
        }
    }
}

#
# Validate reservation request
#
# @param reservation_request reference to reservation request
#
sub validate_reservation_request
{
    my ($reservation_request) = @_;

    if ( !defined($reservation_request->{'slots'}) || $reservation_request->{'slots'} == 0) {
        dialog_error("Requested slots should not be empty!");
        return 0;
    }
    if ( !defined($reservation_request->{'compartments'}) || $reservation_request->{'compartments'} == 0) {
        dialog_error("Requested compartments should not be empty!");
        return 0;
    }
    return 1;
}

#
# Create a new reservation.
#
# @param hash map of attributes, the type must be presented
#
sub create_reservation()
{
    my (%attributes) = @_;

    my %reservation_request = ();
    $reservation_request{'type'} = dialog_select('Select reservation type:', \%enum_type, $attributes{'type'});
    $reservation_request{'purpose'} = dialog_select('Select reservation purpose:', \%enum_purpose, $attributes{'purpose'});

    while ( 1 ) {
        print "\n";
        print_reservation_request(\%reservation_request);
        print "\n";

        my $action = dialog_select('Select action', ordered_hash_ref(
            'slots' => 'Modify requested slots',
            'compartments' => 'Modify requested compartment',
            'confirm' => 'Finish reservation request',
            'cancel' => 'Cancel reservation request'
        ));
        switch ( $action ) {
        	case 'slots' {
        	    modify_reservation_request_slots(\%reservation_request);
        	}
        	case 'compartments' {
                modify_reservation_request_compartments(\%reservation_request);
            }
            case 'confirm' {
                if ( validate_reservation_request(\%reservation_request) ) {
                    print("[TODO] Create reservation.\n");
                    print_reservation_request(\%reservation_request);
                    return;
                }
            }
            else {
                return;
            }
        }
    }
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