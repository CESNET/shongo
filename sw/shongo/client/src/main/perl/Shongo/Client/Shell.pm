#
# Shell class - represents Shongo client shell.
#
package Shongo::Client::Shell;

use strict;
use warnings;

use Shongo::Client::Controller;
use Shongo::Client::Resource;
use Shongo::Client::Reservation;
use Term::Shell::MultiCmd;
use File::HomeDir;

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
        -history_file => File::HomeDir->my_home . "/.shongo_client"
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

    # Populate reservation management commands
    Shongo::Client::Reservation->populate($self->{'_shell'});

    return $self;
}

#
# Run the shell - prompt user in loop
#
sub run {
    my ($self) = @_;
    print("Type 'help' or 'help COMMAND' for more info.\n");
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

# Override Term::Shell::MultiCmd::_say to better format help
{
    no strict 'refs';
    no warnings 'redefine';
    *{"Term::Shell::MultiCmd" . '::' . "_say"} = \&_say;

    sub _say(@) {
        my $string = join ('', @_);
        $string =~ /^\n*(.*?)\s*$/s;

        # Convert " :\t" to fixed width form
        if ( $string =~ m/(.*): \t(.*)/ ) {
        	printf("%-15s  %s\n", $1 . ":", $2);
        } else {
            print($string, "\n");
        }
    }
}

1;