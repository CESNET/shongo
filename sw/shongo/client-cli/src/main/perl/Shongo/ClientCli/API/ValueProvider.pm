#
# Alias for video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ValueProvider;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of alias
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('ValueProvider');
    $self->set_object_name('Value Provider');
    $self->add_attribute('patterns', {
        'type' => 'collection',
            'item' => {
            'title' => 'Pattern',
            'add' => sub {
                console_read_value('Pattern', 1);
            }
        },
        'display-empty' => 1,
        'complex' => 0,
        'required' => 1
    });

    return $self;
}

1;