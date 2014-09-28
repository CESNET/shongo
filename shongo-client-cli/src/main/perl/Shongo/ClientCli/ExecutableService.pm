#
# Management of executables.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::ExecutableService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Executable;

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'delete-executable' => {
            desc => 'Delete an existing executable',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                foreach my $id (split(/,/, $args[0])) {
                    delete_executable($id);
                }
            }
        },
        'list-executables' => {
            desc => 'List summary of all existing executables',
            options => 'participant=s -resource=s all',
            args => '[-all] [-participant=<user-id>] [-resource=<resource-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_executables($params->{'options'});
            }
        },
        'get-executable' => {
            desc => 'Get existing executable',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_executable($id);
                    }
                } else {
                    get_executable();
                }
            }
        },
        'update-executable' => {
            desc => 'Start/Update/Stop existing executable again (if it is in failed state)',
            options => 'skip-execution',
            args => '[id] [-skip-execution]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        update_executable($id, $params->{'options'});
                    }
                }
                else {
                    update_executable();
                }
            }
        },
        'attach-room' => {
            desc => 'Attach existing device room to not started room executable',
            args => '[room-executable-id][device-room-id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                attach_room(@args);
            }
        },
        'list-executable-recordings' => {
            desc => 'List recordings for executable',
            args => '[executable-id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_executable_recordings($args[0]);
            }
        },
    });
}

sub select_executable($)
{
    my ($id) = @_;
    $id = console_read_value('Identifier of the executable', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}

sub delete_executable()
{
    my ($id) = @_;
    $id = select_executable($id);
    if ( !defined($id) ) {
        return;
    }
    Shongo::ClientCli->instance()->secure_request(
        'Executable.deleteExecutable',
        RPC::XML::string->new($id)
    );
}

sub list_executables()
{
    my ($options) = @_;
    my $filter = {};
    my $application = Shongo::ClientCli->instance();
    my $request = {};
    if ( defined($options->{'all'}) ) {
        $request->{'history'} = 1;
    }
    if ( defined($options->{'participant'}) ) {
        $request->{'participantUserId'} = $options->{'participant'};
    }
    if ( defined($options->{'resource'}) ) {
        $request->{'resourceId'} = $options->{'resource'};
    }
    my $response = $application->secure_hash_request('Executable.listExecutables', $request);
    if ( !defined($response) ) {
        return
    }
    our $Type = ordered_hash(
        'ROOM' => 'Room',
        'USED_ROOM' => 'Used Room',
        'OTHER' => 'Other'
    );
    my $table = {
        'columns' => [
            {'field' => 'id',         'title' => 'Identifier'},
            {'field' => 'type',       'title' => 'Type'},
            {'field' => 'technology', 'title' => 'Technology'},
            {'field' => 'slot',       'title' => 'Slot'},
            {'field' => 'state',      'title' => 'State'},
        ],
        'data' => []
    };
    foreach my $executable (@{$response->{'items'}}) {
        my $type = '';
        if ( defined($Type->{$executable->{'type'}}) )  {
            $type = $Type->{$executable->{'type'}};
        }
        my $technologies = '';
        foreach my $technology (@{$executable->{'roomTechnologies'}}) {
            if ( length($technologies) > 0 ) {
                $technologies .= ', ';
            }
            $technologies .= $Shongo::Common::Technology->{$technology};
        }
        push(@{$table->{'data'}}, {
            'id' => $executable->{'id'},
            'type' => [$executable->{'type'}, $type],
            'technology' => [$executable->{'roomTechnologies'}, $technologies],
            'slot' => [$executable->{'slot'}, interval_format($executable->{'slot'})],
            'state' => [$executable->{'state'}, Shongo::ClientCli::API::Executable::format_state($executable->{'state'}, $Shongo::ClientCli::API::Executable::State)]
        });
    }
    console_print_table($table);
}

sub get_executable()
{
    my ($id) = @_;
    $id = select_executable($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Executable.getExecutable',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        my $executable = Shongo::ClientCli::API::Executable->from_hash($response);
        if ( defined($executable) ) {
            console_print_text($executable);
        }
    }
}

sub update_executable()
{
    my ($id, $options) = @_;
    $id = select_executable($id);
    if ( !defined($id) ) {
        return;
    }
    my $skip_execution = 0;
    if ( defined($options->{'skip-execution'}) ) {
        $skip_execution = 1;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Executable.updateExecutable',
        RPC::XML::string->new($id),
        RPC::XML::boolean->new($skip_execution)
    );
    if ( defined($response) ) {
        console_print_info("Executable '$id' has been updated.");
    }
}

sub attach_room()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<room-executable-id> <device-room-id>' must be specified.");
        return;
    }
    my $room_executable_id = $args[0];
    my $device_room_id = $args[1];
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Executable.attachRoomExecutable',
        RPC::XML::string->new($room_executable_id),
        RPC::XML::string->new($device_room_id)
    );
    if ( defined($response) ) {
        console_print_info("Room '$device_room_id' has been attached to room executable '$room_executable_id'.");
    }
}

sub list_executable_recordings()
{
    my ($executableId) = @_;

    my $response = Shongo::ClientCli->instance()->secure_hash_request('Executable.listExecutableRecordings', {
        'executableId' => RPC::XML::string->new($executableId),
    });
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
    foreach my $recording (@{$response->{'items'}}) {
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