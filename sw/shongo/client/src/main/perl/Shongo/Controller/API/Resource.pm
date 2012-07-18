#
# Reservation class - Management of reservations.
#
package Shongo::Controller::API::Resource;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

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

    $self->{'name'} = undef;
    $self->{'capabilities'} = [];

    return $self;
}

#
# Get count of requested compartments in reservation request
#
sub get_capabilities_count()
{
    my ($self) = @_;
    return $self->get_collection_size('compartments');
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->{'name'} = $attributes->{'name'};
    $self->modify_attributes(0);

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
                },
                'Add new capability' => sub {
                    console_print_info("TODO:");
                    return undef;
                }
            ];
            if ( $self->get_capabilities_count() > 0 ) {
                push($actions, 'Remove existing capability' => sub {
                    my $index = console_read_choice("Type a number of capability", 0, $self->get_capabilities_count());
                    if ( defined($index) ) {
                        $self->remove_collection_item('capabilities', $index - 1);
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
        $string .= " Identifier: $self->{'identifier'}\n";
    }
    $string .= "       Name: $self->{'name'}\n";
    $string .= $self->capabilities_to_string();

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
            my $capability = $self->get_collection_item('capabilities', $index);
            var_dump($capability);
            #$string .= sprintf("   %d) at %s for %s\n", $index + 1, $start, $duration);
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;