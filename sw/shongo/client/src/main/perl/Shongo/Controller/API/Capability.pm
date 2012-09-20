#
# Capability for a device resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Capability;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::DeviceResource;
use Shongo::Controller::API::Alias;

#
# Capability types
#
our $Type = ordered_hash(
    'TerminalCapability' => 'Terminal',
    'StandaloneTerminalCapability' => 'Standalone Terminal',
    'VirtualRoomsCapability' => 'Virtual Rooms',
    'AliasProviderCapability' => 'Alias Provider'
);

#
# Create a new instance of capability
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# Get count of aliases
#
sub get_aliases_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'aliases'});
}

#
# Create a new capability from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    my $capability = console_read_enum('Select type of capability', $Type);
    if ( defined($capability) ) {
        $self->{'class'} = $capability;
        $self->modify();
        return $self;
    }
    return undef;
}

#
# Modify the capability
#
sub modify()
{
    my ($self) = @_;

    switch ($self->{'class'}) {
        case ['TerminalCapability', 'StandaloneTerminalCapability'] {
            $self->modify_aliases();
            return $self;
        }
        case 'VirtualRoomsCapability' {
            $self->{'portCount'} = console_edit_value('Maximum number of ports', 0, '\\d+', $self->{'portCount'});
        }
        case 'AliasProviderCapability' {
            $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::DeviceResource::Technology, $self->{'technology'});
            $self->{'type'} = console_edit_enum("Select alias type", $Shongo::Controller::API::Alias::Type, $self->{'type'});
            $self->{'pattern'} = console_edit_value('Pattern', 0, '.+', $self->{'pattern'});
            $self->{'restrictedToOwnerResource'} = console_edit_bool('Restricted only to owner resource', 0, $self->{'restrictedToOwnerResource'});
        }
    }
}

#
# Modify aliases for the capability
#
sub modify_aliases()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            console_print_text($self->get_aliases());
        },
        sub {
            my @actions = (
                'Add new alias' => sub {
                    my $alias = Shongo::Controller::API::Alias->new();
                    $alias = $alias->create();
                    if ( defined($alias) ) {
                        add_collection_item(\$self->{'aliases'}, $alias);
                    }
                    return undef;
                }
            );
            if ( $self->get_aliases_count() > 0 ) {
                push(@actions, 'Modify existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, $self->get_aliases_count());
                    if ( defined($index) ) {
                        get_collection_item(\$self->{'aliases'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, $self->get_aliases_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'aliases'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying aliases' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

# @Override
sub get_name
{
    my ($self) = @_;
    if ( defined($self->{'class'}) && exists $Type->{$self->{'class'}} ) {
        return $Type->{$self->{'class'}};
    } else {
        return "Capability";
    }
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);

    switch ($self->{'class'}) {
        case ['TerminalCapability', 'StandaloneTerminalCapability'] {
            if ( $self->get_aliases_count() > 0 ) {
                $attributes->{'add_collection'}($self->get_aliases());
            }
        }
        case 'VirtualRoomsCapability' {
            $attributes->{'add'}('Port Count', $self->{'portCount'});
        }
        case 'AliasProviderCapability' {
            $attributes->{'add'}('Technology', $Shongo::Controller::API::DeviceResource::Technology->{$self->{'technology'}});
            $attributes->{'add'}('Type', $Shongo::Controller::API::Alias::Type->{$self->{'type'}});
            $attributes->{'add'}('Pattern', $self->{'patern'});
        }
    }
}

#
# Format aliases to string
#
sub get_aliases
{
    my ($self) = @_;

    my $collection = $self->create_collection('Aliases');
    for ( my $index = 0; $index < $self->get_aliases_count(); $index++ ) {
        my $alias = get_collection_item($self->{'aliases'}, $index);
        $collection->{'add'}($alias);
    }
    return $collection;
}

1;