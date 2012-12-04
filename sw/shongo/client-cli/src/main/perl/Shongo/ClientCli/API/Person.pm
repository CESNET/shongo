#
# Person
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Person;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of person
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->add_attribute('name', {'title' => 'Full Name'});
    $self->add_attribute('email');

    return $self;
}

1;