#
# Alias for video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Alias;
use base qw(Shongo::Controller::API::ObjectOld);

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
    my $self = Shongo::Controller::API::ObjectOld->new(@_);
    bless $self, $class;

    return $self;
}

#
# Create a new alias
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->modify();

    return $self;
}

#
# Modify the alias
#
sub modify()
{
    my ($self) = @_;

    $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::DeviceResource::Technology, $self->{'technology'});
    $self->{'type'} = console_edit_enum("Select type of alias", $Type, $self->{'type'});
    $self->{'value'} = console_edit_value("Alias value", 1, $TypePattern->{$self->{'type'}}, $self->{'value'});
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Alias";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'single_line'} = 1;
    $attributes->{'add'}('Value', $self->{'value'});
    $attributes->{'add'}('Technology', $Shongo::Controller::API::DeviceResource::Technology->{$self->{'technology'}});
    $attributes->{'add'}('Type', $Type->{$self->{'type'}});
}

# @Override
sub to_string_short
{
    my ($self) = @_;
    return $self->{'value'};
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