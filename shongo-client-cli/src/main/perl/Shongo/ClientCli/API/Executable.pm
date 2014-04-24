#
# Compartment of virtual rooms
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Executable;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::DeviceResource;

# States
our $State = {
    'NOT_STARTED' => {'title' => 'Not-Started', 'color' => 'yellow'},
    'STARTED' => {'title' => 'Started', 'color' => 'green'},
    'STARTING_FAILED' => {'title' => 'Failed', 'color' => 'red'},
    'STOPPED' => {'title' => 'Finished', 'color' => 'blue'},
    'STOPPING_FAILED' => {'title' => 'Stopping Failed', 'color' => 'red'},
};
our $RoomState = {
    'NOT_STARTED' => {'title' => 'Not-Created', 'color' => 'yellow'},
    'STARTED' => {'title' => 'Created', 'color' => 'green'},
    'STARTING_FAILED' => {'title' => 'Failed', 'color' => 'red'},
    'STOPPED' => {'title' => 'Deleted', 'color' => 'blue'},
    'STOPPING_FAILED' => {'title' => 'Deleting Failed', 'color' => 'red'},
};
our $ConnectionState = {
    'NOT_STARTED' => {'title' => 'Not-Established', 'color' => 'yellow'},
    'STARTED' => {'title' => 'Established', 'color' => 'green'},
    'STARTING_FAILED' => {'title' => 'Failed', 'color' => 'red'},
    'STOPPED' => {'title' => 'Closed', 'color' => 'blue'},
    'STOPPING_FAILED' => {'title' => 'Closing Failed', 'color' => 'red'},
};

#
# Capability types
#
our $Type = ordered_hash(
    'CompartmentExecutable' => 'Compartment',
    'RoomExecutable' => 'Room',
    'UsedRoomExecutable' => 'Used Room'
);

our $Technology = $Shongo::ClientCli::API::DeviceResource::Technology;

#
# Create a new instance of alias
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

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

    $self->add_attribute('id', {'title' => 'Identifier'});
    $self->add_attribute('reservationId', {'title' => 'Reservation'});
    $self->add_attribute('state', {
        'format' => sub {
            my ($attribute_value) = @_;
            my $state = '[' . format_state($attribute_value, $State) . ']';
            if ( defined($state) ) {
                my $color = 'blue';
                if ( defined($self->get('state')) && $self->get('state') eq 'STARTING_FAILED'  || $self->get('state') eq 'STOPPING_FAILED') {
                    $color = 'red';
                }
                my $state_report = $self->{'stateReport'};
                $state_report = format_report($state_report, get_term_width() - 23);
                $state .= "\n" . colored($state_report, $color);
                return $state;
            }
            return undef;
        },
    });
    $self->add_attribute('slot', {
        'type' => 'interval'
    });
    switch ($class) {
        case 'CompartmentExecutable' {
            $self->add_attribute(
                'endpoints', {
                    'type' => 'collection',
                    'item' => {
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
                'rooms', {
                    'title' => 'Rooms',
                    'type' => 'collection',
                    'item' => {
                        'format' => sub {
                            my ($roomEndpoint) = @_;
                            my $string = "virtual room (in " . $roomEndpoint->{'resourceId'} . ") for " . $roomEndpoint->{'licenseCount'} . " licenses";
                            foreach my $alias (@{$roomEndpoint->{'aliases'}}) {
                                $string .= sprintf("\nwith assigned %s", $alias->to_string_short());
                                $string =~ s/\n$//g;
                            }
                            $string .= "\nstate: [" . format_state($roomEndpoint->{'state'}, $RoomState) . ']';
                            return $string;
                        }
                    },
                    'display' => 'newline'
                }
            );
            $self->add_attribute(
                'connections', {
                    'type' => 'collection',
                    'item' => {
                        'format' => sub {
                            my ($connection) = @_;
                            my $endpointFrom = $self->get_endpoint($connection->{'endpointFromId'});
                            my $endpointTo = $self->get_endpoint($connection->{'endpointToId'});
                            if ( $endpointFrom->{'class'} eq 'RoomExecutable' ) {
                                $endpointFrom->{'description'} = "virtual room (in " . $endpointFrom->{'resourceId'} . ")";
                            }
                            if ( $endpointTo->{'class'} eq 'RoomExecutable' ) {
                                $endpointTo->{'description'} = "virtual room (in " . $endpointTo->{'resourceId'} . ")";
                            }
                            my $string = sprintf("from %s to %s", $endpointFrom->{'description'}, $endpointTo->{'description'});
                            $string .= sprintf("\nby alias %s", trim($connection->{'alias'}->to_string_short()));
                            $string .= "\nstate: [" . format_state($connection->{'state'}, $ConnectionState) . ']';
                            return $string;
                        }
                    },
                    'display' => 'newline'
                }
            );
        }
        case 'RoomExecutable' {
            $self->add_attribute(
                'licenseCount', {
                    'title' => 'Number of Licenses'
                }
            );
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier'
            });
            $self->add_attribute('roomId', {
                'title' => 'Room Identifier'
            });
            $self->add_attribute('aliases', {
                'type' => 'collection',
                'item' => {
                    'short' => 1
                }
            });
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Technology
                }
            });
        }
        case 'UsedRoomExecutable' {
            $self->add_attribute(
                'reusedRoomExecutableId', {
                    'title' => 'Used Room'
                }
            );
            $self->add_attribute(
                'licenseCount', {
                    'title' => 'Number of Licenses'
                }
            );
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier'
            });
            $self->add_attribute('aliases', {
                'type' => 'collection',
                'item' => {
                    'short' => 1
                }
            });
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Technology
                }
            });
        }
    }
    $self->add_attribute('migratedExecutable', {
        'title' => 'Migrated From',
        'format' => sub {
            my ($attribute_value) = @_;
            return $attribute_value->{'id'};
        },
    });

    switch ($class) {
        case ['RoomExecutable', 'UsedRoomExecutable'] {
            $self->add_attribute(
                'participantConfiguration', {
                    'title' => 'Participants',
                    'format' => sub {
                        my ($attribute_value) = @_;
                        return $self->format_value($attribute_value->{'participants'});
                    }
                }
            );
        }
    }

    return $self;
}

#
# @param $id of endpoint
# @return endpoint with given $id
#
sub get_endpoint
{
    my ($self, $id) = @_;
    foreach my $endpoint (@{$self->{'endpoints'}}) {
        if ( $endpoint->{'id'} eq $id) {
            return $endpoint;
        }
    }
    foreach my $room (@{$self->{'rooms'}}) {
        if ( $room->{'id'} eq $id) {
            return $room;
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
    return $title;
}

1;