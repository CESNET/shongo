#
# Shell class - represents Shongo client shell.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Shell;
use base qw(Term::ShellUI);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller;
use Shongo::Controller::CommonService;
use Shongo::Controller::ResourceService;
use Shongo::Controller::ReservationService;

#
# Create a new shell for controller client
#
sub new
{
    my $class = shift;
    my $self = Term::ShellUI->new(@_, commands => {}, history_file => '~/.shongo_client');

    $self->{term}->Attribs->ornaments(0);
    $self->prompt('shongo> ');
    $self->add_commands({
        "help" => {
            desc => "Print help information",
            args => sub { shift->help_args(undef, @_); },
            method => sub { shift->help_call(undef, @_); }
        },
        "exit" => {
            desc => "Exit the shell",
            method => sub { shift->exit_requested(1); }
        },
    });

    # Populate controller commands
    Shongo::Controller->populate($self);

    # Populate common commands
    Shongo::Controller::CommonService->populate($self);

    # Populate resource management commands
    Shongo::Controller::ResourceService->populate($self);

    # Populate reservation management commands
    Shongo::Controller::ReservationService->populate($self);

    return(bless($self, $class));;
}

#
# Override invocation of command to parse options
#
sub call_cmd
{
    my ($self, $params) = @_;
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
    # Default implemetnation
    return Term::ShellUI::call_cmd(@_);
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