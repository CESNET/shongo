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
use Shongo::Controller::API::Resource;
use Shongo::Controller::API::Alias;

#
# Capability types
#
our $Type = ordered_hash(
    'TerminalCapability' => 'Terminal',
    'StandaloneTerminalCapability' => 'Standalone Terminal',
    'VirtualRoomsCapability' => 'Virtual Rooms'
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

    return $self;
}

#
# Get count of aliases
#
sub get_aliases_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'aliases'});
}

#
# Create a new capability from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    my $capability = console_read_enum('Select type of capability', $Type);
    if ( defined($capability) ) {
        $self->{'class'} = $capability;
        $self->modify();
        return $self;

    }
    return undef;
}

#
# Modify the capability
#
sub modify()
{
    my ($self) = @_;

    switch ($self->{'class'}) {
        case ['TerminalCapability', 'StandaloneTerminalCapability'] {
            $self->modify_aliases();
            return $self;
        }
        case 'VirtualRoomsCapability' {
            $self->{'portCount'} = console_read_value('Maximum number of ports', 0, '\\d+');
        }
    }
}

#
# Modify aliases for the capability
#
sub modify_aliases()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            printf("\n%s\n", $self->aliases_to_string());
        },
        sub {
            my $actions = [
                'Add new alias' => sub {
                    my $alias = Shongo::Controller::API::Alias->new();
                    $alias = $alias->create();
                    if ( defined($alias) ) {
                        add_collection_item(\$self->{'aliases'}, $alias);
                    }
                    return undef;
                }
            ];
            if ( $self->get_aliases_count() > 0 ) {
                push($actions, 'Modify existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, $self->get_aliases_count());
                    if ( defined($index) ) {
                        get_collection_item(\$self->{'aliases'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push($actions, 'Remove existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, $self->get_aliases_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'aliases'}, $index - 1);
                    }
                    return undef;
                });
            }
            push($actions, 'Finish modifying aliases' => sub {
                return 0;
            });
            return ordered_hash($actions);
        }
    );
}

# @Override
sub to_string()
{
    my ($self) = @_;

    my $string = $Type->{$self->{'class'}} . ' ';
    switch ($self->{'class'}) {
        case ['TerminalCapability', 'StandaloneTerminalCapability'] {
            if ( $self->get_aliases_count() > 0 ) {
                $string .= 'aliases: ';
                for ( my $index = 0; $index < $self->get_aliases_count(); $index++ ) {
                    my $alias = get_collection_item($self->{'aliases'}, $index);
                    if ( $index > 0 ) {
                        $string .= ', ';
                    }
                    $string .= sprintf("%s", $alias->{value});
                }

            }
        }
        case 'VirtualRoomsCapability' {
            $string .= sprintf("portCount: %s", $self->{'portCount'});
        }
        else {
            $string .= sprintf("unknown capability ");
        }
    }
    return $string;
}

#
# Format aliases to string
#
sub aliases_to_string
{
    my ($self) = @_;

    my $string = " Aliases:\n";
    if ( $self->get_aliases_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_aliases_count(); $index++ ) {
            my $alias = get_collection_item($self->{'aliases'}, $index);
            $string .= sprintf("   %d) %s\n", $index + 1, $alias->to_string());
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;