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

    $self->{'class'} = 'DeviceResource';
    $self->{'technologies'} = [];

    return $self;
}

#
# On create
#
sub on_create()
{
    my ($self, $attributes) = @_;

    # Parse technologies
    if ( defined($attributes->{'technology'}) ) {
        for ( my $index = 0; $index < @{$attributes->{'technology'}}; $index++ ) {
            my $technology = $attributes->{'technology'}->[$index];
            if ( defined($Technology->{$technology}) ) {
                add_collection_item(\$self->{'technologies'}, $technology);
            } else {
                console_print_error("Illegal technology '%s' was specified!", $technology);
            }
        }
    }

    return Shongo::Controller::API::Resource::on_create(@_);
}

#
# On modify
#
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    append_technologies_actions($actions, \$self->{'technologies'});
}

# @Override
sub modify_attributes()
{
    my ($self, $edit) = @_;

    Shongo::Controller::API::Resource::modify_attributes(@_);

    $self->{'address'} = console_edit_value('Address', 0, undef, $self->{'address'});

    my $mode = 0;
    if ( ref($self->{'mode'}) ) {
        $mode = 1;
    }
    $mode = console_edit_enum('Select mode', ordered_hash(0 => 'Unmanaged', 1 => 'Managed'), $mode);
    if ( $mode == 0 ) {
        $self->{'mode'} = 'UNMANAGED';
    } else {
        my $connectorAgentName = undef;
        if ( ref($self->{'mode'}) eq 'HASH' ) {
            $connectorAgentName = $self->{'mode'}->{'connectorAgentName'};
        }
        $connectorAgentName = console_edit_value('Connector agent name', 1, undef, $connectorAgentName);
        $self->{'mode'} = {'connectorAgentName' => $connectorAgentName};
    }
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

# @Override
sub get_name
{
    my ($self) = @_;
    return "Device Resource";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Address', $self->{'address'});

    my $mode = '';
    if ( $self->{'mode'} eq 'UNMANAGED' ) {
        $mode = 'Unmanaged';
    } elsif ( ref($self->{'mode'}) eq 'HASH' ) {
        $mode = 'Managed(' . $self->{'mode'}->{'connectorAgentName'} . ')';
    }
    $attributes->{'add'}('Mode', $mode);

    $attributes->{'add_collection'}($self->get_technologies());
}

# @Override
sub to_string_collections
{
    my ($self) = @_;
    my $string = "";
    $string .= technologies_to_string($self->{'technologies'});
    $string .= Shongo::Controller::API::Resource::to_string_collections(@_);
    return $string;
}

#
# Format technologies to string
#
sub get_technologies
{
    my ($self) = @_;

    my $collection = $self->create_collection('Technologies');
    for ( my $index = 0; $index < get_collection_size($self->{'technologies'}); $index++ ) {
        my $technology = get_collection_item($self->{'technologies'}, $index);
        $collection->{'add'}($Technology->{$technology});
    }
    return $collection;
}

1;