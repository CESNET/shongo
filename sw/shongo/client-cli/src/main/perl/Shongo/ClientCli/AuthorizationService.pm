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
        'get-user' => {
            desc => 'Get user',
            args => '<user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_user(@args);
            }
        },
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
        'get-acl' => {
            desc => 'Get ACL record',
            args => '<id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_acl(@args);
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
        },
        'list-permissions' => {
            desc => 'List permissions for authorized user to a entity',
            args => '<entity-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_permissions(@args);
            }
        },
    });
}

sub get_user()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<user-id>' must be specified.");
        return;
    }

    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.getUser', RPC::XML::string->new($args[0]));
    if ( defined($response) ) {
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('User Information');
        $object->add_attribute('Id', {}, $response->{'id'});
        $object->add_attribute('EPPN', {}, $response->{'eduPersonPrincipalName'});
        $object->add_attribute('First Name', {}, $response->{'firstName'});
        $object->add_attribute('Last Name', {}, $response->{'lastName'});
        console_print_text($object);
    }
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

sub get_acl()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<id>' must be specified.");
        return;
    }

    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.getAclRecord', RPC::XML::string->new($args[0]));
    if ( defined($response) ) {
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('ACL Record');
        $object->add_attribute('Id', {}, $response->{'id'});
        $object->add_attribute('User-id', {}, $response->{'userId'});
        $object->add_attribute('Entity', {}, $response->{'entityId'});
        $object->add_attribute('Role', {}, $response->{'role'});
        console_print_text($object);
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
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Authorization.listAclRecords', $user_id, $entity_id, $role);
    if ( !defined($response) ) {
        return;
    }
    my $table = Text::Table->new(\'| ', 'Id', \' | ', 'User', \' | ', 'Entity', \' | ', 'Role', \' |');
    foreach my $record (@{$response}) {
        $table->add(
            $record->{'id'},
            $application->format_user($record->{'userId'}),
            $record->{'entityId'},
            $record->{'role'},
        );
    }
    console_print_table($table);
}

sub list_permissions()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Arguments '<entity-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.listPermissions',
        RPC::XML::string->new($args[0]),
    );
    my $table = Text::Table->new(\'| ', 'Permission', \' |');
    foreach my $permission (@{$response}) {
        $table->add(
            $permission
        );
    }
    console_print_table($table);
}

1;