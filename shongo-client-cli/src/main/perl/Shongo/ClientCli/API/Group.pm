#
# Group of users.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Group;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

our $Type = ordered_hash(
    'USER' => 'User',
    'SYSTEM' => 'System'
);

#
# Create a new instance of group
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Group');
    $self->set_object_name('Group');
    $self->add_attribute('id', {
        'title' => 'Identifier',
        'editable' => 0
    });
    $self->add_attribute('parentGroupId', {
        'title' => 'Parent Identifier'
    });
    $self->add_attribute('type', {
        'required' => 1,
        'type' => 'enum',
        'enum' =>  $Type
    });
    $self->add_attribute('name', {
        'required' => 1
    });
    $self->add_attribute('description');
    $self->add_attribute('administrators', {
        'title' => 'Administrators',
        'type' => 'collection',
        'item' => {
            'title' => 'Administrator',
            'format' => sub {
                my ($item) = @_;
                return Shongo::ClientCli->instance()->format_user($item, 1);
            },
            'add' => sub {
                console_read_value('Administrator', 1);
            }
        },
        'display-empty' => 1
    });
    return $self;
}

1;