#
# Reservation class - Management of reservations.
#
package Shongo::Client::ReservationService;

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
            opts => 'type=s name=s purpose=s',
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

# Enumeration of technologies
our %enum_technology = ordered_hash('H323' => 'H.323', 'SIP' => 'SIP', 'ADOBE_CONNECT' => 'Adobe Connect');

#
# Print reservation request
#
# @param reservation_request reference to reservation request
#
sub print_reservation_request
{
    my ($reservation_request) = @_;

    print " RESERVATION REQUEST\n";
    if ( defined($reservation_request->{'identifier'}) ) {
        print " Identifier: $enum_type{$reservation_request->{'identifier'}}\n";
    }
    print "       Type: $enum_type{$reservation_request->{'type'}}\n";
    print "       Name: $reservation_request->{'name'}\n";
    print "    Purpose: $enum_purpose{$reservation_request->{'purpose'}}\n";
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
    if ( defined($reservation_request->{'slots'}) && @{$reservation_request->{'slots'}} > 0) {
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
# Print compartment
#
# @param compartment reference to compartment
#
sub print_compartment
{
    my ($compartment) = @_;

    print " COMPARTMENT\n";
    print " Requested resources:\n";
    if ( defined($compartment->{'resources'}) && @{$compartment->{'resources'}} > 0) {
    }
    else {
        print "   -- None --\n";
    }
    print " Requested persons:\n";
    if ( defined($compartment->{'persons'}) && @{$compartment->{'persons'}} > 0) {
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
    if ( defined($reservation_request->{'compartments'}) && @{$reservation_request->{'compartments'}} > 0) {
        my $index = 1;
        foreach my $compartment ( @{$reservation_request->{'compartment'}} ) {
            my $resources_count = 0;
            if ( defined($compartment->{'resources'}) ) {
                $resources_count = @{$compartment->{'resources'}};
            }
            my $persons_count = 0;
            if ( defined($compartment->{'persons'}) ) {
                $persons_count = @{$compartment->{'persons'}};
            }
            printf("   %d) Compartment (resources: %d, persons: %d)\n", $index, $resources_count, $persons_count);
            $index++;
        }
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
        my $slots_count = @{$reservation_request->{'slots'}};

        my $actions = [
            'new-absolute' => 'Add new requested slot by absolute date/time',
            'new-periodic' => 'Add new requested slot by periodic date/time'
        ];
        if ( $slots_count ) {
            push($actions, 'remove' => 'Remove existing requested slot');
        }
        push($actions, 'stop' => 'Stop modifying requested slots');

        my $action = dialog_select('Select action', ordered_hash_ref($actions));
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
                if ( $slots_count > 0 ) {
                    my $index = dialog_get_choice("Type a number of requested slot", $slots_count);
                    splice($reservation_request->{'slots'}, $index - 1, 1);
                }
            }
            else {
                return;
            }
        }
    }
}

sub create_reservation_request_compartment
{
    my $compartment = {};
    modify_reservation_request_compartment();
    return $compartment;
}

sub modify_reservation_request_compartment
{
    my ($compartment) = @_;

    while ( 1 ) {
        print "\n";
        print_compartment($compartment);
        print "\n";

        if ( !defined($compartment->{'resources'}) ) {
            $compartment->{'resources'} = [];
        }
        if ( !defined($compartment->{'persons'}) ) {
            $compartment->{'persons'} = [];
        }
        my $resources_count = @{$compartment->{'resources'}};
        my $persons_count = @{$compartment->{'persons'}};

        my $actions = [];
        push($actions, 'resource-new' => 'Add new requested resource');
        if ( $resources_count) {
            #push($actions, 'resource-modify' => 'Modify existing requested resource');
            push($actions, 'resource-remove' => 'Remove existing requested resource');
        }
        push($actions, 'person-new' => 'Add new requested person');
        if ( $resources_count) {
            #push($actions, 'person-modify' => 'Modify existing requested person');
            push($actions, 'person-remove' => 'Remove existing requested person');
        }
        push($actions, 'stop' => 'Stop modifying compartment');

        my $action = dialog_select('Select action', ordered_hash_ref($actions));
        switch ( $action ) {
            case 'resource-new' {
                my $technology = dialog_select("Select technology", \%enum_technology);
                my $count = dialog_get("Count", 1, "\\d");
                if ( defined($technology) && defined($count) ) {
                    push($compartment->{'resources'}, {'technology' => $technology, 'count' => $count});
                }
            }
            case 'resource-remove' {
                if ( $resources_count > 0 ) {
                    my $index = dialog_get_choice("Type a number of requested person", $persons_count);
                    splice($compartment->{'resources'}, $index - 1, 1);
                }
            }
            case 'person-new' {
                my $name = dialog_get("Name");
                my $email = dialog_get("Email");
                if ( defined($name) && defined($email) ) {
                    push($compartment->{'persons'}, {'name' => $name, 'email' => $email});
                }
            }
            case 'person-remove' {
                if ( $persons_count > 0 ) {
                    my $index = dialog_get_choice("Type a number of requested person", $persons_count);
                    splice($compartment->{'persons'}, $index - 1, 1);
                }
            }
            else {
                return;
            }

        }
    }
}

#
# Modify reservation request compartment
#
# @param reservation_request reference to reservation request
#
sub modify_reservation_request_compartments
{
    my ($reservation_request) = @_;

    while ( 1 ) {
        print "\n";
        print_reservation_request_compartments($reservation_request);
        print "\n";

        if ( !defined($reservation_request->{'compartments'}) ) {
            $reservation_request->{'compartments'} = [];
        }
        my $compartments_count = @{$reservation_request->{'compartments'}};

        my $actions = [
            'new' => 'Add new requested compartment',
        ];
        if ( $compartments_count ) {
            push($actions, 'modify' => 'Modify existing requested compartment');
            push($actions, 'remove' => 'Remove existing requested compartment');
        }
        push($actions, 'stop' => 'Stop modifying requested compartments');

        my $action = dialog_select('Select action', ordered_hash_ref($actions));
        switch ( $action ) {
            case 'new' {
                my $compartment = create_reservation_request_compartment();
                if ( defined($compartment) ) {
                    push($reservation_request->{'compartments'}, $compartment);
                }
            }
            case 'modify' {
                if ( $compartments_count > 0 ) {
                    my $index = dialog_get_choice("Type a number of requested compartment", $compartments_count);
                    modify_reservation_request_compartment($reservation_request->{'compartments'}->[$index - 1]);
                }
            }
            case 'remove' {
                if ( $compartments_count > 0 ) {
                    my $index = dialog_get_choice("Type a number of requested compartment", $compartments_count);
                    splice($reservation_request->{'compartments'}, $index - 1, 1);
                }
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

    if ( !defined($reservation_request->{'slots'}) || @{$reservation_request->{'slots'}} == 0) {
        dialog_error("Requested slots should not be empty!");
        return 0;
    }
    if ( !defined($reservation_request->{'compartments'}) || @{$reservation_request->{'compartments'}} == 0) {
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
    $reservation_request{'type'} = dialog_select('Select reservation type', \%enum_type, $attributes{'type'});
    $reservation_request{'name'} = dialog_get('Name of the reservation', 1, '..', $attributes{'name'});
    $reservation_request{'purpose'} = dialog_select('Select reservation purpose', \%enum_purpose, $attributes{'purpose'});
    modify_reservation_request_slots(\%reservation_request);

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