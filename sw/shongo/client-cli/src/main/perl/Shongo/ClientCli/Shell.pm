#
# Shell class - represents Shongo client shell.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::Shell;
use base qw(Shongo::Shell);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli;
use Shongo::ClientCli::AuthorizationService;
use Shongo::ClientCli::CommonService;
use Shongo::ClientCli::ResourceService;
use Shongo::ClientCli::ResourceControlService;
use Shongo::ClientCli::ReservationService;
use Shongo::ClientCli::ExecutableService;
use Shongo::Test;

#
# Create a new shell for controller client
#
sub new
{
    my $class = shift;
    my $self = Shongo::Shell->new('controller');

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

    # Populate commands
    Shongo::ClientCli->populate($self);
    Shongo::ClientCli::AuthorizationService->populate($self);
    Shongo::ClientCli::CommonService->populate($self);
    Shongo::ClientCli::ResourceService->populate($self);
    Shongo::ClientCli::ResourceControlService->populate($self);
    Shongo::ClientCli::ReservationService->populate($self);
    Shongo::ClientCli::ExecutableService->populate($self);

    return(bless($self, $class));;
}

sub command
{
    my ($self, $command) = @_;
    if ( $command eq 'test' ) {
        Shongo::Test::test();
    }
    else {
        $self->SUPER::command($command);
    }
}

1;