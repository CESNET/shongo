#
# Compartment of video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Compartment;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::DeviceResource;

# States
our $State = {
    'NOT_STARTED' => {'title' => 'Not-Started', 'color' => 'yellow'},
    'STARTED' => {'title' => 'Started', 'color' => 'green'},
    'FINISHED' => {'title' => 'Finished', 'color' => 'blue'}
};
our $VirtualRoomState = {
    'NOT_CREATED' => {'title' => 'Not-Created', 'color' => 'yellow'},
    'CREATED' => {'title' => 'Created', 'color' => 'green'},
    'FAILED' => {'title' => 'Failed', 'color' => 'red'},
    'DELETED' => {'title' => 'Deleted', 'color' => 'blue'}
};
our $ConnectionState = {
    'NOT_ESTABLISHED' => {'title' => 'Not-Established', 'color' => 'yellow'},
    'ESTABLISHED' => {'title' => 'Established', 'color' => 'green'},
    'FAILED' => {'title' => 'Failed', 'color' => 'red'},
    'CLOSED' => {'title' => 'Closed', 'color' => 'blue'}
};

#
# Create a new instance of alias
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Compartment');
    $self->set_object_name('Compartment');
    $self->add_attribute('identifier');
    $self->add_attribute('state', {
        'format' => sub {
            my ($attribute_value) = @_;
            return format_state($attribute_value, $State);
        }
    });
    $self->add_attribute('slot', {
        'type' => 'interval'
    });
    $self->add_attribute(
        'endpoints', {
            'type' => 'collection',
            'collection' => {
                'format' => sub {
                    my ($endpoint) = @_;
                    my $string = $endpoint->{'description'};
                    foreach my $alias (@{$endpoint->{'aliases'}}) {
                        $string .= sprintf("\nwith assigned %s", trim($alias->to_string()));
                        $string =~ s/\n$//g;
                    }
                    return $string;
                }
            },
            'display' => 'newline'
        }
    );
    $self->add_attribute(
        'virtualRooms', {
            'title' => 'Rooms',
            'type' => 'collection',
            'collection' => {
                'format' => sub {
                    my ($virtualRoom) = @_;
                    my $string = $virtualRoom->{'description'} . " for " . $virtualRoom->{'portCount'} . " ports";
                    foreach my $alias (@{$virtualRoom->{'aliases'}}) {
                        $string .= sprintf("\nwith assigned %s", $alias->to_string_short());
                        $string =~ s/\n$//g;
                    }
                    $string .= "\nstate: " . format_state($virtualRoom->{'state'}, $VirtualRoomState);
                    return $string;
                }
            },
            'display' => 'newline'
        }
    );
    $self->add_attribute(
        'connections', {
            'type' => 'collection',
            'collection' => {
                'format' => sub {
                    my ($connection) = @_;
                    my $endpointFrom = $self->get_endpoint($connection->{'endpointFromIdentifier'});
                    my $endpointTo = $self->get_endpoint($connection->{'endpointToIdentifier'});
                    my $string = sprintf("from %s to %s", $endpointFrom->{'description'}, $endpointTo->{'description'});
                    if ( $connection->{'class'} eq 'Compartment.ConnectionByAddress' ) {
                        $string .= sprintf("\nby address %s in technology %s", $connection->{'address'},
                            $Shongo::Controller::API::DeviceResource::Technology->{$connection->{'technology'}});
                    } elsif ( $connection->{'class'} eq 'Compartment.ConnectionByAlias' ) {
                        $string .= sprintf("\nby alias %s", trim($connection->{'alias'}->to_string_short()));
                    }
                    $string .= "\nstate: " . format_state($connection->{'state'}, $ConnectionState);
                    return $string;
                }
            },
            'display' => 'newline'
        }
    );

    return $self;
}

#
# @param $identifier of endpoint
# @return endpoint with given $identifier
#
sub get_endpoint
{
    my ($self, $identifier) = @_;
    foreach my $endpoint (@{$self->{'endpoints'}}) {
        if ( $endpoint->{'identifier'} eq $identifier ) {
            return $endpoint;
        }
    }
    foreach my $virtualRoom (@{$self->{'virtualRooms'}}) {
        if ( $virtualRoom->{'identifier'} eq $identifier ) {
            return $virtualRoom;
        }
    }
    return undef;
}

#
# Format given $state to string.
#
# @param $state
# @param $types
#
sub format_state
{
    my ($state, $types) = @_;
    $state = $types->{$state};
    my $title = $state->{'title'};
    if ( defined($state->{'color'}) ) {
        $title = colored($title, $state->{'color'});
    }
    return '[' . $title . ']';
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);

    $attributes->{'add'}('Identifier', $self->{'identifier'});
    $attributes->{'add'}('State', format_state($self->{'state'}, $State));
    $attributes->{'add'}('Slot', format_interval($self->{'slot'}));

    my $endpointByIdentifier = {};
    my $endpoints = $attributes->{'add_collection'}('Endpoints');
    foreach my $endpoint (@{$self->{'endpoints'}}) {
        my $string = $endpoint->{'description'};
        foreach my $alias (@{$endpoint->{'aliases'}}) {
            $string .= sprintf("\nwith assigned %s", trim($alias->to_string()));
            $string =~ s/\n$//g;
        }
        $endpoints->{'add'}($string);
        $endpointByIdentifier->{$endpoint->{'identifier'}} = $endpoint;
    }

    my $virtualRooms = $attributes->{'add_collection'}('Virtual Rooms');
    foreach my $virtualRoom (@{$self->{'virtualRooms'}}) {
        my $string = $virtualRoom->{'description'} . " for " . $virtualRoom->{'portCount'} . " ports";
        foreach my $alias (@{$virtualRoom->{'aliases'}}) {
            $string .= sprintf("\nwith assigned %s", $alias->to_string());
            $string =~ s/\n$//g;
        }
        $string .= "\nstate: " . format_state($virtualRoom->{'state'}, $VirtualRoomState);
        $virtualRooms->{'add'}($string);
        $endpointByIdentifier->{$virtualRoom->{'identifier'}} = $virtualRoom;
    }
    my $connections = $attributes->{'add_collection'}('Connections');
    foreach my $connection (@{$self->{'connections'}}) {
        my $endpointFrom = $endpointByIdentifier->{$connection->{'endpointFromIdentifier'}};
        my $endpointTo = $endpointByIdentifier->{$connection->{'endpointToIdentifier'}};
        my $string = sprintf("from %s to %s", $endpointFrom->{'description'}, $endpointTo->{'description'});
        if ( $connection->{'class'} eq 'Compartment.ConnectionByAddress' ) {
            $string .= sprintf("\nby address %s in technology %s", $connection->{'address'},
                $Shongo::Controller::API::DeviceResource::Technology->{$connection->{'technology'}});
        } elsif ( $connection->{'class'} eq 'Compartment.ConnectionByAlias' ) {
            $string .= sprintf("\nby alias %s", trim($connection->{'alias'}->to_string()));
        }
        $string .= "\nstate: " . format_state($connection->{'state'}, $ConnectionState);
        $connections->{'add'}($string);
    }
}

1;