#
# Room Availability
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::RoomAvailability;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::ClientCli::API::ExecutableServiceSpecification;

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

    $self->set_object_name('Availability');
    $self->set_object_class('RoomAvailability');

    $self->add_attribute('slotMinutesBefore', {
        'title' => 'Slot Before (min)',
        'type' => 'int'
    });
    $self->add_attribute('slotMinutesAfter', {
        'title' => 'Slot After (min)',
        'type' => 'int'
    });
    $self->add_attribute('participantCount', {
        'title' => 'Participant Count',
        'type' => 'int',
        'required' => 1
    });
    $self->add_attribute('notifyParticipants', {
        'type' => 'bool',
        'title' => 'Notify Participants',
        'required' => 1
    });
    $self->add_attribute('notifyParticipantsMessage', {
        'title' => 'Notify Message',
        'type' => 'string'
    });
    $self->add_attribute('serviceSpecifications', {
        'title' => 'Services',
        'type' => 'collection',
        'item' => {
            'title' => 'service',
            'class' => 'ExecutableServiceSpecification',
        },
        'complex' => 1
    });

    return $self;
}

1;