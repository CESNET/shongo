#
# Management of resources.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::ResourceControlService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Resource;
use Shongo::Controller::API::Alias;
use Shongo::Controller::API::Room;

#
# Populate shell by options for management of resources.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'control-resource' => {
            desc => 'Control existing managed resource',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                control_resource($args[0]);
            }
        }
    });
}

sub select_resource($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the resource', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
}

sub control_resource()
{
    my ($identifier) = @_;
    $identifier = select_resource($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Resource.getResource',
        RPC::XML::string->new($identifier)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $resource = Shongo::Controller::API::Resource->from_hash($result);
    my $resourceIdentifier = $resource->get('identifier');
    if ( !(ref($resource->{'mode'}) eq 'HASH') ) {
        console_print_error("Resource '%s' is not managed!", $resourceIdentifier);
        return;
    }

    my $shell = Shongo::Shell->new('resource-control');
    $shell->prompt($resource->{'name'} . '> ');
    $shell->add_commands({
        "help" => {
            desc => "Print help information",
            args => sub { shift->help_args(undef, @_); },
            method => sub { shift->help_call(undef, @_); }
        },
        "exit" => {
            desc => "Exit the shell",
            method => sub { shift->exit_requested(1); }
        }
    });

    my $supportedMethods = resource_get_supported_methods($resourceIdentifier);
    if ( !defined($supportedMethods) ) {
        return;
    }
    my @supportedMethods = @{$supportedMethods};

    if (grep $_ eq 'dial', @supportedMethods) {
        $shell->add_commands({
            "dial" => {
                desc => "Dial a number or address",
                minargs => 1, args => "[number/address]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_dial($resourceIdentifier, $args[0]);
                }
            }
        });
    }
    if (grep $_ eq 'standBy', @supportedMethods) {
        $shell->add_commands({
            "standby" => {
                desc => "Switch to the standby mode",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_standby($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'hangUp', @supportedMethods) {
        $shell->add_commands({
            "hangup" => {
                desc => "Hang up a call",
                minargs => 1,
                args => "[callId]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_hang_up($resourceIdentifier, $args[0]);
                }
            }
        });
    }
    if (grep $_ eq 'hangUpAll', @supportedMethods) {
        $shell->add_commands({
            "hangup-all" => {
                desc => "Hang up all calls",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_hang_up_all($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'resetDevice', @supportedMethods) {
        $shell->add_commands({
            "reset-device" => {
                desc => "Resets the device",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_reset_device($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'mute', @supportedMethods) {
        $shell->add_commands({
            "mute" => {
                desc => "Mute the device",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_mute($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'unmute', @supportedMethods) {
        $shell->add_commands({
            "unmute" => {
                desc => "Unmute the device",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_unmute($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'setMicrophoneLevel', @supportedMethods) {
        $shell->add_commands({
            "set-microphone-level" => {
                desc => "Sets microphone(s) level",
                minargs => 1, args => "[number]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_set_microphone_level($resourceIdentifier, $args[0]);
                }
            }
        });
    }
    if (grep $_ eq 'setPlaybackLevel', @supportedMethods) {
        $shell->add_commands({
            "set-playback-level" => {
                desc => "Sets playback level",
                minargs => 1, args => "[number]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_set_playback_level($resourceIdentifier, $args[0]);
                }
            }
        });
    }
    if (grep $_ eq 'enableVideo', @supportedMethods) {
        $shell->add_commands({
            "enable-video" => {
                desc => "Enables video from the endpoint",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_enable_video($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'disableVideo', @supportedMethods) {
        $shell->add_commands({
            "disable-video" => {
                desc => "Disables video from the endpoint",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_disable_video($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'startPresentation', @supportedMethods) {
        $shell->add_commands({
            "start-presentation" => {
                desc => "Starts the presentation mode from the endpoint",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_start_presentation($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'stopPresentation', @supportedMethods) {
        $shell->add_commands({
            "stop-presentation" => {
                desc => "Stops the presentation mode from the endpoint",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_stop_presentation($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'dialParticipant', @supportedMethods) {
        $shell->add_commands({
            "dial-participant" => {
                desc => "Dial participant",
                options => 'roomId=s target=s',
                args => '[-roomId] [-target]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_dial_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'disconnectParticipant', @supportedMethods) {
        $shell->add_commands({
            "disconnect-participant" => {
                desc => "Disconnect participant from a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_disconnect_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'getRoom', @supportedMethods) {
        $shell->add_commands({
            "get-room" => {
                desc => "Gets info about a virtual room",
                options => 'roomId=s',
                args => '[<ROOM-ID>] [-roomId <ROOM-ID>]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_get_room($resourceIdentifier, $args[0], $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'createRoom', @supportedMethods) {
        $shell->add_commands({
            "create-room" => {
                desc => "Create virtual room",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_create_room($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'modifyRoom', @supportedMethods) {
        $shell->add_commands({
            "modify-room" => {
                desc => "Modify virtual room",
                args => '[<ROOM-ID>] [-roomId <ROOM-ID>]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_modify_room($resourceIdentifier, $args[0], $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'deleteRoom', @supportedMethods) {
        $shell->add_commands({
            "delete-room" => {
                desc => "Delete virtual room",
                options => 'roomId=s',
                args => '[-roomId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_delete_room($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'getRoomList', @supportedMethods) {
        $shell->add_commands({
            "list-rooms" => {
                desc => "List virtual rooms",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_list_rooms($resourceIdentifier);
                }
            }
        });
    }
    if (grep $_ eq 'listParticipants', @supportedMethods) {
        $shell->add_commands({
            "list-participants" => {
                desc => "List participants in a given room",
                minargs => 1, args => "[roomId]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_list_participants($resourceIdentifier, $args[0]);
                }
            }
        });
    }
    if (grep $_ eq 'getParticipant', @supportedMethods) {
        $shell->add_commands({
            "get-participant" => {
                desc => "Gets user information and room settings for given participant.",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_get_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'modifyParticipant', @supportedMethods) {
        $shell->add_commands({
            "modify-participant" => {
                desc => "Modifies some participant settings.",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_modify_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'muteParticipant', @supportedMethods) {
        $shell->add_commands({
            "mute-participant" => {
                desc => "Mutes a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_mute_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'unmuteParticipant', @supportedMethods) {
        $shell->add_commands({
            "unmute-participant" => {
                desc => "Unmutes a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_unmute_participant($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'enableParticipantVideo', @supportedMethods) {
        $shell->add_commands({
            "enable-participant-video" => {
                desc => "Enables video from a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_enable_participant_video($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'disableParticipantVideo', @supportedMethods) {
        $shell->add_commands({
            "disable-participant-video" => {
                desc => "Disables video from a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_disable_participant_video($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'setParticipantMicrophoneLevel', @supportedMethods) {
        $shell->add_commands({
            "set-participant-microphone-level" => {
                desc => "Sets microphone level of a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_set_participant_microphone_level($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'setParticipantPlaybackLevel', @supportedMethods) {
        $shell->add_commands({
            "set-participant-playback-level" => {
                desc => "Sets playback level of a participant in a room",
                options => 'roomId=s participantId=s',
                args => '[-roomId] [-participantId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_set_participant_playback_level($resourceIdentifier, $params->{'options'});
                }
            }
        });
    }
    if (grep $_ eq 'getDeviceLoadInfo', @supportedMethods) {
        $shell->add_commands({
            "get-device-load-info" => {
                desc => "Get info about current load of the controlled device",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_get_device_load_info($resourceIdentifier);
                }
            }
        });
    }

    $shell->run();
}

sub resource_get_supported_methods
{
    my ($resourceIdentifier) = @_;

    my $response = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getSupportedMethods',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $response->is_fault() ) {
        return;
    }
    return $response->value();
}

sub resource_dial
{
    my ($resourceIdentifier, $target) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.dial',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($target)
    );
    my $callId = $result->value();
    if ( !defined($callId) ) {
        $callId = '-- None --';
    }
    printf("Call ID: %s\n", $callId);
}

sub resource_standby
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.standBy',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_hang_up
{
    my ($resourceIdentifier, $callId) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.hangUp',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($callId)
    );
    if ($result->is_fault) {
        return;
    }
}

sub resource_hang_up_all
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.hangUpAll',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_reset_device
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.resetDevice',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault) {
        return;
    }
}

sub resource_mute
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.mute',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_unmute
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.unmute',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_set_microphone_level
{
    my ($resourceIdentifier, $level) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setMicrophoneLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::int->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_set_playback_level
{
    my ($resourceIdentifier, $level) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setPlaybackLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::int->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_enable_video
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.enableVideo',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_disable_video
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.disableVideo',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_start_presentation
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.startPresentation',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_stop_presentation
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.stopPresentation',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_dial_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $target = $attributes->{'target'};
    if ( !defined($target) ) {
        my $targetType = console_read_enum('Select target type', ordered_hash(
            'address' => 'Address',
            'alias' => 'Alias',
        ));
        if ( $targetType eq 'address' ) {
            $target = console_read_value('Address', 1, undef);
        } else {
            $target = Shongo::Controller::API::Alias->create();
        }
    }

    if ( ref($target) ) {
        $target = $target->to_xml();
    }

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.dialParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        $target
    );
    if ( $result->is_fault ) {
        return;
    }
    my $callId = $result->value();
    if ( !defined($callId) ) {
        $callId = '-- None --';
    }
    printf("Participant ID: %s\n", $callId);
}

sub resource_disconnect_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.disconnectParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub select_room
{
    my ($identifier, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'roomId'}) ) {
        $identifier = $attributes->{'roomId'};
    }
    $identifier = console_read_value('Identifier of the room', 0, undef, $identifier);
    return $identifier;
}

sub resource_get_room
{
    my ($resourceIdentifier, $roomIdentifier, $attributes) = @_;
    $roomIdentifier = select_room($roomIdentifier, $attributes);
    if ( !defined($resourceIdentifier) || !defined($roomIdentifier) ) {
        return;
    }

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $room = Shongo::Controller::API::Room->from_hash($result);
    if ( defined($room) ) {
        console_print_text($room->to_string());
    }
    else {
        print "No room returned\n";
    }
}

sub resource_create_room
{
    my ($resourceIdentifier, $attributes) = @_;

    Shongo::Controller::API::Room->create(undef, {
        'on_confirm' => sub {
            my ($room) = @_;
            my $result = Shongo::Controller->instance()->secure_request(
                'ResourceControl.createRoom',
                RPC::XML::string->new($resourceIdentifier),
                $room->to_xml()
            );
            if ( $result->is_fault ) {
                return;
            }
            my $roomIdentifier = $result->value();
            if ( !defined($roomIdentifier) ) {
                $roomIdentifier = '-- None --';
            }
            printf("Room ID: %s\n", $roomIdentifier);
            return $roomIdentifier;
        }
    });
}

sub resource_modify_room
{
    my ($resourceIdentifier, $roomIdentifier, $attributes) = @_;
    $roomIdentifier = select_room($roomIdentifier, $attributes);
    if ( !defined($resourceIdentifier) || !defined($roomIdentifier) ) {
        return;
    }

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomIdentifier)
    );

    my $options = {};
    $options->{'on_confirm'} = sub {
        my ($room) = @_;
        my $response = Shongo::Controller->instance()->secure_request(
            'ResourceControl.modifyRoom',
            RPC::XML::string->new($resourceIdentifier),
            $room->to_xml()
        );
        if ( !$response->is_fault() ) {
            my $newRoomId = $response->value();
            if ( $newRoomId ne $roomIdentifier ) {
                printf("New room ID: %s\n", $newRoomId);
            }
            return $room->{'identifier'};
        }
        return undef;
    };

    if ( !$result->is_fault ) {
        my $room = Shongo::Controller::API::Room->from_hash($result);
        if ( defined($room) ) {
            $room->modify(undef, $options);
        }
    }
}

sub resource_delete_room
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.deleteRoom',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId)
    );
}

sub resource_list_rooms
{
    my ($resourceIdentifier) = @_;

    my $response = Shongo::Controller->instance()->secure_request(
        'ResourceControl.listRooms',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $response->is_fault() ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Name', \' | ', 'Start date/time', \' |');
    foreach my $room (@{$response->value()}) {
        $table->add(
            $room->{'identifier'},
            $room->{'name'},
            format_datetime($room->{'startDateTime'})
        );
    }
    console_print_table($table);
}

sub resource_list_participants
{
    my ($resourceIdentifier, $roomId) = @_;

    my $response = Shongo::Controller->instance()->secure_request(
        'ResourceControl.listParticipants',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId)
    );
    if ( $response->is_fault() ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Display name', \' | ', 'Join time', \' | ');
    # TODO: add an --all switch to the command and, if used, print all available info to the table (see resource_get_participant)
    foreach my $roomUser (@{$response->value()}) {
        $table->add(
            $roomUser->{'userId'},
            $roomUser->{'displayName'},
            format_datetime($roomUser->{'joinTime'})
        );
    }
    console_print_table($table);
}

sub resource_get_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
    if ( $result->is_fault() ) {
        return;
    }
    my $participant = $result->value();
    if ( !defined($participant) ) {
        print "No participant info returned\n";
    }
    else {
        printf("Room ID:          %s\n", $participant->{'roomId'});
        printf("Participant ID:   %s\n", $participant->{'userId'});
        printf("User identity:    %s\n", ($participant->{'userIdentity'} ? $participant->{'userIdentity'} : "(not defined)"));
        printf("Display name:     %s\n", $participant->{'displayName'});
        printf("Join time:        %s\n", format_datetime($participant->{'joinTime'}));
        printf("Audio muted:      %s\n", ($participant->{'audioMuted'} ? "yes" : "no"));
        printf("Video muted:      %s\n", ($participant->{'videoMuted'} ? "yes" : "no"));
        printf("Microphone level: %s\n", $participant->{'microphoneLevel'});
        printf("Playback level:   %s\n", $participant->{'playbackLevel'});
        printf("Layout:           %s\n", $participant->{'layout'});
    }
}

sub resource_modify_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    printf("\n");
    my $displayName = console_read_value('New display name', 0, undef, undef);
    my $audioMuted = console_read_value('Audio muted (y/n)', 0, '^[yn]$', undef);
    my $videoMuted = console_read_value('Video muted (y/n)', 0, '^[yn]$', undef);
    my $microphoneLevel = console_read_value('Microphone level', 0, '^\\d+$', undef);
    my $playbackLevel = console_read_value('Playback level', 0, '^\\d+$', undef);

    # NOTE: attribute names must match RoomUser attribute name constants
    my %attributes = ();
    if ( defined $displayName ) {
        $attributes{'displayName'} = $displayName;
    }
    if ( defined $audioMuted ) {
        $attributes{'audioMuted'} = RPC::XML::boolean->new(($audioMuted eq 'y'));
    }
    if ( defined $videoMuted ) {
        $attributes{'videoMuted'} = RPC::XML::boolean->new(($videoMuted eq 'y'));
    }
    if ( defined $microphoneLevel ) {
        $attributes{'microphoneLevel'} = $microphoneLevel;
    }
    if ( defined $playbackLevel ) {
        $attributes{'playbackLevel'} = $playbackLevel;
    }
    # TODO: offer modification of room layout

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.modifyParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::struct->new(%attributes)
    );
}

sub resource_mute_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.muteParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_unmute_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.unmuteParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_enable_participant_video
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.enableParticipantVideo',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_disable_participant_video
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.disableParticipantVideo',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_set_participant_microphone_level
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});
    my $level = console_read_value('Level', 1, '^\\d+$', undef);

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setParticipantMicrophoneLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::int->new($level)
    );
}

sub resource_set_participant_playback_level
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});
    my $level = console_read_value('Level', 1, '^\\d+$', undef);

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setParticipantPlaybackLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::int->new($level)
    );
}

sub resource_get_device_load_info
{
    my ($resourceIdentifier) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getDeviceLoadInfo',
        RPC::XML::string->new($resourceIdentifier)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $info = $result->value();
    if ( !defined($info) ) {
        print "No info returned\n";
    }
    else {
        if (defined($info->{'uptime'})) {
            my $uptime = $info->{'uptime'};
            my $uptimeStr = '';
            if ($uptime >= 60*60*24) {
                $uptimeStr .= sprintf('%dd ', $uptime/(60*60*24));
                $uptime %= 60*60*24;
            }
            if ($uptime >= 60*60) {
                $uptimeStr .= sprintf('%dh ', $uptime/(60*60));
                $uptime %= 60*60;
            }
            if ($uptime >= 60) {
                $uptimeStr .= sprintf('%dm ', $uptime/(60));
                $uptime %= 60;
            }
            $uptimeStr .= sprintf('%ds', $uptime);
            printf("Uptime:               %s\n", $uptimeStr);
        }
        if (defined($info->{'cpuLoad'})) {
            printf("CPU load:             %.1f %%\n", $info->{'cpuLoad'});
        }
        if (defined($info->{'memoryOccupied'})) {
            printf("Memory occupied:      %d bytes\n",   $info->{'memoryOccupied'});
        }
        if (defined($info->{'memoryAvailable'})) {
            printf("Memory available:     %d bytes\n",   $info->{'memoryAvailable'});
        }
        if (defined($info->{'diskSpaceOccupied'})) {
            printf("Disk space occupied:  %d bytes\n",   $info->{'diskSpaceOccupied'});
        }
        if (defined($info->{'diskSpaceAvailable'})) {
            printf("Disk space available: %d bytes\n",   $info->{'diskSpaceAvailable'});
        }
    }
}


1;
