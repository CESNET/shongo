#
# Reservation request
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;
use Switch;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Compartment;

# Enumeration of reservation request type
our %Type = ordered_hash('NORMAL' => 'Normal', 'PERMANENT' => 'Permanent');

# Enumeration of reservation request purpose
our %Purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');

#
# Get count of requested slots in reservation request
#
sub get_slots_count()
{
    my ($self) = @_;
    return scalar(@{$self->{'slots'}});
}

#
# Get count of requested compartments in reservation request
#
sub get_compartments_count()
{
    my ($self) = @_;
    return scalar(@{$self->{'compartments'}});
}

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = {};
    bless $self, $class;

    $self->{'identifier'} = undef;
    $self->{'type'} = undef;
    $self->{'name'} = undef;
    $self->{'purpose'} = undef;
    $self->{'slots'} = [];
    $self->{'compartments'} = [];

    return $self;
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->{'type'} = console_select('Select reservation type', \%Type, $attributes->{'type'});
    $self->{'name'} = console_read('Name of the reservation', 1, undef, $attributes->{'name'});
    $self->{'purpose'} = console_select('Select reservation purpose', \%Purpose, $attributes->{'purpose'});

    # Parse requested slots
    if ( defined($attributes->{'slot'}) ) {
        for ( my $index = 0; $index < @{$attributes->{'slot'}}; $index++ ) {
            my $slot = $attributes->{'slot'}->[$index];
            my $result = 0;
            if ($slot =~ /\((.+)\),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                if ($dateTime =~ /(.+),(.*)/) {
                    $result = 1;
                    push($self->{'slots'}, {'start' => {'start' => $1, 'period' => $2}, 'duration' => $duration});
                }
            }
            elsif ($slot =~ /(.+),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                push($self->{'slots'}, {'start' => $dateTime, 'duration' => $duration});
                $result = 1;
            }
            if ( $result == 0 ) {
                console_print_error("Requested slot '%s' is in wrong format!", $slot);
                return;
            }
        }
    }

    # Parse requested compartment
    if ( defined($attributes->{'resource'}) || defined($attributes->{'person'}) ) {
        my $compartment = Shongo::Controller::API::Compartment->new();
        if ( defined($attributes->{'resource'}) ) {
            for ( my $index = 0; $index < @{$attributes->{'resource'}}; $index++ ) {
                my $resource = $attributes->{'resource'}->[$index];
                if ($resource =~ /(.+),(.*)/) {
                    my $technology = $1;
                    my $count = $2;
                    push($compartment->{'resources'}, {'technology' => $technology, 'count' => $count});
                }
            }
        }
        if ( defined($attributes->{'person'}) ) {
            for ( my $index = 0; $index < @{$attributes->{'person'}}; $index++ ) {
                my $resource = $attributes->{'person'}->[$index];
                if ($resource =~ /(.+),(.*)/) {
                    my $name = $1;
                    my $email = $2;
                    push($compartment->{'persons'}, {'name' => $name, 'email' => $email});
                }
            }
        }
        push($self->{'compartments'}, $compartment);
    }

    if ( $self->get_slots_count() == 0 ) {
        console_print_info("Fill requested slots:");
        $self->modify_slots();
    }
    if ( $self->get_compartments_count() == 0 ) {
        console_print_info("Fill requested resources and/or persons:");
        my $compartment = Shongo::Controller::API::Compartment->create();
        if ( defined($compartment) ) {
            push($self->{'compartments'}, $compartment);
        }
    }

    while ( $self->modify_loop('creation of reservation request') ) {
        console_print_info("Creating reservation request...");
        my $response = Shongo::Controller->instance()->request(
            'Reservation.createReservationRequest',
            RPC::XML::struct->new(),
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return $response->value();
        }
    }
    return undef;
}

#
# Modify the reservation request
#
sub modify()
{
    my ($self) = @_;

    $self->modify_loop('modification of reservation request');

    console_print_info("TODO: Modify");
}

#
# Run modify loop
#
sub modify_loop()
{
    my ($self, $message) = @_;

    while ( 1 ) {
        printf("\n%s\n", $self->to_string());

        my $action = console_select('Select action', ordered_hash_ref(
            'slots' => 'Modify requested slots',
            'compartments' => 'Modify requested compartments',
            'confirm' => 'Confirm ' . $message,
            'cancel' => 'Cancel ' . $message
        ));
        switch ( $action ) {
        	case 'slots' {
        	    $self->modify_slots();
        	}
        	case 'compartments' {
        	    $self->modify_compartments();
            }
            case 'confirm' {
                return 1;
            }
            else {
                return 0;
            }
        }
    }
}

