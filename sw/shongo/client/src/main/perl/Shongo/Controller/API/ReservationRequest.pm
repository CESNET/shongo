#
# Reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Term::ANSIColor;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Compartment;

# Enumeration of reservation request type
our $Type = ordered_hash('NORMAL' => 'Normal', 'PERMANENT' => 'Permanent');

# Enumeration of reservation request purpose
our $Purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');

# Enumeration of request state
our $RequestState = ordered_hash(
    'NOT_COMPLETE' => 'Not Complete',
    'NOT_ALLOCATED' => 'Not Allocated',
    'ALLOCATED' => 'Allocated',
    'ALLOCATION_FAILED' => 'Allocation Failed'
);

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->{'slots'} = [];
    $self->{'compartments'} = [];
    $self->{'requests'} = [];

    $self->to_xml_skip_attribute('requests');

    return $self;
}

#
# Get count of requested slots in reservation request
#
sub get_slots_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'slots'});
}

#
# Get count of requested compartments in reservation request
#
sub get_compartments_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'compartments'});
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->{'type'} = $attributes->{'type'};
    $self->{'name'} = $attributes->{'name'};
    $self->{'purpose'} = $attributes->{'purpose'};
    $self->modify_attributes(0);

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
                    add_collection_item(\$self->{'slots'}, {
                        'start' => {'start' => $1, 'period' => $2},
                        'duration' => $duration
                    });
                }
            }
            elsif ($slot =~ /(.+),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                add_collection_item(\$self->{'slots'}, {'start' => $dateTime, 'duration' => $duration});
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
                    add_collection_item(\$compartment->{'resources'}, {'technology' => $technology, 'count' => $count});
                }
            }
        }
        if ( defined($attributes->{'person'}) ) {
            for ( my $index = 0; $index < @{$attributes->{'person'}}; $index++ ) {
                my $resource = $attributes->{'person'}->[$index];
                if ($resource =~ /(.+),(.*)/) {
                    my $name = $1;
                    my $email = $2;
                    add_collection_item(\$compartment->{'persons'}, {'name' => $name, 'email' => $email});
                }
            }
        }
        add_collection_item(\$self->{'compartments'}, $compartment);
    }

    if ( $self->get_slots_count() == 0 ) {
        console_print_info("Fill requested slots:");
        $self->modify_slots();
    }
    if ( $self->get_compartments_count() == 0 ) {
        console_print_info("Fill requested resources and/or persons:");
        my $compartment = Shongo::Controller::API::Compartment->create();
        if ( defined($compartment) ) {
            add_collection_item(\$self->{'compartments'}, $compartment);
        }
    }

    while ( $self->modify_loop('creation of reservation request') ) {
        console_print_info("Creating reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.createReservationRequest',
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

    while ( $self->modify_loop('modification of reservation request') ) {
        console_print_info("Modifying reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.modifyReservationRequest',
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return;
        }
    }
}

#
# Run modify loop
#
sub modify_loop()
{
    my ($self, $message) = @_;
    console_action_loop(
        sub {
            printf("\n%s\n", $self->to_string());
        },
        sub {
            my @actions = (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                },
                'Modify requested slots' => sub {
                    $self->modify_slots();
                    return undef;
                }
            );
            if ( $self->get_compartments_count() > 0 ) {
                push(@actions, 'Modify first compartment' => sub {
                    get_collection_item($self->{'compartments'}, 0)->modify();
                    return undef;
                });
            }
            push(@actions, (
                'Modify requested compartments' => sub {
                    $self->modify_compartments();
                    return undef;
                },
                'Confirm ' . $message => sub {
                    return 1;
                },
                'Cancel ' . $message => sub {
                    return 0;
                }
            ));
            return ordered_hash(@actions);
        }
    );
}


sub modify_attributes()
{
    my ($self, $edit) = @_;

    $self->{'type'} = console_auto_enum($edit, 'Select reservation type', $Type, $self->{'type'});
    $self->{'name'} = console_auto_value($edit, 'Name of the reservation', 1, undef, $self->{'name'});
    $self->{'purpose'} = console_auto_enum($edit, 'Select reservation purpose', $Purpose, $self->{'purpose'});
}

