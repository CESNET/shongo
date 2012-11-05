#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ParticipantSpecification;
use base qw(Shongo::Controller::API::Specification);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of participant specification
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    my $self = Shongo::Controller::API::Specification->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return specification class
#
sub select_type($)
{
    my ($type) = @_;
    return console_edit_enum('Select type of specification', $Shongo::Controller::API::Specification::ParticipantType, $type);
}

1;