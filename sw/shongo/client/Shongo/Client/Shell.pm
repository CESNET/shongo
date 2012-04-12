#
# Shell class - represents Shongo client shell.
#
package Shongo::Client::Shell;

use strict;
use warnings;

use Shongo::Client::Controller;
use Shongo::Client::Resource;
use Term::Shell::MultiCmd;

#
# Create a new Shell instance.
#
sub new() {

    my $class = shift;
    my $self = {};
    bless $self, $class;
    $self->{'_shell'} = Term::Shell::MultiCmd->new(
        -prompt => 'command',
        -quit_cmd => 'exit',
        -history_file => "$ENV{HOME}/.shongo_client"
    );

    # Populate common commands
    my @tree = (
        'info' => {
            help => 'Show controller info',
            exec => sub {
                Shongo::Client::Controller->instance()->print_info();
            },
        }
    );
    $self->{'_shell'}->populate(@tree);

    # Populate resource management commands
    Shongo::Client::Resource->populate($self->{'_shell'});

    return $self;
}

#
# Run the shell - prompt user in loop
#
sub run {
    my ($self) = @_;
    print("Type 'help' for list of supported commands.\n");
    $self->{'_shell'}->loop();
}

#
# Run single command
#
sub command {
    my ($self, $command) = @_;
    print("Performing command '", $command, "'.\n");
    $self->{'_shell'}->cmd($command);
}

1;