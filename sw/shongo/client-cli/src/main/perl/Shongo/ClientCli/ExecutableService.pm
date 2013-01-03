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
            options => 'owner=s',
            args => '[-owner=*|<user-id>]',
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
    if ( defined($options->{'owner'}) ) {
        $filter->{'userId'} = $options->{'owner'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Executable.listExecutables', $filter);
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(
        \'| ', 'Identifier',
        \' | ', 'Owner',
        \' | ', 'Type',
        \' | ', 'Slot',
        \' | ', 'State',
        \' |'
    );
    foreach my $executable (@{$response->value()}) {
        my $type = '';
        if ( $executable->{'type'} eq 'COMPARTMENT' ) {
            $type = 'Compartment';
        }
        elsif ( $executable->{'type'} eq 'VIRTUAL_ROOM' ) {
            $type = 'Virtual Room';
        }
        $table->add(
            $executable->{'id'},
            $application->format_user($executable->{'userId'}),
            $type,
            format_interval($executable->{'slot'}),
            Shongo::ClientCli::API::Executable::format_state($executable->{'state'}, $Shongo::ClientCli::API::Executable::State)
        );
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
    my $result = Shongo::ClientCli->instance()->secure_request(
        'Executable.getExecutable',
        RPC::XML::string->new($id)
    );
    if ( !$result->is_fault ) {
        my $executable = Shongo::ClientCli::API::Executable->from_hash($result);
        if ( defined($executable) ) {
            console_print_text($executable->to_string());
        }
    }
}

1;