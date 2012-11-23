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
    my ($cgi, $template, $session) = @_;
    my $self = {};
    bless $self, $class;

    $self->{'cgi'} = $cgi;
    $self->{'template'} = $template;
    $self->{'session'} = $session;
    $self->{'controller'} = {};

    return $self;
}

sub add_action
{
    my ($self, $controller, $action, $action_callback) = @_;
    if ( !defined($action_callback) ) {
        $action_callback = $action;
        $action = 'index';
    }
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

    use Devel::StackTrace;
    my $stack_trace = '';
    $stack_trace .= '<table>';
    my $index = 0;
    foreach my $frame (Devel::StackTrace->new()->frames) {
        if ( !($frame->{'subroutine'} eq 'Devel::StackTrace::new') ) {
            $index++;
            my $filename = $frame->{'filename'};
            $filename =~ s/^\/.+\/main\/perl\///g;
            $filename =~ s/^\/.+\/src\/public\///g;
            $filename =~ s/^\/.+\/perl\d\///g;
            $filename =~ s/^\/.+\/perl\/\d+(.\d+)+\///g;
            $filename =~ s/([^\/]+(pm|pl))/<strong>$1<\/strong>/g;
            $stack_trace .= sprintf('<tr><td>%d. %s (%d) %s()<td></tr>',
                $index,
                $filename,
                $frame->{'line'},
                $frame->{'subroutine'}
            );
        }
    }
    $stack_trace .= '</table>';

    select STDOUT;

    print $self->{'cgi'}->header(type => 'text/html');
    var_dump();
    print $self->render_template('error.html', {
        error => $error,
        stackTrace => $stack_trace
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
    my $possible_location = $self->{cgi}->url(-absolute => 1);
    if ( length($possible_location) != 0 ) {
        $location = $possible_location;
    }

    if ( defined($location) && $location =~ /([^\/]+)(\/([^\/]+))?/ ) {
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

sub render_headers
{
    my ($self) = @_;
    $self->{'session'}->param('previous_url', $self->{cgi}->url(-absolute => 1, -query => 1));
    my $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    print $self->{'cgi'}->header(-type => 'text/html', -cookie => $cookie);
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
    $self->render_page_content($title, $content);
}

sub render_page_content
{
    my ($self, $title, $content) = @_;

    my $params = {};
    var_dump();
    $params->{'title'} = $title;
    $params->{'content'} = $content;
    $params->{'session'} = {};
    foreach my $name ($self->{'session'}->param()) {
        $params->{'session'}->{$name} = $self->{'session'}->param($name);
    }

    # Render layout with the rendered content
    print $self->render_template('layout.html', $params);
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

#
# Redirect to given $url
#
# @param $url
#
sub redirect
{
    my ($self, $url) = @_;
    if ( !defined($url) && defined($self->{'session'}->param('previous_url')) ) {
        $url = $self->{'session'}->param('previous_url');
    }
    if ( !($url =~ /^\//) ) {
        $url = '/' . $url;
    }
    $self->{'session'}->param('previous_url', $self->{cgi}->url(-absolute => 1, -query => 1));
    my $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    print $self->{'cgi'}->redirect(-uri => $url, -cookie => $cookie);
}

1;