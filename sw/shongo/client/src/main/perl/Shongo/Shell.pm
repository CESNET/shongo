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

# Unique name of the shell
our $__shell_count = 0;

#
# Create a new shell for controller client
#
sub new
{
    my $class = shift;
    my $self = Term::ShellUI->new(@_, commands => {}, app => 'shell' . (++$__shell_count));

    $self->{term}->ornaments(0);

    return(bless($self, $class));;
}

#
# Override processing of a cmd
#
sub process_a_cmd
{
    my ($self) = @_;

    # Set completion function before every command
    my $attrs = $self->{term}->Attribs;
    $attrs->{completion_function} = sub { Term::ShellUI::completion_function($self, @_); };

    # Default implementation
    return Term::ShellUI::process_a_cmd(@_);
}

#
# Override loading history to erase previous
#
sub load_history
{
    my ($self) = @_;

    # Clear history
    $self->{term}->SetHistory();

    return Term::ShellUI::load_history(@_);
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
# Run single command
#
sub command
{
    my ($self, $command) = @_;
    print("Performing command '", $command, "'.\n");
    $self->process_a_cmd($command);
}

1;