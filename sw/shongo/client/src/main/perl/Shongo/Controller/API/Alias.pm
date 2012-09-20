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

1;