#
# Modify requested slots in the reservation request
#
sub modify_slots()
{
    my ($self) = @_;

    while ( 1 ) {
        printf("\n%s\n", $self->slots_to_string());

        my $actions = [
            'new-absolute' => 'Add new requested slot by absolute date/time',
            'new-periodic' => 'Add new requested slot by periodic date/time'
        ];
        if ( $self->get_slots_count() > 0 ) {
            push($actions, 'remove' => 'Remove existing requested slot');
        }
        push($actions, 'stop' => 'Finish modifying requested slots');

        my $action = console_select('Select action', ordered_hash_ref($actions));
        switch ( $action ) {
            case 'new-absolute' {
                my $dateTime = console_read("Type a date/time"); # "\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d"
                my $duration = console_read("Type a slot duration");
                if ( defined($dateTime) && defined($duration) ) {
                    push($self->{'slots'}, {'start' => $dateTime, 'duration' => $duration});
                }
            }
            case 'new-periodic' {
                my $dateTime = console_read("Type a starting date/time");
                my $period = console_read("Type a period");
                my $duration = console_read("Type a slot duration");
                if ( defined($dateTime) && defined($period) && defined($duration) ) {
                    push($self->{'slots'}, {'start' => {'start' => $dateTime, 'period' => $period}, 'duration' => $duration});
                }
            }
            case 'remove' {
                my $index = console_read_choice("Type a number of requested slot", $self->get_slots_count());
                if ( defined($index) ) {
                    splice($self->{'slots'}, $index - 1, 1);
                }
            }
            else {
                return;
            }
        }
    }
}

#
# Modify compartment in the reservation request
#
sub modify_compartments
{
    my ($self) = @_;

    while ( 1 ) {
        printf("\n%s\n", $self->compartments_to_string());

        my $actions = [
            'new' => 'Add new requested compartment',
        ];
        if ( $self->get_compartments_count() > 0 ) {
            push($actions, 'modify' => 'Modify existing requested compartment');
            push($actions, 'remove' => 'Remove existing requested compartment');
        }
        push($actions, 'stop' => 'Finish modifying requested compartments');

        my $action = console_select('Select action', ordered_hash_ref($actions));
        switch ( $action ) {
            case 'new' {
                my $compartment = Shongo::Controller::API::Compartment->create();
                if ( defined($compartment) ) {
                    push($self->{'compartments'}, $compartment);
                }
            }
            case 'modify' {
                my $index = console_read_choice("Type a number of requested compartment", $self->get_compartments_count());
                if ( defined($index) ) {
                    $self->{'compartments'}->[$index - 1]->modify();
                }
            }
            case 'remove' {
                my $index = console_read_choice("Type a number of requested compartment", $self->get_compartments_count());
                if ( defined($index) ) {
                    splice($self->{'compartments'}, $index - 1, 1);
                }
            }
            else {
                return;
            }
        }
    }
}

#
# Validate the reservation request
#
sub validate()
{
    my ($self) = @_;

    if ( $self->get_slots_count() == 0 ) {
        console_print_error("Requested slots should not be empty.");
        return 0;
    }
    if ( $self->get_compartments_count() == 0 ) {
        console_print_error("Requested compartments should not be empty.");
        return 0;
    }
    for ( my $index = 0; $index < $self->get_compartments_count(); $index++ ) {
        my $compartment = $self->{'compartments'}->[$index];
        if ( $compartment->get_resources_count() == 0 && $compartment->get_persons_count() == 0 ) {
            console_print_error("Requested compartment should not be empty.");
            return 0;
        }
    }
    return 1;
}

#
# Convert object to string
#
sub to_string()
{
    my ($self) = @_;

    my $string = " RESERVATION REQUEST\n";
    if ( defined($self->{'identifier'}) ) {
        $string .= " Identifier: $self->{'identifier'}\n";
    }
    $string .= "       Type: $Type{$self->{'type'}}\n";
    $string .= "       Name: $self->{'name'}\n";
    $string .= "    Purpose: $Purpose{$self->{'purpose'}}\n";
    $string .= $self->slots_to_string();
    $string .= $self->compartments_to_string();

    return $string;
}

#
# Convert requested slots to string
#
sub slots_to_string()
{
    my ($self) = @_;

    my $string = " Requested slots:\n";
    if ( $self->get_slots_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_slots_count(); $index++ ) {
            my $slot = $self->{'slots'}->[$index];
            my $start = $slot->{'start'};
            my $duration = $slot->{'duration'};
            if ( ref($start) ) {
                $start = sprintf("(%s, %s)", $start->{'start'}, $start->{'period'});
            }
            $string .= sprintf("   %d) at %s for %s\n", $index + 1, $start, $duration);
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

#
# Convert requested compartments to string
#
sub compartments_to_string()
{
    my ($self) = @_;

    my $string = " Requested compartments:\n";
    if ( $self->get_compartments_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_compartments_count(); $index++ ) {
            my $compartment = $self->{'compartments'}->[$index];
            $string .= sprintf("   %d) Compartment (resources: %d, persons: %d)\n", $index + 1,
                $compartment->get_resources_count(), $compartment->get_persons_count());
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;