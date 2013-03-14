#
# Common services.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::AuthorizationService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;

#
# Populate shell by options for management of authorization.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'create-acl' => {
            desc => 'Create ACL record',
            args => '<user-id> <entity-id> <role>',
            method => sub {
                my ($shell, $params, @args) = @_;
                create_acl(@args);
            }
        },
        'delete-acl' => {
            desc => 'Delete ACL record',
            args => '<id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_acl(@args);
            }
        },
        'list-acl' => {
            desc => 'List ACL records',
            options => 'user=s entity=s role=s',
            args => '[-user=<user-id>] [-entity=<entity-id>] [-role=<role>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_acl($params->{'options'});
            }
        }
    });
}

sub create_acl()
{
    my (@args) = @_;
    if ( scalar(@args) < 3 ) {
        console_print_error("Arguments '<user-id> <entity-id> <role>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.createAclRecord',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1]),
        RPC::XML::string->new($args[2])
    );
    if ( defined($response) ) {
        console_print_info("ACL record '%s' has been created.", $response);
    }
}

sub delete_acl()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.deleteAclRecord',
        RPC::XML::string->new($args[0])
    );
    if ( defined($response) ) {
        console_print_info("ACL record '%s' has been deleted.", $args[0]);
    }
}

sub list_acl()
{
    my ($options) = @_;
    my $user_id = {};
    my $entity_id = {};
    my $role = {};
    if ( defined($options->{'user'}) ) {
        $user_id = RPC::XML::string->new($options->{'user'});
    }
    if ( defined($options->{'entity'}) ) {
        $entity_id = RPC::XML::string->new($options->{'entity'});
    }
    if ( defined($options->{'role'}) ) {
        $role = RPC::XML::string->new($options->{'role'});
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.listAclRecords', $user_id, $entity_id, $role);
    if ( !defined($response) ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Id', \' | ', 'User', \' | ', 'Entity', \' | ', 'Role', \' |');
    foreach my $record (@{$response}) {
        $table->add(
            $record->{'id'},
            $record->{'userId'},
            $record->{'entityId'},
            $record->{'role'},
        );
    }
    console_print_table($table);
}

1;