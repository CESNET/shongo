#
#
# Web application.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Web::Application;

use strict;
use warnings;
use URI::Escape;
use Shongo::Common;
use Log::Log4perl;
use Encode;

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
    $self->{'template-parameters'} = {};
    $self->{'session'} = $session;
    $self->{'controller'} = {};
    $self->{'logger'} = Log::Log4perl->get_logger('cz.cesnet.shongo.client-common');

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
# @return 1 if request is ajax, 0 otherwise
#
sub is_ajax
{
    my ($self) = @_;
    my $ajax_header = 'HTTP_X_REQUESTED_WITH';
    if (exists $ENV{$ajax_header} && lc $ENV{$ajax_header} eq 'xmlhttprequest') {
        return 1;
    }
    return 0;
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
    $self->render_headers();
    print $self->render_template('error.html', {
        error => $error,
        stackTrace => $stack_trace
    });
    exit(0);
}

#
# Dispatch $action in $controller.
#
# @param $controller
# @param $action
#
sub dispatch
{
    my ($self, $controller, $action) = @_;

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
        if ( !$controller_instance->pre_dispatch($action) ) {
            return;
        }
        my $method_name = $action;
        $method_name =~ s/-/_/g;
        $method_name .= '_action';
        if ( $controller_instance->can($method_name) ) {
            $controller_instance->$method_name();
            return;
        }
    }

    $self->error_action("Undefined action '$action' in controller '$controller'!");
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
    my $possible_location = $self->get_current_url(0);
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

    $self->dispatch($controller, $action);
}

#
# Render http headers.
#
sub render_headers
{
    my ($self) = @_;
    my $cookie = undef;
    if ( defined($self->{'session'}) ) {
        if ( !$self->is_ajax() ) {
            $self->{'session'}->param('previous_url', $self->get_current_url());
        }
        $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    }
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
    else {
        # Decode all string values
        foreach my $key (keys %{$parameters}) {
            if ( !ref($parameters->{$key}) ) {
                $parameters->{$key} = decode_utf8($parameters->{$key});
            }
        }
    }
    $parameters->{'title'} = $title;
    $parameters->{'back'} = $self->get_back();
    my $content = $self->render_template($file, $parameters);
    $self->render_page_content($title, $content, $parameters);
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
    my ($self, $title, $content, $parameters) = @_;

    $parameters->{'title'} = $title;
    $parameters->{'content'} = $content;
    $parameters->{'session'} = {};
    foreach my $name ($self->{'session'}->param()) {
        $parameters->{'session'}->{$name} = $self->{'session'}->param($name);
    }

    # Render layout with the rendered content
    print $self->render_template('layout.html', $parameters);
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
    foreach my $parameter (keys %{$self->{'template-parameters'}}) {
        $parameters->{$parameter} = $self->{'template-parameters'}->{$parameter};
    }

    # Utility functions for templates
    my $time_zone_offset = $self->get_time_zone_offset();
    $parameters->{'util'} = {
        'datetime_format' => sub { datetime_format(@_, $time_zone_offset); },
        'datetime_format_date' => sub { datetime_format_date(@_, $time_zone_offset); },
        'datetime_partial_format' => sub { datetime_partial_format(@_); },
        'period_format' => sub { period_format(@_); },
        'interval_format' => sub { interval_format(@_, $time_zone_offset); },
        'interval_format_date' => sub { interval_format_date(@_, $time_zone_offset); },
    };
    if ( $self->{template}->process($file, $parameters, \$result) ) {
        return $result;
    }
    return undef;
}

sub get_current_url
{
    my ($self, $query) = @_;
    if ( !defined($query) ) {
        $query = 1;
    }

    return $self->{cgi}->url(-absolute => 1, -query => $query);
}

#
# Redirect to given $url
#
# @param $url
# @param $query
#
sub redirect
{
    my ($self, $url, $query, $disable_set_as_previous) = @_;
    if ( !defined($url) && defined($self->{'session'}->param('previous_url')) ) {
        $url = uri_unescape($self->{'session'}->param('previous_url'));
    }
    if ( !($url =~ /^(\/|http)/) ) {
        $url = '/' . $url;
    }
    $url = URI->new($url);
    if ( defined($query) ) {
        $url->query_form($query);
    }
    if ( !defined($disable_set_as_previous) || !$disable_set_as_previous ) {
        if ( !$self->is_ajax() ) {
            $self->{'session'}->param('previous_url', $self->get_current_url());
        }
    }
    my $cookie = $self->{'cgi'}->cookie(CGISESSID => $self->{'session'}->id);
    print $self->{'cgi'}->redirect(-uri => $url->as_string(), -cookie => $cookie);
}

#
# Push current url to the stack for "going back"
#
sub push_back
{
    my ($self) = @_;

    my $current_url = $self->get_current_url();
    my $history = $self->{'session'}->param('history');
    if ( !defined($history) ) {
        $history = [];
    }
    my $query = undef;
    if ( $current_url =~ /(.+)\?(.+)/ ) {
        $current_url = $1;
        $query = $2;
    }

    for ( my $index = 0; $index < scalar(@{$history}); $index++ ) {
        if ( $history->[$index]->{'url'} eq $current_url ) {
            splice(@{$history}, $index);
            last;
        }
    }

    push(@{$history}, {
        'url' => $current_url,
        'query' => $query
    });
    $self->{'session'}->param('history', $history);
}

#
# Set the query for the last back url
#
sub set_back_query
{
    my ($self, $query) = @_;
    my $history = $self->{'session'}->param('history');
    if ( defined($history) ) {
        my $index = scalar(@{$history}) - 1;
        if ( $index >= 0 ) {
            $history->[$index]->{'query'} = $query;
            $self->{'session'}->param('history', $history);
        }
    }
}

#
# Reset the stack for "going back"
#
sub reset_back
{
    my ($self) = @_;
    $self->{'session'}->param('history', []);
}

#
# Reset the stack for "going back"
#
sub get_back
{
    my ($self, $default) = @_;
    my $history = $self->{'session'}->param('history');

    my $back = undef;
    if ( defined($history) ) {
        my $current_url = $self->get_current_url();
        if ( $current_url =~ /(.+)\?(.+)/ ) {
            $current_url = $1;
        }
        my $index = scalar(@{$history}) - 1;
        while ( $index >= 0 ) {
            if ( $history->[$index]->{'url'} ne $current_url ) {
                $back = uri_unescape($history->[$index]->{'url'});
                if ( defined($history->[$index]->{'query'}) ) {
                    if ( ref($history->[$index]->{'query'}) ) {
                        $back = URI->new($back);
                        $back->query_form($history->[$index]->{'query'});
                        $back = $back->as_string();
                    }
                    else {
                        $back .= '?' . uri_unescape($history->[$index]->{'query'});
                    }
                }
                last;
            }
            else {
                $index--;
            }
        }
    }
    if ( !defined($back) ) {
        if ( !defined($default) ) {
            $back = '/';
        }
        else {
            $back = $default;
        }
    }
    return $back;
}

#
# @return time zone offset
#
sub get_time_zone_offset
{
    my ($self) = @_;
    if ( !defined($self->{'session'}) ) {
        return 0;
    }
    my $time_zone_offset = $self->{'session'}->param('time_zone_offset');
    if ( !defined($time_zone_offset) ) {
        $time_zone_offset = 0;
    }
    return $time_zone_offset;
}

1;