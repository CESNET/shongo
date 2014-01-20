#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Specification;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Alias;
use Shongo::ClientCli::API::DeviceResource;
use Shongo::ClientCli::API::Person;
use Shongo::ClientCli::API::CompartmentSpecification;
use Shongo::ClientCli::API::Participant;
use Shongo::ClientCli::API::RoomAvailability;
use Shongo::ClientCli::API::RoomEstablishment;
use Shongo::ClientCli::API::RoomSettings;

#
# Specification types
#
our $Type = ordered_hash(
    'ResourceSpecification' => 'Resource',
    'CompartmentSpecification' => 'Compartment',
    'MultiCompartmentSpecification' => 'Multi-Compartment',
    'ValueSpecification' => 'Value',
    'AliasSpecification' => 'Alias',
    'AliasSetSpecification' => 'Alias Set',
    'RoomSpecification' => 'Room'
);

#
# Call initiation
#
our $CallInitiation = ordered_hash(
    NULL() => 'Default',
    'TERMINAL' => 'Terminal',
    'VIRTUAL_ROOM' => 'Virtual Room'
);

#
# Alias type for specification
#
our $Technology = $Shongo::ClientCli::API::DeviceResource::Technology;
our $AliasType = $Shongo::ClientCli::API::Alias::Type;

#
# Create a new instance of specification
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return specification class
#
sub select_type($)
{
    my ($type) = @_;

    return console_edit_enum('Select type of specification', $Type, $type);
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    my $specification = $attributes->{'class'};
    if ( !defined($specification) ) {
        $specification = $self->select_type();
    }
    if ( defined($specification) ) {
        $self->set_object_class($specification);
    }
}

# @Override
sub on_init()
{
    my ($self) = @_;

    my $class = $self->get_object_class();
    if ( !defined($class) ) {
        return;
    }

    if ( exists $Type->{$class} ) {
        $self->set_object_name($Type->{$class});
    }

    switch ($class) {
        case 'MultiCompartmentSpecification' {
            $self->add_attribute('compartmentSpecifications', {
                'type' => 'collection',
                'item' => {
                    'title' => 'compartment',
                    'class' => 'Shongo::ClientCli::API::CompartmentSpecification',
                },
                'complex' => 0,
                'display' => 'newline'
            });
        }
        case 'CompartmentSpecification' {
            $self->add_attribute('callInitiation', {
                'title' => 'Call Initiation',
                'type' => 'enum',
                'enum' => $Shongo::ClientCli::API::Specification::CallInitiation
            }, NULL());
            $self->add_attribute('participants', {
                'type' => 'collection',
                'item' => {
                    'title' => 'participant',
                    'class' => 'Shongo::ClientCli::API::Participant',
                },
                'complex' => 0,
                'display' => 'newline'
            });
        }
        case 'ResourceSpecification' {
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern,
                'required' => 1
            });
        }
        case 'ValueSpecification' {
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern
            });
            $self->add_attribute('values', {
                'title' => 'Values',
                'type' => 'collection',
                'item' => {
                    'title' => 'value',
                    'add' => sub {
                        console_read_value('Value', 1);
                     },
                },
                'complex' => 0,
                'required' => 1
            });
        }
        case 'AliasSpecification' {
            $self->add_attribute('aliasTypes', {
                'title' => 'Alias Types',
                'type' => 'collection',
                'item' => {
                    'title' => 'Alias Type',
                    'enum' => $AliasType
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('resourceId', {
                'title' => 'Alias Provider Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern
            });
            $self->add_attribute('value', {
                'title' => 'Requested Value'
            });
        }
        case 'AliasSetSpecification' {
            $self->add_attribute('aliasSpecifications', {
                'title' => 'Aliases',
                'type' => 'collection',
                'item' => {
                    'title' => 'alias',
                    'class' => 'AliasSpecification',
                },
                'complex' => 1,
                'required' => 1
            });
        }
        case 'RoomSpecification' {
            $self->add_attribute('establishment', {
                'title' => 'Establishment',
                'type' => 'class',
                'class' => 'Shongo::ClientCli::API::RoomEstablishment',
                'complex' => 1,
            });
            $self->add_attribute('availability', {
                'title' => 'Availability',
                'type' => 'class',
                'class' => 'Shongo::ClientCli::API::RoomAvailability',
                'complex' => 1,
            });
            $self->add_attribute('roomSettings', {
                'title' => 'Room Settings',
                'display' => 'newline',
                'complex' => 1,
                'format' => sub() {
                    my ($room_settings) = @_;
                    Shongo::ClientCli::API::RoomSettings::format_room_settings($room_settings);
                },
                'modify' => sub() {
                    my ($room_settings) = @_;
                    return Shongo::ClientCli::API::RoomSettings::modify_room_settings($room_settings);
                }
            });
            $self->add_attribute('participants', {
                'title' => 'Participants',
                'type' => 'collection',
                'item' => {
                    'title' => 'participant',
                    'class' => 'Participant',
                },
                'complex' => 1
            });
        }
    }
}

1;