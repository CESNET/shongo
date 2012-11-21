#
# Controller.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Web::Controller;

use strict;
use warnings;
use Shongo::Common;

#
# Create a new instance of controller.
#
# @static
#
sub new
{
    my $class = shift;
    my ($application) = @_;
    my $self = {};
    bless $self, $class;

    $self->{application} = $application;

    return $self;
}

1;