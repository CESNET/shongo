#
# Auxiliary data for ReservationRequestAbstract
#
# @author Filip Karnis <karnis@cesnet.cz>
#
package Shongo::ClientCli::API::AuxiliaryData;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of auxiliary data
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('AuxData');
    $self->set_object_name('Auxiliary Data');
    $self->add_attribute('tagName', {
        'required' => 1,
        'type' => 'string',
    });
    $self->add_attribute('enabled', {
        'required' => 1,
        'type' => 'bool',
    });
    $self->add_attribute('data', {
        'required' => 0,
        'type' => 'string',
    });

    return $self;
}

1;
