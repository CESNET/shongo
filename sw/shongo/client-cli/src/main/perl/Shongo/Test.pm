#
# Test
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Test;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Object;

#
# Test modifying object
#
sub test
{
    my $object = Shongo::ClientCli::API::Object->new();
    $object->set_object_name('Object');
    $object->add_attribute('name', {'required' => 1});
    $object->add_attribute('aliases', {
        'type' => 'collection',
        'item' => {
            'title' => 'Alias',
            'class' => 'Shongo::ClientCli::API::Alias',
            'short' => 1
        },
        'complex' => 0
    });
    $object->from_hash({
        'name' => 'test',
        'aliases' => [
            {'value' => 'alias1'},
            {'value' => 'alias2'},
            {'value' => 'alias3'}
        ]
    });
    $object->modify();
    var_dump($object->to_xml());
}

1;