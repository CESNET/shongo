#
# Management of resources.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::ResourceControlService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Resource;
use Shongo::ClientCli::API::Alias;
use Shongo::ClientCli::API::Room;

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
            args => '[id] [cmd]',
            method => sub {
                my ($shell, $params, @args) = @_;

                # Parse command to perform from arguments
                my $command = undef;
                for ( my $index = 1; $index < scalar(@args); $index++ ){
                    if ( !defined($command) ) {
                        $command = $args[$index];
                    }
                    else {
                        $command .= ' ' . $args[$index];
                    }
                }

                control_resource($args[0], $command);
            }
        }
    });
}

sub select_resource($)
{
    my ($id) = @_;
    $id = console_read_value('Identifier of the resource', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}

sub control_resource()
{
    my ($id, $command) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
    }
    my $result = Shongo::ClientCli->instance()->secure_request(
        'Resource.getResource',
        RPC::XML::string->new($id)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $resource = Shongo::ClientCli::API::Resource->from_hash($result);
    my $resourceId = $resource->get('id');
    if ( !(ref($resource->{'mode'}) eq 'HASH') ) {
        console_print_error("Resource '%s' is not managed!", $resourceId);
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

    my $supportedMethods = resource_get_supported_methods($resourceId);
    if ( !defined($supportedMethods) ) {
        return;
    }
    my @supportedMethods = @{$supportedMethods};

    if (grep $_ eq 'dial', @supportedMethods) {
        $shell->add_commands({
            "dial" => {
                desc => "Dial a number or address",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_dial($resourceId);
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
                    resource_standby($resourceId);
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
                    resource_hang_up($resourceId, $args[0]);
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
                    resource_hang_up_all($resourceId);
                }
            }
        });
    }
    if (grep $_ eq 'rebootDevice', @supportedMethods) {
        $shell->add_commands({
            "reboot-device" => {
                desc => "Reboots the device",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_reboot_device($resourceId);
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
                    resource_mute($resourceId);
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
                    resource_unmute($resourceId);
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
                    resource_set_microphone_level($resourceId, $args[0]);
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
                    resource_set_playback_level($resourceId, $args[0]);
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
                    resource_enable_video($resourceId);
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
                    resource_disable_video($resourceId);
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
                    resource_start_presentation($resourceId);
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
                    resource_stop_presentation($resourceId);
                }
            }
        });
    }
    if (grep $_ eq 'showMessage', @supportedMethods) {
        $shell->add_commands({
            "show-message" => {
                desc => "Shows a message on the endpoint display",
                options => 'duration=s text=s',
                args => '[-duration] [-text]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_show_message($resourceId, $params->{'options'});
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
                    resource_dial_participant($resourceId, $params->{'options'});
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
                    resource_disconnect_participant($resourceId, $params->{'options'});
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
                    resource_list_rooms($resourceId);
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
                    resource_get_room($resourceId, $args[0], $params->{'options'});
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
                    resource_create_room($resourceId);
                }
            }
        });
    }
    if (grep $_ eq 'modifyRoom', @supportedMethods) {
        $shell->add_commands({
            "modify-room" => {
                desc => "Modify virtual room",
                options => 'roomId=s',
                args => '[<ROOM-ID>] [-roomId <ROOM-ID>]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_modify_room($resourceId, $args[0], $params->{'options'});
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
                    resource_delete_room($resourceId, $args[0], $params->{'options'});
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
                    resource_list_participants($resourceId, $args[0]);
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
                    resource_get_participant($resourceId, $params->{'options'});
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
                    resource_modify_participant($resourceId, $params->{'options'});
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
                    resource_mute_participant($resourceId, $params->{'options'});
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
                    resource_unmute_participant($resourceId, $params->{'options'});
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
                    resource_enable_participant_video($resourceId, $params->{'options'});
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
                    resource_disable_participant_video($resourceId, $params->{'options'});
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
                    resource_set_participant_microphone_level($resourceId, $params->{'options'});
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
                    resource_set_participant_playback_level($resourceId, $params->{'options'});
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
                    resource_get_device_load_info($resourceId);
                }
            }
        });
    }

    if ( defined($command) ) {
        $shell->command($command);
    }
    else {
        $shell->run();
    }
}

sub resource_get_supported_methods
{
    my ($resourceId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getSupportedMethods',
        RPC::XML::string->new($resourceId)
    );
    if ( $response->is_fault() ) {
        return;
    }
    return $response->value();
}

sub resource_dial
{
    my ($resourceId) = @_;
    my $alias = Shongo::ClientCli::API::Alias->create()->to_xml();
    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.dial',
        RPC::XML::string->new($resourceId),
        $alias
    );
    my $callId = $result->value();
    if ( !defined($callId) ) {
        $callId = '-- None --';
    }
    printf("Call ID: %s\n", $callId);
}

sub resource_standby
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.standBy',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_hang_up
{
    my ($resourceId, $callId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.hangUp',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($callId)
    );
    if ($result->is_fault) {
        return;
    }
}

sub resource_hang_up_all
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.hangUpAll',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_reboot_device
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.rebootDevice',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault) {
        return;
    }
}

sub resource_mute
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.mute',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_unmute
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.unmute',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_set_microphone_level
{
    my ($resourceId, $level) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setMicrophoneLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::int->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_set_playback_level
{
    my ($resourceId, $level) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setPlaybackLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::int->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_enable_video
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.enableVideo',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_disable_video
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.disableVideo',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_start_presentation
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.startPresentation',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_stop_presentation
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.stopPresentation',
        RPC::XML::string->new($resourceId)
    );
    if ( $result->is_fault ) {
        return;
    }
}

