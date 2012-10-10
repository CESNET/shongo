#
# Shell class - represents Shongo client shell.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::Shell;
use base qw(Shongo::Shell);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller;
use Shongo::Controller::CommonService;
use Shongo::Controller::ResourceService;
use Shongo::Controller::ResourceControlService;
use Shongo::Controller::ReservationService;
use Shongo::Controller::CompartmentService;
use Shongo::Authentication;

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
    Shongo::Controller->populate($self);
    Shongo::Controller::CommonService->populate($self);
    Shongo::Controller::ResourceService->populate($self);
    Shongo::Controller::ResourceControlService->populate($self);
    Shongo::Controller::ReservationService->populate($self);
    Shongo::Controller::CompartmentService->populate($self);

    return(bless($self, $class));;
}

1;