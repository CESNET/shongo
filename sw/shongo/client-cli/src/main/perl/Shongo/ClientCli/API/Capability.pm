#
# Capability for a device resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Capability;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::DeviceResource;
use Shongo::ClientCli::API::Alias;
use Shongo::ClientCli::API::ValueProvider;

#
# Capability types
#
our $Type = ordered_hash(
    'ValueProviderCapability' => 'Value Provider',
    'AliasProviderCapability' => 'Alias Provider',
    'TerminalCapability' => 'Terminal',
    'StandaloneTerminalCapability' => 'Standalone Terminal',
    'RoomProviderCapability' => 'Room Provider',
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
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_name('Capability');

    return $self;
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
        case ['TerminalCapability', 'StandaloneTerminalCapability'] {
            $self->add_attribute('aliases', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Alias',
                    'class' => 'Shongo::ClientCli::API::Alias',
                    'short' => 1
                }
            });
        }
        case 'RoomProviderCapability' {
            $self->add_attribute('licenseCount', {
                'title' => 'Number of Licenses',
                'required' => 1,
                'type' => 'int'
            });
            $self->add_attribute('requiredAliasTypes', {
                'title' => 'Required Alias Types',
                'type' => 'collection',
                'item' => {
                    'title' => 'Alias Type',
                    'enum' => $Shongo::ClientCli::API::Alias::Type
                 }
            });
        }
        case 'ValueProviderCapability' {
            $self->add_attribute('patterns', {
                'type' => 'collection',
                    'item' => {
                    'title' => 'Pattern',
                    'add' => sub {
                        console_read_value('Pattern', 1);
                    }
                },
                'display-empty' => 1,
                'complex' => 0,
                'required' => 1
            });
        }
        case 'AliasProviderCapability' {
            $self->add_attribute(
                'aliases', {
                    'type' => 'collection',
                    'item' => {
                        'title' => 'Alias',
                        'class' => 'Shongo::ClientCli::API::Alias',
                        'short' => 1
                    }
                }
            );
            $self->add_attribute('valueProvider', {
                'title' => 'Value Provider',
                'format' => sub {
                    my ($valueProvider) = @_;
                    if ( ref($valueProvider) ) {
                        return $valueProvider->to_string();
                    }
                    else {
                        return $valueProvider;
                    }
                },
                'modify' => sub {
                    my ($attribute_value) = @_;
                    my $valueProvider = undef;
                    if ( ref($attribute_value) ) {
                        $valueProvider = 1;
                    }
                    elsif ( defined($attribute_value) ) {
                        $valueProvider = 0;
                    }

                    $valueProvider = console_edit_enum('Select type of value provider', ordered_hash(
                        0 => 'Other Resource',
                        1 => 'Definition'), $valueProvider
                    );
                    if ( $valueProvider == 0 ) {
                        $attribute_value = console_edit_value('Resource identifier', 1, $Shongo::Common::IdPattern, $attribute_value);
                    } else {
                        if ( !defined($attribute_value) ) {
                            $attribute_value = Shongo::ClientCli::API::ValueProvider->create();
                        }
                        else {
                            $attribute_value->modify();
                        }
                    }
                    return $attribute_value;
                },
                'required' => 1
            });
            $self->add_attribute(
                'restrictedToResource', {
                    'title' => 'Restricted to Owner',
                    'type' => 'bool'
                }
            );
            $self->add_attribute(
                'permanentRoom', {
                    'title' => 'Permanent Room',
                    'type' => 'bool'
                }
            );
        }
    }
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    return console_read_enum('Select type of capability', $Type, $attributes->{'class'});
}

1;