sub resource_show_message
{
    my ($resourceIdentifier, $attributes) = @_;

    my $duration = console_read_value('Duration', 1, '^\\d+$', $attributes->{'duration'});
    my $text     = console_read_value('Text', 1, undef, $attributes->{'text'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.showMessage',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::int->new($duration),
        RPC::XML::string->new($text)
    );
}

sub resource_dial_participant
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $alias = Shongo::ClientCli::API::Alias->create()->to_xml();

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.dialParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        $alias
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
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.disconnectParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub select_room
{
    my ($id, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'roomId'}) ) {
        $id = $attributes->{'roomId'};
    }
    $id = console_read_value('Identifier of the room', 0, undef, $id);
    return $id;
}

sub resource_get_room
{
    my ($resourceId, $roomId, $attributes) = @_;
    $roomId = select_room($roomId, $attributes);
    if ( !defined($resourceId) || !defined($roomId) ) {
        return;
    }

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $room = Shongo::ClientCli::API::Room->from_hash($result);
    if ( defined($room) ) {
        console_print_text($room->to_string());
    }
    else {
        print "No room returned\n";
    }
}

sub resource_create_room
{
    my ($resourceId, $attributes) = @_;

    Shongo::ClientCli::API::Room->create(undef, {
        'on_confirm' => sub {
            my ($room) = @_;
            my $result = Shongo::ClientCli->instance()->secure_request(
                'ResourceControl.createRoom',
                RPC::XML::string->new($resourceId),
                $room->to_xml()
            );
            if ( $result->is_fault ) {
                return;
            }
            my $roomId = $result->value();
            if ( !defined($roomId) ) {
                $roomId = '-- None --';
            }
            printf("Room ID: %s\n", $roomId);
            return $roomId;
        }
    });
}

sub resource_modify_room
{
    my ($resourceId, $roomId, $attributes) = @_;
    $roomId = select_room($roomId, $attributes);
    if ( !defined($resourceId) || !defined($roomId) ) {
        return;
    }

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );

    my $options = {};
    $options->{'on_confirm'} = sub {
        my ($room) = @_;
        my $response = Shongo::ClientCli->instance()->secure_request(
            'ResourceControl.modifyRoom',
            RPC::XML::string->new($resourceId),
            $room->to_xml()
        );
        if ( !$response->is_fault() ) {
            my $newRoomId = $response->value();
            if ( $newRoomId ne $roomId ) {
                printf("New room ID: %s\n", $newRoomId);
            }
            return $room->{'id'};
        }
        return undef;
    };

    if ( !$result->is_fault ) {
        my $room = Shongo::ClientCli::API::Room->from_hash($result);
        if ( defined($room) ) {
            $room->modify(undef, $options);
        }
    }
}

sub resource_delete_room
{
    my ($resourceId, $roomId, $attributes) = @_;
    $roomId = select_room($roomId, $attributes);
    if ( !defined($resourceId) || !defined($roomId) ) {
        return;
    }

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.deleteRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
}

sub resource_list_rooms
{
    my ($resourceId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.listRooms',
        RPC::XML::string->new($resourceId)
    );
    if ( $response->is_fault() ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Name', \' | ', 'Description', \' | ', 'Start date/time', \' |');
    foreach my $room (@{$response->value()}) {
        $table->add(
            $room->{'id'},
            $room->{'name'},
            $room->{'description'},
            datetime_format($room->{'startDateTime'})
        );
    }
    console_print_table($table);
}

sub resource_list_participants
{
    my ($resourceId, $roomId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.listParticipants',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
    if ( $response->is_fault() ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Display name', \' | ', 'Join time', \' |');
    # TODO: add an --all switch to the command and, if used, print all available info to the table (see resource_get_participant)
    foreach my $roomUser (@{$response->value()}) {
        $table->add(
            $roomUser->{'userId'},
            $roomUser->{'displayName'},
            datetime_format($roomUser->{'joinTime'})
        );
    }
    console_print_table($table);
}

sub resource_get_participant
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getParticipant',
        RPC::XML::string->new($resourceId),
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
        printf("Join time:        %s\n", datetime_format($participant->{'joinTime'}));
        printf("Audio muted:      %s\n", ($participant->{'audioMuted'} ? "yes" : "no"));
        printf("Video muted:      %s\n", ($participant->{'videoMuted'} ? "yes" : "no"));
        printf("Microphone level: %s\n", $participant->{'microphoneLevel'});
        printf("Playback level:   %s\n", $participant->{'playbackLevel'});
        printf("Layout:           %s\n", $participant->{'layout'});
    }
}

sub resource_modify_participant
{
    my ($resourceId, $attributes) = @_;

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

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.modifyParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::struct->new(%attributes)
    );
}

sub resource_mute_participant
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.muteParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_unmute_participant
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.unmuteParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_enable_participant_video
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.enableParticipantVideo',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_disable_participant_video
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.disableParticipantVideo',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
}

sub resource_set_participant_microphone_level
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});
    my $level = console_read_value('Level', 1, '^\\d+$', undef);

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setParticipantMicrophoneLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::int->new($level)
    );
}

sub resource_set_participant_playback_level
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});
    my $level = console_read_value('Level', 1, '^\\d+$', undef);

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setParticipantPlaybackLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::int->new($level)
    );
}

sub resource_get_device_load_info
{
    my ($resourceId) = @_;

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getDeviceLoadInfo',
        RPC::XML::string->new($resourceId)
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
