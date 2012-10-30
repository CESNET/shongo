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
            $self->add_attribute(
                'aliases', {
                    'type' => 'collection',
                    'collection' => {
                        'title' => 'Alias',
                        'class' => 'Shongo::Controller::API::Alias',
                        'short' => 1
                    }
                }
            );
        }
        case 'VirtualRoomsCapability' {
            $self->add_attribute(
                'portCount', {
                    'title' => 'Maximum Number of Ports',
                    'required' => 1,
                    'type' => 'int'
                }
            );
        }
        case 'AliasProviderCapability' {
            $self->add_attribute(
                'technology', {
                    'required' => 1,
                    'type' => 'enum',
                    'enum' =>  $Shongo::Controller::API::DeviceResource::Technology
                }
            );
            $self->add_attribute(
                'type', {
                    'required' => 1,
                    'type' => 'enum',
                    'enum' =>  $Shongo::Controller::API::Alias::Type
                }
            );
            $self->add_attribute(
                'patterns', {
                    'type' => 'collection',
                    'collection' => {
                        'title' => 'Pattern',
                        'add' => sub {
                            console_read_value('Pattern', 1);
                        }
                    },
                    'display-empty' => 1,
                    'complex' => 0,
                    'required' => 1
                }
            );
            $self->add_attribute(
                'restrictedToOwnerResource', {
                    'title' => 'Restricted to Owner',
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