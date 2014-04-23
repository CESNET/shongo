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
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Resource.getResource',
        RPC::XML::string->new($id)
    );
    if ( !defined($response) ) {
        return;
    }
    my $resource = Shongo::ClientCli::API::Resource->from_hash($response);
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
    if (grep $_ eq 'dialRoomParticipant', @supportedMethods) {
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
    if (grep $_ eq 'disconnectRoomParticipant', @supportedMethods) {
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
    if (grep $_ eq 'listRooms', @supportedMethods) {
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
    if (grep $_ eq 'listRoomParticipants', @supportedMethods) {
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
    if (grep $_ eq 'getRoomParticipant', @supportedMethods) {
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
    if (grep $_ eq 'modifyRoomParticipant', @supportedMethods) {
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
    if (grep $_ eq 'listRecordings', @supportedMethods) {
        $shell->add_commands({
            "list-recordings" => {
                desc => "List recordings in a given folder",
                minargs => 1, args => "[recordingFolderId]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_list_recordings($resourceId, $args[0]);
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
    if ( !defined($response) ) {
        return;
    }
    return $response;
}

sub resource_dial
{
    my ($resourceId) = @_;
    my $alias = Shongo::ClientCli::API::Alias->create()->to_xml();
    my $callId = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.dial',
        RPC::XML::string->new($resourceId),
        $alias
    );
    if ( !defined($callId) ) {
        $callId = '-- None --';
    }
    printf("Call ID: %s\n", $callId);
}

sub resource_standby
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.standBy',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_hang_up
{
    my ($resourceId, $callId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.hangUp',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($callId)
    );
}

sub resource_hang_up_all
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.hangUpAll',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_reboot_device
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.rebootDevice',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_mute
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.mute',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_unmute
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.unmute',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_set_microphone_level
{
    my ($resourceId, $level) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setMicrophoneLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::int->new($level)
    );
}

sub resource_set_playback_level
{
    my ($resourceId, $level) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.setPlaybackLevel',
        RPC::XML::string->new($resourceId),
        RPC::XML::int->new($level)
    );
}

sub resource_enable_video
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.enableVideo',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_disable_video
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.disableVideo',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_start_presentation
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.startPresentation',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_stop_presentation
{
    my ($resourceId) = @_;

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.stopPresentation',
        RPC::XML::string->new($resourceId)
    );
}

sub resource_show_message
{
    my ($resourceIdentifier, $attributes) = @_;

    my $duration = console_read_value('Duration', 1, '^\\d+$', $attributes->{'duration'});
    my $text     = console_read_value('Text', 1, undef, $attributes->{'text'});

    Shongo::ClientCli->instance()->secure_request(
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

    my $callId = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.dialRoomParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        $alias
    );
    if ( !defined($callId) ) {
        return;
    }
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

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.disconnectRoomParticipant',
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

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
    if ( !defined($response) ) {
        return;
    }
    my $room = Shongo::ClientCli::API::Room->from_hash($response);
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
            my $roomId = Shongo::ClientCli->instance()->secure_request(
                'ResourceControl.createRoom',
                RPC::XML::string->new($resourceId),
                $room->to_xml()
            );
            if ( !defined($roomId) ) {
                return;
            }
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

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );

    my $options = {};
    $options->{'on_confirm'} = sub {
        my ($room) = @_;
        my $newRoomId = Shongo::ClientCli->instance()->secure_request(
            'ResourceControl.modifyRoom',
            RPC::XML::string->new($resourceId),
            $room->to_xml()
        );
        if ( defined($newRoomId) ) {
            if ( $newRoomId ne $roomId ) {
                printf("New room ID: %s\n", $newRoomId);
            }
            return $room->{'id'};
        }
        return undef;
    };

    if ( defined($response) ) {
        my $room = Shongo::ClientCli::API::Room->from_hash($response);
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

    Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.deleteRoom',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
}

our $MAX_ROOM_NAME_LENGTH = 30;
our $MAX_ROOM_DESCRIPTION_LENGTH = 60;

sub resource_list_rooms
{
    my ($resourceId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.listRooms',
        RPC::XML::string->new($resourceId)
    );
    if ( !defined($response) ) {
        return;
    }

    my $table = {
        'columns' => [
            {'field' => 'id',   'title' => 'Identifier'},
            {'field' => 'name', 'title' => 'Name'},
            {'field' => 'description', 'title' => 'Description'},
            {'field' => 'alias', 'title' => 'Alias'},
            {'field' => 'startDateTime', 'title' => 'Start date/time'},
        ],
        'data' => []
    };
    foreach my $room (@{$response}) {
        my $name = $room->{'name'};
        if ( defined($name) && length($name) > $MAX_ROOM_NAME_LENGTH ) {
            $name = substr($name, 0, $MAX_ROOM_NAME_LENGTH - 3) . '...';
        }
        my $description = $room->{'description'};
        if ( defined($description) && length($description) > $MAX_ROOM_DESCRIPTION_LENGTH ) {
            $description = substr($description, 0, $MAX_ROOM_DESCRIPTION_LENGTH - 2) . '...';
        }
        push(@{$table->{'data'}}, {
            'id' => $room->{'id'},
            'name' => [$room->{'name'}, $name],
            'description' => [$room->{'description'}, $description],
            'alias' => $room->{'alias'},
            'startDateTime' => [$room->{'startDateTime'}, datetime_format($room->{'startDateTime'})]
        });
    }
    console_print_table($table);
}

sub resource_list_participants
{
    my ($resourceId, $roomId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.listRoomParticipants',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId)
    );
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'id',   'title' => 'Identifier'},
            {'field' => 'userId',   'title' => 'UserId'},
            {'field' => 'name', 'title' => 'Name'},
            {'field' => 'joinTime', 'title' => 'Join Time'},
        ],
        'data' => []
    };
    # TODO: add an --all switch to the command and, if used, print all available info to the table (see resource_get_participant)
    foreach my $roomParticipant (@{$response}) {
        push(@{$table->{'data'}}, {
            'id' => $roomParticipant->{'id'},
            'userId' => $roomParticipant->{'userId'},
            'name' => $roomParticipant->{'displayName'},
            'joinTime' => [$roomParticipant->{'joinTime'}, datetime_format($roomParticipant->{'joinTime'})]
        });
    }
    console_print_table($table);
}

sub resource_get_participant
{
    my ($resourceId, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $participantId = console_read_value('Participant ID', 1, undef, $attributes->{'participantId'});

    my $participant = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getRoomParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId)
    );
    if ( !defined($participant) ) {
        print "No participant info returned\n";
    }
    else {
        printf("Room ID:          %s\n", $participant->{'roomId'});
        printf("Participant ID:   %s\n", $participant->{'userId'});
        printf("User identity:    %s\n", ($participant->{'userIdentity'} ? $participant->{'userIdentity'} : "(not defined)"));
        printf("Display name:     %s\n", $participant->{'displayName'});
        printf("Join time:        %s\n", datetime_format($participant->{'joinTime'}));
        printf("Microphone muted: %s\n", ($participant->{'microphoneEnabled'} ? "no" : "yes"));
        printf("Microphone level: %s\n", $participant->{'microphoneLevel'});
        printf("Video enabled:    %s\n", ($participant->{'videoEnabled'} ? "yes" : "no"));
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
    my $microphoneMuted = console_read_value('Microphone muted (y/n)', 0, '^[yn]$', undef);
    my $videoEnabled = console_read_value('Video enabled (y/n)', 0, '^[yn]$', undef);
    my $microphoneLevel = console_read_value('Microphone level', 0, '^\\d+$', undef);
    my $playbackLevel = console_read_value('Playback level', 0, '^\\d+$', undef);

    # NOTE: attribute names must match RoomParticipant attribute name constants
    my %attributes = ();
    if ( defined $displayName ) {
        $attributes{'displayName'} = $displayName;
    }
    if ( defined $microphoneMuted ) {
        $attributes{'microphoneEnabled'} = RPC::XML::boolean->new(($microphoneMuted ne 'y'));
    }
    if ( defined $videoEnabled ) {
        $attributes{'videoEnabled'} = RPC::XML::boolean->new(($videoEnabled eq 'y'));
    }
    if ( defined $microphoneLevel ) {
        $attributes{'microphoneLevel'} = $microphoneLevel;
    }
    if ( defined $playbackLevel ) {
        $attributes{'playbackLevel'} = $playbackLevel;
    }
    # TODO: offer modification of room layout

    my $result = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.modifyRoomParticipant',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($participantId),
        RPC::XML::struct->new(%attributes)
    );
}

sub resource_get_device_load_info
{
    my ($resourceId) = @_;

    my $info = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.getDeviceLoadInfo',
        RPC::XML::string->new($resourceId)
    );
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

sub resource_list_recordings
{
    my ($resourceId, $recordingFolderId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_request(
        'ResourceControl.listRecordings',
        RPC::XML::string->new($resourceId),
        RPC::XML::string->new($recordingFolderId)
    );
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'name',     'title' => 'Name'},
            {'field' => 'date',     'title' => 'Date'},
            {'field' => 'duration', 'title' => 'Duration'},
            {'field' => 'url',      'title' => 'URL'}
        ],
        'data' => []
    };
    # TODO: add an --all switch to the command and, if used, print all available info to the table (see resource_get_participant)
    foreach my $recording (@{$response}) {
        push(@{$table->{'data'}}, {
            'name' => $recording->{'name'},
            'date' => datetime_format($recording->{'beginDate'}),
            'duration' => period_format($recording->{'duration'}),
            'url' => $recording->{'url'}
        });
    }
    console_print_table($table);
}

1;
