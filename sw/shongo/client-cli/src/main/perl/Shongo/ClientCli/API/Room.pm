#
# Room
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Room;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::RoomSettings;

# Enumeration of room layouts
our $Layout = ordered_hash(
    'OTHER' => 'Other',
    'SPEAKER' => 'Speaker',
    'SPEAKER_CORNER' => 'Speaker Corner',
    'GRID' => 'Grid'
);

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

    $self->set_object_name('Room');
    $self->set_object_class('Room');
    $self->add_attribute('id', {
        'title' => 'Identifier',
        'editable' => 0
    });
    $self->add_attribute('description');
    $self->add_attribute('licenseCount', {'title' => 'License Count', 'type' => 'int', 'required' => 1});
    $self->add_attribute(
        'technologies', {
            'type' => 'collection',
            'item' => {
                'title' => 'Technology',
                'enum' => $Shongo::Common::Technology
            },
            'required' => 1
        }
    );
    $self->add_attribute('aliases', {
        'type' => 'collection',
        'item' => {
            'title' => 'Alias',
            'class' => 'Shongo::ClientCli::API::Alias',
            'short' => 1
        }
    });
    $self->add_attribute('layout', {
        'type' => 'enum',
        'enum' =>  $Layout
    });
    $self->add_attribute('roomSettings', {
        'title' => 'Room Settings',
        'display' => 'newline',
        'complex' => 1,
        'format' => sub() {
            my ($room_settings) = @_;
            Shongo::ClientCli::API::RoomSettings::format_room_settings($room_settings, $self->get('technologies'));
        },
        'modify' => sub() {
            my ($room_settings) = @_;
            return Shongo::ClientCli::API::RoomSettings::modify_room_settings($room_settings);
        }
    });

    return $self;
}

1;