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
    my $resource = Shongo::Controller::API::Resource->from_xml($result);
    my $resourceIdentifier = $resource->{'identifier'};
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
        },
        "dial" => {
            desc => "Dial a number",
            minargs => 1, args => "[number]",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_dial($resourceIdentifier, $args[0]);
            }
        },
        "standby" => {
            desc => "Switch to the standby mode",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_standby($resourceIdentifier);
            }
        },
        "hangUpAll" => {
            desc => "Hang up all calls",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_hang_up_all($resourceIdentifier);
            }
        },
        "mute" => {
            desc => "Mute the device",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_mute($resourceIdentifier);
            }
        },
        "unmute" => {
            desc => "Unmute the device",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_unmute($resourceIdentifier);
            }
        },
        "setMicrophoneLevel" => {
            desc => "Sets microphone(s) level",
            minargs => 1, args => "[number]",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_set_microphone_level($resourceIdentifier, $args[0]);
            }
        },
        "setPlaybackLevel" => {
            desc => "Sets playback level",
            minargs => 1, args => "[number]",
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_set_playback_level($resourceIdentifier, $args[0]);
            }
        },
        "dialParticipant" => {
            desc => "Dial participant",
            options => 'roomId=s roomUserId=s target=s',
            args => '[-roomId] [-roomUserId] [-target]',
            method => sub {
                my ($shell, $params, @args) = @_;
                resource_dial_participant($resourceIdentifier, $params->{'options'});
            }
        }
    });
    $shell->run();
}

sub resource_dial
{
    my ($resourceIdentifier, $target) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.dial',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($target)
    );
    if ( $result->is_fault ) {
        return;
    }
    printf("%s\n", $result->value());
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
    printf("%s\n", $result->value());
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
    printf("%s\n", $result->value());
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
    printf("%s\n", $result->value());
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
    printf("%s\n", $result->value());
}

sub resource_set_microphone_level
{
    my ($resourceIdentifier, $level) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setMicrophoneLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
    printf("%s\n", $result->value());
}

sub resource_set_playback_level
{
    my ($resourceIdentifier, $level) = @_;

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.setPlaybackLevel',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($level)
    );
    if ( $result->is_fault ) {
        return;
    }
    printf("%s\n", $result->value());
}

sub resource_dial_participant
{
    my ($resourceIdentifier, $attributes) = @_;

    my $roomId = console_read_value('Room ID', 1, undef, $attributes->{'roomId'});
    my $roomUserId = console_read_value('User ID', 1, undef, $attributes->{'roomUserId'});
    my $target = $attributes->{'target'};
    if ( !defined($target) ) {
        my $targetType = console_read_enum('Select target type', ordered_hash(
            'address' => 'Address',
            'alias' => 'Alias',
        ));
        if ( $targetType eq 'address' ) {
            $target = console_read_value('Address', 1, undef);
        } else {
            $target = Shongo::Controller::API::Alias->new();
            $target->create();
        }
    }

    if ( ref($target) ) {
        $target = $target->to_xml();
    }

    my $result = Shongo::Controller->instance()->secure_request(
        'ResourceControl.dialParticipant',
        RPC::XML::string->new($resourceIdentifier),
        RPC::XML::string->new($roomId),
        RPC::XML::string->new($roomUserId),
        $target
    );
    if ( $result->is_fault ) {
        return;
    }
}

1;