#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ParticipantSpecification;
use base qw(Shongo::ClientCli::API::Specification);

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
    my $self = Shongo::ClientCli::API::Specification->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return specification class
#
sub select_type($)
{
    my ($type) = @_;
    return console_edit_enum('Select type of specification', $Shongo::ClientCli::API::Specification::ParticipantType, $type);
}

1;