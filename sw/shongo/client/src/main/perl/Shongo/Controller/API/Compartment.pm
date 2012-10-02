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

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);

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
        $virtualRooms->{'add'}($string);
    }

    my $connections = $attributes->{'add_collection'}('Connections');
    foreach my $connection (@{$self->{'connections'}}) {
        my $string = sprintf("from %s to %s", $connection->{'endpointFrom'}, $connection->{'endpointTo'});
        if ( $connection->{'class'} eq 'CompartmentReservation.ConnectionByAddress' ) {
            $string .= sprintf("\nby address %s in technology %s", $connection->{'address'},
                $Shongo::Controller::API::DeviceResource::Technology->{$connection->{'technology'}});
        } elsif ( $connection->{'class'} eq 'CompartmentReservation.ConnectionByAlias' ) {
            $string .= sprintf("\nby alias %s", $connection->{'alias'}->to_string());
        }
        $connections->{'add'}($string);
    }
}

1;