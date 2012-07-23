#
# Reservation class - Management of reservations.
#
package Shongo::Controller::API::Resource;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;

use Shongo::Common;
use Shongo::Console;

# Enumeration of technologies
our $Technology = ordered_hash('H323' => 'H.323', 'SIP' => 'SIP', 'ADOBE_CONNECT' => 'Adobe Connect');

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->{'technologies'} = [];
    $self->{'capabilities'} = [];

    return $self;
}

#
# Get count of requested compartments in reservation request
#
sub get_capabilities_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'capabilities'});
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->{'name'} = $attributes->{'name'};
    $self->modify_attributes(0);

    # Parse technologies
    if ( defined($attributes->{'technology'}) ) {
        for ( my $index = 0; $index < @{$attributes->{'technology'}}; $index++ ) {
            my $technology = $attributes->{'technology'}->[$index];
            if ( defined($Technology->{$technology}) ) {
                add_collection_item(\$self->{'technologies'}, $technology);
            } else {
                console_print_error("Illegal technology '%s' was specified!", $technology);
            }
        }
    }

    # Parse capabilities
    if ( defined($attributes->{'capability'}) ) {
        for ( my $index = 0; $index < @{$attributes->{'capability'}}; $index++ ) {
            my $capability = $attributes->{'capability'}->[$index];
            if ($capability =~ /(.+)\((.+)\)/) {
                my $class = $1;
                my $params = $2;
                if ( $class eq "VirtualRoomsCapability" && $params =~ /\d+/) {
                    add_collection_item(\$self->{'capabilities'}, {'class' => $class, 'portCount' => $params});
                } else {
                    console_print_error("Illegal capability '%s' was specified!", $capability);
                }
            }
        }
    }

    while ( $self->modify_loop('creation of resource') ) {
        console_print_info("Creating resource...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Resource.createResource',
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

    while ( $self->modify_loop('modification of resource') ) {
        console_print_info("Modifying resource...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Resource.modifyResource',
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
            my $actions = [
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                }
            ];
            append_technologies_actions($actions, \$self->{'technologies'});
            push($actions, 'Add new capability' => sub {
                my $capability = console_read_enum('Select type of capability', ordered_hash('VIRTUAL_ROOMS' => 'Virtual Rooms'));
                if ( defined($capability) ) {
                    switch ($capability) {
                    	case 'VIRTUAL_ROOMS' {
                    	    my $portCount = console_read_value('Maximum number of ports', 0, '\\d+');
                    	    $capability = {'class' => 'VirtualRoomsCapability', 'portCount' => $portCount};
                    	}
                    	else {
                    	    $capability = undef;
                    	}
                    }
                    if ( defined($capability) ) {
                        add_collection_item(\$self->{'capabilities'}, $capability);
                    }
                }
                return undef;
            });
            if ( $self->get_capabilities_count() > 0 ) {
                push($actions, 'Remove existing capability' => sub {
                    my $index = console_read_choice("Type a number of capability", 0, $self->get_capabilities_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'capabilities'}, $index - 1);
                    }
                    return undef;
                });
            }
            push($actions, (
                'Confirm ' . $message => sub {
                    return 1;
                },
                'Cancel ' . $message => sub {
                    return 0;
                }
            ));
            return ordered_hash($actions);
        }
    );
}


sub modify_attributes()
{
    my ($self, $edit) = @_;

    $self->{'name'} = console_auto_value($edit, 'Name of the resource', 1, undef, $self->{'name'});
    if (!$edit) {
        return;
    }
    $self->{'schedulable'} = console_edit_bool('Schedulable', 0, $self->{'schedulable'});
}

#
# Append actions for modifying technologies
#
# @static
#
sub append_technologies_actions()
{
    my ($actions, $technologies) = @_;

    # Get available technologies
    my $available_technologies = [];
    my %technologies_hash = map { $_ => 1 } @{get_collection_items(${$technologies})};
    foreach my $key (ordered_hash_keys($Technology)) {
        if ( !exists($technologies_hash{$key}) ) {
            push($available_technologies, $key => $Technology->{$key});
        }
    }
    if ( get_collection_size($available_technologies) > 0 ) {
        push($actions, 'Add new technology' => sub {
            my $technology = console_read_enum('Select technology', ordered_hash($available_technologies));
            if ( defined($technology) ) {
                add_collection_item($technologies, $technology);
            }
            return undef;
        });
    }
    if ( get_collection_size(${$technologies}) > 0 ) {
        push($actions, 'Remove existing technology' => sub {
            my $index = console_read_choice("Type a number of technology", 0, get_collection_size(${$technologies}));
            if ( defined($index) ) {
                remove_collection_item($technologies, $index - 1);
            }
            return undef;
        });
    }
}

#
# Validate the reservation request
#
sub validate()
{
    my ($self) = @_;

    if ( $self->get_capabilities_count() == 0 ) {
        console_print_error("Capabilities should not be empty.");
        return 0;
    }
    return 1;
}

#
# Convert object to string
#
sub to_string()
{
    my ($self) = @_;

    my $string = " RESOURCE\n";
    if ( defined($self->{'identifier'}) ) {
        $string .= "  Identifier: $self->{'identifier'}\n";
    }
    $string .= "        Name: $self->{'name'}\n";
    $string .= " Schedulable: $self->{'schedulable'}\n";
    $string .= technologies_to_string($self->{'technologies'});
    $string .= $self->capabilities_to_string();

    return $string;
}

sub technologies_to_string
{
    my ($technologies) = @_;

    my $string = " Technologies:\n";
    my $technologies_count = get_collection_size($technologies);
    if ( $technologies_count > 0 ) {
        for ( my $index = 0; $index < $technologies_count; $index++ ) {
            my $technology = get_collection_item($technologies, $index);
            $string .= sprintf("   %d) %s\n", $index + 1, $Technology->{$technology});
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

#
# Convert requested slots to string
#
sub capabilities_to_string()
{
    my ($self) = @_;

    my $string = " Capabilities:\n";
    if ( $self->get_capabilities_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_capabilities_count(); $index++ ) {
            my $capability = get_collection_item($self->{'capabilities'}, $index);
            my $description = '';
            switch ($capability->{'class'}) {
            	case 'VirtualRoomsCapability' {
            	    $description .= sprintf("portCount: %s", $capability->{'portCount'});
            	}
            	else {
            	}
            }
           $string .= sprintf("   %d) %s %s\n", $index + 1, $capability->{'class'}, $description);
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;