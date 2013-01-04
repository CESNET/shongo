#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::DeviceResource;
use base qw(Shongo::ClientCli::API::Resource);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Capability;

# Enumeration of technologies
our $Technology = $Shongo::Common::Technology;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Resource->new(@_);
    bless $self, $class;

    $self->set_object_class('DeviceResource');
    $self->set_object_name('Device Resource');
    $self->add_attribute('address');
    $self->add_attribute('mode', {
        'format' => sub {
            my $mode = '';
            if ( !defined($self->get('mode')) || $self->get('mode') eq 'UNMANAGED' ) {
                $mode = 'Unmanaged';
            } elsif ( ref($self->get('mode')) eq 'HASH' ) {
                $mode = 'Managed(' . $self->get('mode')->{'connectorAgentName'} . ')';
            }
            return $mode;
        },
        'modify' => sub {
            my ($attribute_value) = @_;
            my $mode = 0;
            if ( ref($attribute_value) ) {
                $mode = 1;
            }
            $mode = console_edit_enum('Select mode', ordered_hash(0 => 'Unmanaged', 1 => 'Managed'), $mode);
            if ( $mode == 0 ) {
                $attribute_value = 'UNMANAGED';
            } else {
                my $connectorAgentName = undef;
                if ( ref($self->get('mode')) eq 'HASH' ) {
                    $connectorAgentName = $self->get('mode')->{'connectorAgentName'};
                }
                $connectorAgentName = console_edit_value('Connector agent name', 1, undef, $connectorAgentName);
                $attribute_value = {'connectorAgentName' => $connectorAgentName};
            }
            return $attribute_value;
        },
        'required' => 1
    });
    $self->add_attribute(
        'technologies', {
            'type' => 'collection',
            'item' => {
                'title' => 'Technology',
                'enum' => $Technology
            },
            'required' => 1
        }
    );

    return $self;
}

1;