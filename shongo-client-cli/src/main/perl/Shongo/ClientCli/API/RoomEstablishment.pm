#
# Room Establishment
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::RoomEstablishment;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;

#
# Create a new instance
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_name('Establishment');
    $self->set_object_class('RoomEstablishment');

    $self->add_attribute('resourceId', {
        'title' => 'Resource Identifier',
        'string-pattern' => $Shongo::Common::IdPattern
    });
    $self->add_attribute('technologies', {
        'type' => 'collection',
        'item' => {
            'title' => 'Technology',
            'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
        },
        'complex' => 0,
        'required' => 1
    });
    $self->add_attribute('aliasSpecifications', {
        'title' => 'Aliases',
        'type' => 'collection',
        'item' => {
            'title' => 'alias',
            'class' => 'AliasSpecification',
        },
        'complex' => 1
    });

    return $self;
}

1;