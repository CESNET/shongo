#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::DeviceResource;
use base qw(Shongo::Controller::API::Resource);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Capability;

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
    my $self = Shongo::Controller::API::Resource->new(@_);
    bless $self, $class;

    $self->set_object_class('DeviceResource');
    $self->add_attribute('address');
    $self->add_attribute('mode', {
        'format' => sub {
            my $mode = '';
            if ( $self->get('mode') eq 'UNMANAGED' ) {
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
        }
    });
    $self->add_attribute(
        'technologies', {
            'type' => 'collection',
            'collection-title' => 'Technology',
            'collection-add' => sub {
                my $available_technologies = [];
                my %technologies_hash = map { $_ => 1 } @{get_collection_items($self->get('technologies'))};
                foreach my $key (ordered_hash_keys($Technology)) {
                    if ( !exists($technologies_hash{$key}) ) {
                        push(@{$available_technologies}, $key => $Technology->{$key});
                    }
                }
                return console_read_enum('Select technology', ordered_hash($available_technologies));
            },
            'required' => 1
        }
    );

    return $self;
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
            push(@{$available_technologies}, $key => $Technology->{$key});
        }
    }
    if ( get_collection_size($available_technologies) > 0 ) {
        push(@{$actions}, 'Add new technology' => sub {
            my $technology = console_read_enum('Select technology', ordered_hash($available_technologies));
            if ( defined($technology) ) {
                add_collection_item($technologies, $technology);
            }
            return undef;
        });
    }
    if ( get_collection_size(${$technologies}) > 0 ) {
        push(@{$actions}, 'Remove existing technology' => sub {
            my $index = console_read_choice("Type a number of technology", 0, get_collection_size(${$technologies}));
            if ( defined($index) ) {
                remove_collection_item($technologies, $index - 1);
            }
            return undef;
        });
    }
}

1;