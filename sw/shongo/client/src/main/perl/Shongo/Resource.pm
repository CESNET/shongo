#
# Reservation class - Management of reservations.
#
package Shongo::Resource;

use strict;
use warnings;

use Shongo::Common;

# Enumeration of technologies
our %Technology = ordered_hash('H323' => 'H.323', 'SIP' => 'SIP', 'ADOBE_CONNECT' => 'Adobe Connect');

1;