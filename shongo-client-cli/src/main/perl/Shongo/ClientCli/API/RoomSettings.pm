#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::RoomSettings;

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

my $ROOM_SETTING = {
    'H323RoomSetting' => {
        'technology' => 'H323',
        'options' => ordered_hash(
            'pin'                    => {'type' => 'string',  'title' => 'Pin'},
            'listedPublicly'         => {'type' => 'bool',    'title' => 'Listed Publicly'},
            'allowContent'           => {'type' => 'bool',    'title' => 'Allow Content'},
            'allowGuests'            => {'type' => 'bool',    'title' => 'Allow Guests'},
            'joinMicrophoneDisabled' => {'type' => 'bool',    'title' => 'Join Microphone Muted'},
            'joinVideoDisabled'      => {'type' => 'bool',    'title' => 'Join Video Disabled'},
            'registerWithGatekeeper' => {'type' => 'bool',    'title' => 'Register With Gatekeeper'},
            'registerWithRegistrar'  => {'type' => 'bool',    'title' => 'Register With Registrar'},
            'startLocked'            => {'type' => 'bool',    'title' => 'Start Locked'},
            'conferenceMeEnabled'    => {'type' => 'bool',    'title' => 'Conference Me Enabled'},
        )
    }
};

#
# Format collection of room settings.
#
# @param $room_settings collection of room settings
# @param $technologies  collection of technologies defining which room settings should be shown
#
sub format_room_settings
{
    my ($room_settings, $technologies) = @_;
    my $output = Shongo::ClientCli::API::Object->new();

    foreach my $room_setting (@{$room_settings}) {
        my $room_setting_definition = $ROOM_SETTING->{$room_setting->{'class'}};
        if ( !defined($room_setting_definition) ) {
            next;
        }
        if ( defined($technologies) && !array_value_exists($room_setting_definition->{'technology'}, @{$technologies}) ) {
            next;
        }
        my $options = $room_setting_definition->{'options'};
        foreach my $name (ordered_hash_keys($options)) {
            my $value = $room_setting->{$name};
            if ( !defined($value) ) {
                next;
            }
            my $option = $options->{$name};
            $output->add_attribute($option->{'title'}, {'type' => $option->{'type'}}, $value);
        }

    }
    return $output->to_string();
}

sub modify_room_settings
{
    my ($room_settings, $technologies) = @_;
    console_print_error('TODO: Implement room settings modification');
    return $room_settings;
}

1;