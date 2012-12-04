#
# Controller.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Web::Controller;

use strict;
use warnings;
use Shongo::Common;
use Data::FormValidator;

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

#
# @return hash of params
#
sub get_params
{
    my ($self) = @_;
    my $hash = $self->{'application'}->{'cgi'}->Vars;
    my $params = {};
    foreach my $name (keys %{$hash}) {
        $params->{$name} = $hash->{$name};
    }
    return $params;
}

#
# @param $name
# @return value of param $name
#
sub get_param
{
    my ($self, $name) = @_;
    my $value = $self->{'application'}->{'cgi'}->param($name);
    if ( !defined($value) ) {
        $value = $self->{'application'}->{'cgi'}->url_param($name);
        if ( !defined($value) ) {
            my $keywords = $self->{'application'}->{'cgi'}->url_param('keywords');
            if ( defined($keywords) && array_value_exists($name, $keywords) ) {
                return 1;
            }
        }
    }
    return $value;
}

#
# @param $name
# @return value of param $name
#
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
# Pre dispatch $action.
#
# @param $action
# @return 1 if $action should be dispatch, 0 if application should exit
#
sub pre_dispatch
{
    my ($self, $action) = @_;
    return 1;
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

#
# @see Shongo::Web::Application::render_page_content
#
sub render_page_content
{
    my ($self, $title, $content) = @_;
    $self->{'application'}->render_page_content($title, $content);
}

#
# Redirect to given $url
#
# @param $url
#
sub redirect
{
    my ($self, $url) = @_;
    if ( !($url =~ /^\//) ) {
        $url = $self->get_location() . '/' . $url;
    }
    print $self->{'application'}->redirect($url);
}

#
# Quit application and report error
#
# @param $error
#
sub error
{
    my ($self, $error) = @_;
    $self->{'application'}->error_action($error);
}

#
# Validate form
#
# @param $data     form data to be validated
# @param $profile  profile for Data::FormValidator
# @return hash of errors (field => error_message)
#
sub validate_form
{
    my ($self, $data, $profile) = @_;

    my $validators = {
        'number' => qr/^\d+$/,
        'datetime' => qr/^\d\d\d\d-\d\d-\d\d(T\d\d:\d\d)?$/,
        'date' => qr/^\d\d\d\d-\d\d-\d\d$/,
        'time' => qr/^\d\d:\d\d$/,
        'period' => qr/^[pP](\d+[yY])?(\d+[mM])?(\d+[wW])?(\d+[dD])?([tT](\d+[hH])?(\d+[mM])?(\d+[sS])?)?$/
    };
    foreach my $field (keys %{$profile->{constraint_methods}}) {
        my $constraint = $profile->{constraint_methods}->{$field};
        if ( defined($validators->{$constraint}) ) {
            $profile->{constraint_methods}->{$field} = {
                name => $constraint,
                constraint_method => $validators->{$constraint}
            };
        }
    }

    $profile->{'msgs'} = {
        format => '<div class="error">* %s</div>',
        constraints => {
            'number' => 'Not a valid number',
            'date' => 'Not a valid date (e.g., 2012-02-25)',
            'time' => 'Not a valid time (e.g., 14:01)',
            'period' => 'Not a valid period (e.g., PT2H)'
        },
    };

    my $results = Data::FormValidator->check($data, $profile);
    my $errors = $results->msgs();
    return $errors;
}

1;