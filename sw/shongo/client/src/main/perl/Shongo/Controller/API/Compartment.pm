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

    return $self;
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Compartment";
}

# States
our $CompartmentState = {
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
# Get state
#
sub get_state
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

    $attributes->{'add'}('State', get_state($self->{'state'}, $CompartmentState));
    $attributes->{'add'}('Slot', format_interval($self->{'slot'}));

    my $endpoints = $attributes->{'add_collection'}('Endpoints');
    foreach my $endpoint (@{$self->{'endpoints'}}) {
        my $string = $endpoint->{'description'};
        foreach my $alias (@{$endpoint->{'aliases'}}) {
            $string .= sprintf("\nwith assigned %s", trim($alias->to_string()));
            $string =~ s/\n$//g;
        }
        $endpoints->{'add'}($string);
    }

    my $virtualRooms = $attributes->{'add_collection'}('Virtual Rooms');
    foreach my $virtualRoom (@{$self->{'virtualRooms'}}) {
        my $string = $virtualRoom->{'description'};
        foreach my $alias (@{$virtualRoom->{'aliases'}}) {
            $string .= sprintf("\nwith assigned %s", $alias->to_string());
            $string =~ s/\n$//g;
        }
        $string .= "\nstate: " . get_state($virtualRoom->{'state'}, $VirtualRoomState);
        $virtualRooms->{'add'}($string);
    }

    my $connections = $attributes->{'add_collection'}('Connections');
    foreach my $connection (@{$self->{'connections'}}) {
        my $string = sprintf("from %s to %s", $connection->{'endpointFrom'}, $connection->{'endpointTo'});
        if ( $connection->{'class'} eq 'CompartmentReservation.ConnectionByAddress' ) {
            $string .= sprintf("\nby address %s in technology %s", $connection->{'address'},
                $Shongo::Controller::API::DeviceResource::Technology->{$connection->{'technology'}});
        } elsif ( $connection->{'class'} eq 'CompartmentReservation.ConnectionByAlias' ) {
            $string .= sprintf("\nby alias %s", trim($connection->{'alias'}->to_string()));
        }
        $string .= "\nstate: " . get_state($connection->{'state'}, $ConnectionState);
        $connections->{'add'}($string);
    }
}

1;