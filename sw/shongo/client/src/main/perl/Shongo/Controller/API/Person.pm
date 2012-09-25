#
# Person
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Person;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of person
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

    $self->{'name'} = console_edit_value("Full Name", 1, undef, $self->{'name'});
    $self->{'email'} = console_edit_value("Email", 1, undef, $self->{'email'});
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Person";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'single_line'} = 1;
    $attributes->{'add'}('Full Name', $self->{'name'});
    $attributes->{'add'}('Email', $self->{'email'});
}

1;