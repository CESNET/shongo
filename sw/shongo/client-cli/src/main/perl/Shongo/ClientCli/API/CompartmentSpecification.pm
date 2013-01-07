#
# Compartment specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::CompartmentSpecification;
use base qw(Shongo::ClientCli::API::Specification);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of compartment specification
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
    return 'CompartmentSpecification';
}

1;