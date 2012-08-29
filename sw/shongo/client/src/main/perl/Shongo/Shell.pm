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

#
# Create a new shell for controller client
#
sub new
{
    my $class = shift;
    my $self = Term::ShellUI->new(@_, commands => {});

    $self->{term}->ornaments(0);

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