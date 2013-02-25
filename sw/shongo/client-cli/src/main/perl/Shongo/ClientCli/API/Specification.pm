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
use Shongo::ClientCli::API::ParticipantSpecification;

#
# Specification types
#
our $RootType = ordered_hash(
    'ResourceSpecification' => 'Resource',
    'CompartmentSpecification' => 'Compartment',
    'MultiCompartmentSpecification' => 'Multi-Compartment',
    'AliasSpecification' => 'Alias',
    'AliasSetSpecification' => 'Alias Set',
    'RoomSpecification' => 'Virtual Room'
);
our $ParticipantType = ordered_hash(
    'ExternalEndpointSpecification' => 'External Endpoint',
    'ExternalEndpointSetSpecification' => 'Set of External Endpoints',
    'ExistingEndpointSpecification' => 'Existing Endpoint',
    'LookupEndpointSpecification' => 'Lookup Resource',
    'PersonSpecification' => 'Person',
);
our $Type = ordered_hash_merge($RootType, $ParticipantType);

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

    return console_edit_enum('Select type of specification', $RootType, $type);
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
            $self->add_attribute('specifications', {
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
            $self->add_attribute('specifications', {
                'type' => 'collection',
                'item' => {
                    'title' => 'specification',
                    'class' => 'Shongo::ClientCli::API::ParticipantSpecification',
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
        case 'ExternalEndpointSpecification' {
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('alias', {
                'modify' => sub {
                    my $hasAlias = 0;
                    if ( defined($self->{'alias'}) ) {
                        $hasAlias = 1;
                    }
                    $hasAlias = console_edit_bool("Has alias", 1, $hasAlias);
                    if ( $hasAlias ) {
                        if ( !defined($self->{'alias'}) ) {
                            $self->{'alias'} = Shongo::ClientCli::API::Alias->create();
                        } else {
                            $self->{'alias'}->modify();
                        }
                    } else {
                        $self->{'alias'} = undef;
                    }
                    return $self->{'alias'};
                }
            });
        }
        case 'ExternalEndpointSetSpecification' {
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('count', {
                'type' => 'int',
                'required' => 1
            });
        }
        case 'ExistingEndpointSpecification' {
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern,
                'required' => 1
            });
        }
        case 'LookupEndpointSpecification' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::ClientCli::API::DeviceResource::Technology,
                'required' => 1
            });
        }
        case 'PersonSpecification' {
            $self->add_attribute('person', {
                'modify' => sub() {
                    my ($person) = @_;
                    if ( !defined($person) ) {
                        $person = Shongo::ClientCli::API::Person->new();
                    }
                    $person->modify();
                    return $person;
                },
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
            $self->add_attribute('sharedExecutable', {
                'title' => 'Shared Executable',
                'type' => 'bool',
                'required' => 1
            });
            $self->add_attribute('aliases', {
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
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('participantCount', {
                'title' => 'Participant Count',
                'type' => 'int',
                'required' => 1
            });
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern
            });
            $self->add_attribute('aliases', {
                'title' => 'Aliases',
                'type' => 'collection',
                'item' => {
                    'title' => 'alias',
                    'class' => 'AliasSpecification',
                },
                'complex' => 1
            });
        }
    }
}

1;