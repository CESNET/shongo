#
# Alias for video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Alias;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::DeviceResource;

# Enumeration of alias types
our $Type = ordered_hash(
    'E164' => 'E.164 Phone Number',
    'IDENTIFIER' => 'String Identifier',
    'URI' => 'URI'
);

# Regular expression patters for type values
our $TypePattern = {
    'E164' => '\\d+',
    'IDENTIFIER' => '.+',
    'URI' => '.+@.+'
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

    $self->set_object_class('Alias');
    $self->set_object_name('Alias');
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
            'enum' =>  $Type
        }
    );
    $self->add_attribute(
        'value', {
            'required' => 1,
            'type' => 'string',
            'string-pattern' =>  sub {
                if ( !defined($self->get('type')) ) {
                    return undef;
                }
                return $TypePattern->{$self->get('type')};
            }
        }
    );

    return $self;
}

#
# Modify collection of aliases
#
# @static
#
sub modify_aliases
{
    my ($aliases) = @_;

    console_action_loop(
        sub {
            console_print_text(get_aliases(${$aliases}));
        },
        sub {
            my @actions = (
                'Add new alias' => sub {
                    my $alias = Shongo::Controller::API::Alias->new();
                    $alias = $alias->create();
                    if ( defined($alias) ) {
                        add_collection_item($aliases, $alias);
                    }
                    return undef;
                }
            );
            if ( get_collection_size(${$aliases}) > 0 ) {
                push(@actions, 'Modify existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, get_collection_size(${$aliases}));
                    if ( defined($index) ) {
                        get_collection_item(${$aliases}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing alias' => sub {
                    my $index = console_read_choice("Type a number of alias", 0, get_collection_size(${$aliases}));
                    if ( defined($index) ) {
                        remove_collection_item($aliases, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying aliases' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# Format collection of aliases
#
# @static
#
sub get_aliases
{
    my ($aliases) = @_;

    my $collection = Shongo::Controller::API::ObjectOld::create_collection('Aliases');
    for ( my $index = 0; $index < get_collection_size($aliases); $index++ ) {
        my $alias = get_collection_item($aliases, $index);
        $collection->{'add'}($alias);
    }
    return $collection;
}

1;