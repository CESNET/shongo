#
# Shell class - represents a interactive shell.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Shell;
use base qw(Term::ShellUI);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new shell
#
# @param $name
#
sub new
{
    my $class = shift;
    my $name = shift;
    my $disable_term = shift;
    my $self = Term::ShellUI->new(@_,
        commands => {},
        name => $name,
        backslash_continues_command => 1,
        disable_term => $disable_term
    );

    if ( defined($self->{'term'}) ) {
        $self->{'term'}->ornaments(0);
    }

    $self = (bless($self, $class));
    $self->load_history();

    return $self;
}

#
# Override processing of a cmd
#
sub process_a_cmd
{
    my ($self) = @_;

    # Set completion function before every command
    if ( defined($self->{'term'}) ) {
        my $attrs = $self->{'term'}->Attribs;
        $attrs->{completion_function} = sub { Term::ShellUI::completion_function($self, @_); };
    }

    # Default implementation
    my $result = Term::ShellUI::process_a_cmd(@_);

    return $result;
}

# @Override
sub load_history
{
    my ($self) = @_;
    if ( defined($self->{'term'}) ) {
        history_set_group_to($self->{'name'}, $self->{'term'});
    }
}

# @Override
sub save_history
{
    my ($self) = @_;
    if ( defined($self->{'term'}) ) {
        history_get_group_from($self->{'name'}, $self->{'term'});
    }
}

#
# Override invocation of command to parse options
#
sub call_cmd
{
    my ($self, $params) = @_;

    # store history
    $self->save_history();

    my $command = $params->{'cmd'};
    my $options = $command->{'options'};
    # Parse options if defined
    if ( defined($options) ) {
        my @parsed_options = ref($options) ? @{$options} : split ' ', $options;
        my $arguments = $params->{'args'};
        use Getopt::Long ;
        local @ARGV = @{$arguments};
        my $values = {};
        if ( eval{GetOptions( $values, @parsed_options)} ) {
            $params->{'args'} = [@ARGV];
            $params->{'options'} = $values;
        } else {
            return;
        }
    }
    # Default implementation
    my $result = Term::ShellUI::call_cmd(@_);

    # reload history
    $self->load_history();

    return $result;
}

#
# Pattern for matching JSON parameter.
#
our $JSON_PATTERN = '({.*})($| -.+)';

#
# Run single command
#
sub command
{
    my ($self, $command) = @_;
    my $string = $command;
    $string =~ s/(^\s*)|\s*$//g;
    $string =~ s/$JSON_PATTERN/.../g;
    console_print_debug("Performing command '" . $string . "'.\n");

    # store history
    if ( defined($self->{'term'}) ) {
        $self->{'term'}->addhistory($command);
        $self->save_history();
    }

    $self->process_a_cmd($command);

    # reload history
    if ( defined($self->{'term'}) ) {
        $self->{'term'}->addhistory($command);
        $self->save_history();
    }
}

#
# Parse attributes
#
sub parse_attributes
{
    my ($shell_params) = @_;
    my $attributes = {};

    # Parse ending json
    my $json_data = undef;
    if ( $shell_params->{'rawline'} =~ /^.+? $JSON_PATTERN/ ) {
        $json_data = $1;
    }
    if ( defined($json_data) ) {
        eval {
            my $json = JSON->new();
            $json->allow_singlequote(1);
            $json->allow_barekey(1);
            $json->relaxed(1);
            $json->loose(1);
            $json_data = $json->decode($json_data);
            foreach my $attribute_name (keys %{$json_data}) {
                $attributes->{$attribute_name} = $json_data->{$attribute_name};
            }
            1;
        } or do {
            my $error = $@;
            console_print_error("JSON data cannot be parsed!");
            console_print_text($json_data);
            console_print_error("$error");
            return undef;
        }
    }
    return $attributes;
}

1;