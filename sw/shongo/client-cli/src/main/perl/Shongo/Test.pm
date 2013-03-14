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
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Authorization.createAclRecord',
        RPC::XML::string->new('11'),
        'shongo:cz.cesnet:res:1',
        'owner'
    );
    var_dump($response->value());
}

1;