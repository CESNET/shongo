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
    'RecordingCapability' => 'Recording',
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
            $self->add_attribute('maxLicencesPerRoom', {
                'title' => 'Maximum Licences Per Room',
                'required' => 0,
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
            $self->add_attribute('valueProvider', {
                'title' => 'Value Provider',
                'type' => 'class',
                'class' => 'Shongo::ClientCli::API::ValueProvider',
                'required' => 1
            });
        }
        case 'AliasProviderCapability' {
            $self->add_attribute('aliases', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Alias',
                    'class' => 'Shongo::ClientCli::API::Alias',
                    'short' => 1
                }
            });
            $self->add_attribute('valueProvider', {
                'title' => 'Value Provider',
                'complex' => 1,
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
                    my $value_provider = undef;
                    if ( ref($attribute_value) ) {
                        $value_provider = $attribute_value->{'class'};
                    }
                    elsif ( defined($attribute_value) ) {
                        $value_provider = NULL();
                    }

                    my $new_value_provider = console_edit_enum('Select type of value provider',
                        ordered_hash_merge({ NULL() => 'Other Resource' }, $Shongo::ClientCli::API::ValueProvider::Type),
                        $value_provider
                    );
                    if ( $new_value_provider ne $value_provider ) {
                        $attribute_value = undef;
                        $value_provider = $new_value_provider;
                    }
                    if ( $value_provider eq NULL() ) {
                        $attribute_value = console_edit_value('Resource identifier', 1, $Shongo::Common::IdPattern, $attribute_value);
                    }
                    else {
                        if ( !defined($attribute_value) ) {
                            $attribute_value = Shongo::ClientCli::API::ValueProvider->create({'class' => $value_provider});
                        }
                        else {
                            $attribute_value->modify();
                        }
                    }
                    return $attribute_value;
                },
                'required' => 1
            });
            $self->add_attribute('maximumFuture', {
                'title' => 'Maximum Future',
                'type' => 'period'
            });
            $self->add_attribute('restrictedToResource', {
                'title' => 'Restricted to Owner',
                'type' => 'bool'
            });
        }
        case 'RecordingCapability' {
            $self->add_attribute('licenseCount', {
                'title' => 'Number of Licenses',
                'required' => 0,
                'type' => 'int'
            });
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