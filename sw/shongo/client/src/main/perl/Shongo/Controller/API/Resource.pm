#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Resource;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Capability;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->{'capabilities'} = [];

    $self->to_xml_skip_attribute('childResourceIdentifiers');

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

    $self->on_create($attributes);

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
# On create
#
sub on_create
{
    my ($self, $attributes) = @_;

    $self->{'name'} = $attributes->{'name'};
    $self->{'allocatable'} = 0;
    $self->{'maxFuture'} = 'P4M';
    $self->modify_attributes(0);

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
            my @actions = (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                }
            );
            $self->on_modify_loop(\@actions);
            push(@actions, 'Add new capability' => sub {
                my $capability = Shongo::Controller::API::Capability->new();
                $capability = $capability->create();
                if ( defined($capability) ) {
                    add_collection_item(\$self->{'capabilities'}, $capability);
                }
                return undef;
            });
            if ( $self->get_capabilities_count() > 0 ) {
                push(@actions, 'Modify existing capability' => sub {
                    my $index = console_read_choice("Type a number of capability", 0, $self->get_capabilities_count());
                    if ( defined($index) ) {
                        get_collection_item($self->{'capabilities'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing capability' => sub {
                    my $index = console_read_choice("Type a number of capability", 0, $self->get_capabilities_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'capabilities'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, (
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

#
# On modify loop
#
sub on_modify_loop()
{
    my ($self, $actions) = @_;
}

#
# Modify resource attributes
#
# @param $edit
#
sub modify_attributes
{
    my ($self, $edit) = @_;

    $self->{'name'} = console_auto_value($edit, 'Name of the resource', 1, undef, $self->{'name'});
    $self->{'description'} = console_edit_value('Description of the resource', 0, undef, $self->{'description'});
    $self->{'allocatable'} = console_edit_bool('Allocatable', 0, $self->{'allocatable'});
    if (!$edit) {
        return;
    }
    $self->{'parentIdentifier'} = console_edit_value('Parent resource identifier', 0,
        $Shongo::Common::IdentifierPattern, $self->{'parentIdentifier'});
    $self->{'maxFuture'} = console_edit_value('Maximum Future', 0,
        $Shongo::Common::DateTimePattern . '|' . $Shongo::Common::PeriodPattern, $self->{'maxFuture'});
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'capabilities' ) {
        return Shongo::Controller::API::Capability->new();
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

# @Override
sub to_string_name
{
    return "Resource";
}

# @Override
sub to_string_attributes
{
    my ($self) = @_;
    my $string = "";
    if ( defined($self->{'identifier'}) ) {
        $string .= "  Identifier: $self->{'identifier'}\n";
    }
    $string .= "        Name: $self->{'name'}\n";
    if ( defined($self->{'description'}) ) {
        $string .= " Description: $self->{'description'}\n";
    }
    if ( defined($self->{'parentIdentifier'}) ) {
        $string .= "      Parent: $self->{'parentIdentifier'}\n";
    }
    if ( defined($self->{'childResourceIdentifiers'}) && scalar(@{$self->{'childResourceIdentifiers'}}) > 0 ) {
        $string .= "    Children: ";
         my $index = 0;
         foreach my $identifier (@{$self->{'childResourceIdentifiers'}}) {
             if ( $index > 0 ) {
                 $string .= ', ';
             }
             $string .= $identifier;
             $index++;
         }
         $string .= "\n";
    }
    $string .= " Allocatable: $self->{'allocatable'}\n";
    if ( defined($self->{'maxFuture'}) ) {
        $string .= "  Max Future: $self->{'maxFuture'}\n";
    }
    return $string;
}

# @Override
sub to_string_collections
{
    my ($self) = @_;
    my $string = "";
    $string .= $self->capabilities_to_string();
    return $string;
}

#
# Format capabilities to string
#
sub capabilities_to_string
{
    my ($self) = @_;

    my $string = " Capabilities:\n";
    if ( $self->get_capabilities_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_capabilities_count(); $index++ ) {
            my $capability = get_collection_item($self->{'capabilities'}, $index);
            $string .= sprintf("   %d) %s \n", $index + 1, $capability->to_string());
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;