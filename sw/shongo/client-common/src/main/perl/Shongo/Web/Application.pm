#
# Web application.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Web::Application;

use strict;
use warnings;
use Shongo::Common;

#
# Create a new instance of object.
#
# @static
#
sub new
{
    my $class = shift;
    my ($cgi, $template) = @_;
    my $self = {};
    bless $self, $class;

    $self->{cgi} = $cgi;
    $self->{template} = $template;
    $self->{controller} = {};

    return $self;
}

sub add_action
{
    my ($self, $controller, $action, $action_callback) = @_;
    if ( !defined($self->{controller}->{$controller}) ) {
        $self->{controller}->{$controller} = {};
    }
    $self->{controller}->{$controller}->{$action} = $action_callback;
}

sub add_controller
{
    my ($self, $controller) = @_;
    $self->{controller}->{$controller->get_name()} = $controller;
}

sub error_action
{
    my ($self, $error) = @_;

    $error =~ s/\n/<br>/g;

    select STDOUT;
    print $self->{'cgi'}->header(type => 'text/html');
    print $self->render_template('error.html', {
        error => $error
    });
    print "\n";
    exit(0);
}

#
# Run web client
#
sub run
{
    my ($self, $location) = @_;

    # Parse $controller and $action from absolute url
    my $controller = 'index';
    my $action = 'index';
    if ( !defined($location) ) {
        $location = $self->{cgi}->url(-absolute=>1);
    }
    if ( $location =~ /([^\/]+)(\/([^\/]+))?/ ) {
        if ( defined($1) ) {
            $controller = $1;
        }
        if ( defined($3) ) {
            $action = $3;
        }
    }

    # Get controller
    my $controller_instance = $self->{controller}->{$controller};
    if ( !defined($controller_instance) ) {
        $self->error_action("Undefined controller '$controller'!");
        return;
    }

    # Invoke action by callback
    if ( ref($controller_instance) eq 'HASH' ) {
        if ( defined($controller_instance->{$action}) ) {
            $controller_instance->{$action}();
            return;
        }
    }
    # Invoke action by in controller instance
    else {
        my $method_name = $action;
        $method_name =~ s/-/_/;
        $method_name .= '_action';
        if ( $controller_instance->can($method_name) ) {
            $controller_instance->$method_name();
            return;
        }
    }

    $self->error_action("Undefined action '$action' in controller '$controller'!");
}

sub render_page
{
    my ($self, $title, $file, $parameters) = @_;

    # Render given file content
    if ( !defined($parameters) ) {
        $parameters = {};
    }
    $parameters->{'title'} = $title;
    my $content = $self->render_template($file, $parameters);

    # Render layout with the rendered content
    print $self->render_template('layout.html', {
        title => $title,
        content => $content
    });
    print("\n");
}

sub render_template
{
    my ($self, $file, $parameters) = @_;
    my $result = undef;
    if ( $self->{template}->process($file, $parameters, \$result) ) {
        return $result;
    }
    return undef;
}

1;