#
# @param $slot to be modified
#
sub modify_slot($)
{
    my ($slot) = @_;

    if (ref($slot->{'start'}) && $slot->{'start'}->{'class'} eq 'PeriodicDateTime') {
        $slot->{'start'}->{'start'} = console_edit_value("Type a starting date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'}->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
        $slot->{'start'}->{'period'} = console_edit_value("Type a period", 0, $Shongo::Common::PeriodPattern, $slot->{'start'}->{'period'});
        $slot->{'start'}->{'end'} = console_edit_value("Ending date/time", 0, $Shongo::Common::DateTimePartialPattern, $slot->{'start'}->{'end'});
    } else {
        $slot->{'start'} = console_edit_value("Type a date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
    }
}

#
# Modify requested slots in the reservation request
#
sub modify_slots()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            printf("\n%s\n", $self->slots_to_string());
        },
        sub {
            my @actions = (
                'Add new requested slot by absolute date/time' => sub {
                    my $slot = {};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $slot = {'start' => {'class' => 'PeriodicDateTime'}};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                }
            );
            if ( $self->get_slots_count() > 0 ) {
                push(@actions, 'Modify existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        modify_slot(get_collection_item($self->{'slots'}, $index - 1));
                    }
                    return undef;
                });
                push(@actions, 'Remove existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'slots'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying requested slots' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# Modify compartment in the reservation request
#
sub modify_compartments
{
    my ($self) = @_;

    console_action_loop(
        sub {
            printf("\n%s\n", $self->compartments_to_string());
        },
        sub {
            my @actions = (
                'Add new requested compartment' => sub {
                    my $compartment = Shongo::Controller::API::Compartment->create();
                    if ( defined($compartment) ) {
                        add_collection_item(\$self->{'compartments'}, $compartment);
                    }
                    return undef;
                }
            );
            if ( $self->get_compartments_count() > 0 ) {
                push(@actions, 'Modify existing requested compartment' => sub {
                    my $index = console_read_choice("Type a number of requested compartment", 0, $self->get_compartments_count());
                    if ( defined($index) ) {
                        get_collection_item($self->{'compartments'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing requested compartment' => sub {
                    my $index = console_read_choice("Type a number of requested compartment", 0, $self->get_compartments_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'compartments'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying requested compartments' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
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
    $string .= "       Type: $Type->{$self->{'type'}}\n";
    $string .= "       Name: $self->{'name'}\n";
    $string .= "    Purpose: $Purpose->{$self->{'purpose'}}\n";
    $string .= $self->slots_to_string();
    $string .= $self->compartments_to_string();

    my $request_count = get_collection_size($self->{'requests'});
    if ( $request_count > 0 ) {
        $string .= " Created requests:\n";
        for ( my $index = 0; $index < $request_count; $index++ ) {
            my $processedSlots = get_collection_item($self->{'requests'}, $index);
            my $slot = $processedSlots->{'slot'};
            my $state = $RequestState->{$processedSlots->{'state'}};
            $string .= sprintf("   %d) %s (%s)\n", $index + 1, format_interval($slot), $state);

            my $stateDescription = $processedSlots->{'stateDescription'};
            if ( defined($stateDescription) ) {
                my $color = 'blue';
                if ( $processedSlots->{'state'} eq 'ALLOCATION_FAILED' ) {
                    $color = 'red';
                }
                $stateDescription =~ s/\n/\n      /g;
                $stateDescription =~ s/\n      $/\n/g;
                $string .= sprintf("      %s\n", colored($stateDescription, $color));
            }
        }
    }

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
            my $slot = get_collection_item($self->{'slots'}, $index);
            my $start = $slot->{'start'};
            my $duration = $slot->{'duration'};
            if ( ref($start) ) {
                my $startString = sprintf("(%s, %s", format_datetime($start->{'start'}), $start->{'period'});
                if ( defined($start->{'end'}) ) {
                    $startString .= ", " . format_datetime_partial($start->{'end'});
                }
                $startString .= ")";
                $start = $startString;
            } else {
                $start = format_datetime($start);
            }
            $string .= sprintf("   %d) at '%s' for '%s'\n", $index + 1, $start, $duration);
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
            my $compartment = get_collection_item($self->{'compartments'}, $index);
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