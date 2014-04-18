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
    my ($arg) = @_;
    console_print_text('result: ' . $arg);
}

1;