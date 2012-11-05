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

    my @supportedMethods = resource_get_supported_methods($resourceIdentifier);

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
    if (grep $_ eq 'getRoomSummary', @supportedMethods) {
        $shell->add_commands({
            "get-room-summary" => {
                desc => "Gets summary info about a virtual room",
                options => 'roomId=s',
                args => '[-roomId]',
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_get_room_summary($resourceIdentifier, $params->{'options'});
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
                minargs => 1, args => "[roomId]",
                method => sub {
                    my ($shell, $params, @args) = @_;
                    resource_modify_room($resourceIdentifier, $args[0]);
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
    return @{$response->value()};
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

sub resource_get_room_summary
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.getRoomSummary',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $room = $result->value();
    if ( !defined($room) ) {
        print "No room summary returned\n";
    }
    else {
        printf("Identifier:      %s\n", $room->{'identifier'});
        printf("Name:            %s\n", $room->{'name'});
        printf("Description:     %s\n", $room->{'description'});
        printf("Start date/time: %s\n", format_datetime($room->{'startDateTime'}));
    }
}

sub resource_create_room
{
    my ($resourceIdentifier, $attributes) = @_;

    my $room = Shongo::Controller::API::Object->new();
    $room->set_object_name('Room');
    $room->set_object_class('Room');
    $room->add_attribute('name', {'required' => 1});
    $room->add_attribute('portCount', {'type' => 'int', 'required' => 1});
    $room->add_attribute(
        'aliases', {
            'type' => 'collection',
            'collection' => {
                'title' => 'Alias',
                'class' => 'Shongo::Controller::API::Alias',
                'short' => 1
            }
        }
    );
    $room->modify();

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.createRoom',
        RPC::XML::string->new($resourceIdentifier),
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
}

sub resource_modify_room
{
    my ($resourceIdentifier, $roomId) = @_;

    my $roomName = console_read_value('New room name', 0, undef, undef);
    my $portCount = console_read_value('New port count', 0, '^\\d+$', undef);

    my %attributes = ();
    if ( defined $roomName ) {
        $attributes{'name'} = $roomName;
    }
    if ( defined $portCount ) {
        $attributes{'portCount'} = $portCount;
    }
    # TODO: offer modification of room aliases

    my %options = (); # TODO: offer filling room options (see Room.Option enum)

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.modifyRoom',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::struct->new(%attributes),
        RPC::XML::struct->new(%options)
    );
    if ( $result->is_fault ) {
        return;
    }
    my $newRoomId = $result->value();
    if ( !defined($newRoomId) ) {
        $newRoomId = '-- None --';
    }
    if ( $newRoomId ne $roomId ) {
        printf("New room ID: %s\n", $newRoomId);
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
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Name', \' | ', 'Description', \' | ', 'Start date/time', \' |');
    foreach my $room (@{$response->value()}) {
        $table->add(
            $room->{'identifier'},
            $room->{'name'},
            $room->{'description'},
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
        printf("Muted:            %s\n", ($participant->{'muted'} ? "yes" : "no"));
        printf("Microphone level: %s\n", $participant->{'microphoneLevel'});
        printf("Playback level:   %s\n", $participant->{'playbackLevel'});
        printf("Layout:           %s\n", $participant->{'layout'});
    }
}



1;