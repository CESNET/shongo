#
# Person
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Person;
use base qw(Shongo::Controller::API::Object);

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
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->add_attribute('name', {'title' => 'Full Name'});
    $self->add_attribute('email');

    return $self;
}

1;