#
# Controller.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Web::Controller;

use strict;
use warnings;
use Shongo::Common;

#
# Create a new instance of controller.
#
# @param $name         name of the controller (is used as location, e.g. "my-super-controller")
# @param $application  web application
# @static
#
sub new
{
    my $class = shift;
    my ($name, $application) = @_;
    my $self = {};
    bless $self, $class;

    $self->{'name'} = $name;
    $self->{'application'} = $application;

    return $self;
}

#
# @return name of the controller which is used as location
#
sub get_name
{
    my ($self) = @_;
    return $self->{'name'};
}

#
# @return base of the controller
#
sub get_location
{
    my ($self) = @_;
    return '/' . $self->get_name();
}

sub get_param
{
    my ($self, $name) = @_;
    return $self->{'application'}->{'cgi'}->param($name);
}

sub get_param_required
{
    my ($self, $name) = @_;
    my $value = $self->get_param($name);
    if ( !defined($value) ) {
        $self->{'application'}->error_action("Param '$name' was not present and is required.");
    }
    return $value;
}

#
# @see Shongo::Web::Application::render_page
#
sub render_page
{
    my ($self, $title, $file, $parameters) = @_;
    $parameters->{'location'} = $self->get_location();
    $self->{'application'}->render_page($title, $file, $parameters);
}

1;