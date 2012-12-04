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

#
# Add new action to application (location will be /$controller/$action).
#
# @param $controller       forms location
# @param $action           forms location (can be omitted and it defaults to "index")
# @param $action_callback  will be called when user requests the location
#
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

#
# Add controller to application which contains methods *_action which then forms actions.
#
# @param $controller
#
sub add_controller
{
    my ($self, $controller) = @_;
    $self->{controller}->{$controller->get_name()} = $controller;
}

#
# Action which prints information about error.
#
# @param $error  error message which should be displayed
#
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

#
# Render http headers.
#
sub render_headers
{
    my ($self) = @_;
    $self->{'session'}->param('previous_url', $self->{cgi}->url(-absolute => 1, -query => 1));
    my $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    print $self->{'cgi'}->header(-type => 'text/html', -charset => 'UTF-8', -cookie => $cookie);
}

#
# Render page by using Template.
#
# @param $title       title of the page
# @param $file        html file which should be rendered by Template
# @param $parameters  for rendering by Template
#
sub render_page
{
    my ($self, $title, $file, $parameters) = @_;

    # Render given file content
    if ( !defined($parameters) ) {
        $parameters = {};
    }
    $parameters->{'title'} = $title;
    my $content = $self->render_template($file, $parameters);
    $self->render_page_content($title, $content, $parameters->{'options'});
}

#
# Render page by it's content (Template is not used).
#
# @param $title    title of the page
# @param $content  content of the page
# @param $options  options for the layout
#
sub render_page_content
{
    my ($self, $title, $content, $options) = @_;

    my $params = {};
    var_dump();
    $params->{'title'} = $title;
    $params->{'content'} = $content;
    $params->{'options'} = $options;
    $params->{'session'} = {};
    foreach my $name ($self->{'session'}->param()) {
        $params->{'session'}->{$name} = $self->{'session'}->param($name);
    }

    # Render layout with the rendered content
    print $self->render_template('layout.html', $params);
    print("\n");
}

#
# Render template by Template module.
#
# @param $file        which should be rendered by Template.
# @param $parameters  which should be used for rendering by Template.
# @return rendered string
#
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
    my ($self, $url, $query, $disable_set_as_previous) = @_;
    if ( !defined($url) && defined($self->{'session'}->param('previous_url')) ) {
        $url = $self->{'session'}->param('previous_url');
    }
    if ( !($url =~ /^(\/|http)/) ) {
        $url = '/' . $url;
    }
    $url = URI->new($url);
    if ( defined($query) ) {
        $url->query_form($query);
    }
    if ( !defined($disable_set_as_previous) || !$disable_set_as_previous ) {
        $self->{'session'}->param('previous_url', $self->{cgi}->url(-absolute => 1, -query => 1));
    }
    my $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    print $self->{'cgi'}->redirect(-uri => $url->as_string(), -cookie => $cookie);
}

1;