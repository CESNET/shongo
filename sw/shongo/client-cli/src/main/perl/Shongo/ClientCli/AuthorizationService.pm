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
        'list-users' => {
            desc => 'List users',
            args => '<filter>',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_users(@args);
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
            options => 'user=s entity=s role=s',
            args => '<id> [-user=<user-id>] [-entity=<entity-id>] [-role=<role>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_acl($params->{'options'}, @args);
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
        'set-entity-user' => {
            desc => 'Change entity user',
            args => '<entity-id> <user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                set_entity_user(@args);
            }
        },
        'list-referenced-users' => {
            desc => 'List referenced users by any entity',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_referenced_users(@args);
            }
        },
        'modify-user-id' => {
            desc => 'Modify user-id in all entities',
            args => '<old-user-id> <new-user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_user_id(@args);
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

    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listUsers', {
        'userIds' => [RPC::XML::string->new($args[0])]
    });
    if ( defined($response) ) {
        my $user = $response->{'items'}->[0];
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('User Information');
        $object->add_attribute('User ID', {}, $user->{'userId'});
        $object->add_attribute('Original ID', {}, $user->{'originalId'});
        $object->add_attribute('First Name', {}, $user->{'firstName'});
        $object->add_attribute('Last Name', {}, $user->{'lastName'});
        $object->add_attribute('Email', {}, $user->{'emails'});
        console_print_text($object);
    }
}

sub list_users()
{
    my (@args) = @_;
    my $request = {};
    if ( scalar(@args) >= 1 ) {
        $request->{'filter'} = $args[0];
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Authorization.listUsers', $request);
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'userId',   'title' => 'User ID'},
            {'field' => 'originalId', 'title' => 'Original ID'},
            {'field' => 'name',   'title' => 'Name'},
            {'field' => 'email',   'title' => 'Email'},
        ],
        'data' => []
    };
    foreach my $record (@{$response->{'items'}}) {
        my $email = '';
        if ( scalar(@{$record->{'emails'}}) > 0 ) {
            $email = $record->{'emails'}->[0];
        }
        my $originalId = $record->{'originalId'};
        if ( length($originalId) > 30 ) {
            $originalId = substr($originalId, 0, 30) . '...';
        }
        push(@{$table->{'data'}}, {
            'userId' => $record->{'userId'},
            'originalId' => $originalId,
            'name' => $record->{'firstName'} . ' ' . $record->{'lastName'},
            'email' => $email
        });
    }
    console_print_table($table);
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
    if ( defined($response) && !ref($response) ) {
        console_print_info("ACL record '%s' has been created.", $response);
    }
}

sub delete_acl()
{
    my ($options, @args) = @_;

    my $ids = [];

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
    if ( ref($user_id) ne 'HASH' || ref($entity_id) ne 'HASH' || ref($role) ne 'HASH' ) {
        my $application = Shongo::ClientCli->instance();
        my $response = $application->secure_request('Authorization.listAclRecords', $user_id, $entity_id, $role);
        if ( defined($response) ) {
            foreach my $record (@{$response}) {
                push(@{$ids}, $record->{'id'});
            }
        }
    }

    if ( scalar(@args) >= 1 ) {
        foreach my $arg (@args) {
            foreach my $id (split(/,/, $arg)) {
                push(@{$ids}, $id);
            }
        }
    }
    elsif ( scalar(@{$ids}) == 0 ) {
        console_print_error("Argument '<id>' must be specified.");
        return;
    }

    foreach my $id (@{$ids}) {
        my $response = Shongo::ClientCli->instance()->secure_request('Authorization.deleteAclRecord',
            RPC::XML::string->new($id)
        );
        if ( defined($response) ) {
            console_print_info("ACL record '%s' has been deleted.", $id);
        }
    }
}

sub get_acl()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<id>' must be specified.");
        return;
    }

    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listAclRecords', {
        'aclRecordIds' => [RPC::XML::string->new($args[0])]
    });
    if ( defined($response) ) {
        my $acl_record = $response->{'items'}->[0];
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('ACL Record');
        $object->add_attribute('Id', {}, $acl_record->{'id'});
        $object->add_attribute('User-id', {}, $acl_record->{'userId'});
        $object->add_attribute('Entity', {}, $acl_record->{'entityId'});
        $object->add_attribute('Role', {}, $acl_record->{'role'});
        console_print_text($object);
    }
}

sub list_acl()
{
    my ($options) = @_;
    my $request = {};
    if ( defined($options->{'user'}) ) {
        $request->{'userIds'} = [RPC::XML::string->new($options->{'user'})];
    }
    if ( defined($options->{'entity'}) ) {
        $request->{'entityIds'} = [RPC::XML::string->new($options->{'entity'})];
    }
    if ( defined($options->{'role'}) ) {
        $request->{'roles'} = [RPC::XML::string->new($options->{'role'})];
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Authorization.listAclRecords', $request);
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'id',     'title' => 'Id'},
            {'field' => 'user',   'title' => 'User'},
            {'field' => 'entity', 'title' => 'Entity'},
            {'field' => 'role',   'title' => 'Role'},
        ],
        'data' => []
    };
    foreach my $record (@{$response->{'items'}}) {
        push(@{$table->{'data'}}, {
            'id' => $record->{'id'},
            'user' => [$record->{'userId'}, $application->format_user($record->{'userId'})],
            'entity' => $record->{'entityId'},
            'role' => $record->{'role'},
        });
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
    my $entityId = $args[0];
    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listPermissions', {
        'entityIds' => [RPC::XML::string->new($entityId)],
    });
    my $table = {
        'columns' => [
            {'field' => 'permission', 'title' => 'Permission'}
        ],
        'data' => []
    };
    foreach my $permission (@{$response->{$entityId}->{'permissions'}}) {
        push(@{$table->{'data'}}, {
            'permission' => $permission
        });
    }
    console_print_table($table);
}

sub set_entity_user()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<entity-id> <user-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.setEntityUser',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1])
    );
    if ( defined($response) && !ref($response) ) {
        console_print_info("Entity '%s' user has been set to '%s'.", $args[0], $args[1]);
    }
}

sub list_referenced_users()
{
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.listReferencedUsers');
    my $table = {
        'columns' => [
            {'field' => 'userId', 'title' => 'User ID'},
            {'field' => 'description', 'title' => 'Referenced In'}
        ],
        'data' => []
    };
    foreach my $userId (sort(keys ${$response})) {
        push(@{$table->{'data'}}, {
            'userId' => $userId,
            'description' => $response->{$userId}
        });
    }
    console_print_table($table);
}

sub modify_user_id()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<old-user-id> <new-user-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.modifyUserId',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1])
    );
    if ( !ref($response) ) {
        console_print_info("User-id '%s' has been modified to '%s'.", $args[0], $args[1]);
    }
}

1;