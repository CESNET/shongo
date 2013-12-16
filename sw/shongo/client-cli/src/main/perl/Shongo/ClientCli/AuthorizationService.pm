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
            options => 'principal-name',
            args => '[--principal-name] <user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_user($params->{'options'}, @args);
            }
        },
        'list-users' => {
            desc => 'List users',
            options => 'group=s',
            args => '[-group=<group-id>] [<filter>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_users($params->{'options'}, @args);
            }
        },
        'list-groups' => {
            desc => 'List user groups',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_groups();
            }
        },
        'create-group' => {
            desc => 'Create user group',
            args => '<name> <description>',
            method => sub {
                my ($shell, $params, @args) = @_;
                create_group(@args);
            }
        },
        'delete-group' => {
            desc => 'Delete user group',
            args => '<group-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_group(@args);
            }
        },
        'add-group-user' => {
            desc => 'Add user to group',
            args => '<group-id> <user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                add_group_user(@args);
            }
        },
        'remove-group-user' => {
            desc => 'Remove user from group',
            args => '<group-id> <user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                remove_group_user(@args);
            }
        },
        'create-acl' => {
            desc => 'Create ACL entry',
            args => '<user-id> <object-id> <role>',
            method => sub {
                my ($shell, $params, @args) = @_;
                create_acl(@args);
            }
        },
        'delete-acl' => {
            desc => 'Delete ACL entry',
            options => 'user=s object=s role=s',
            args => '<id> [-user=<user-id>] [-object=<object-id>] [-role=<role>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_acl($params->{'options'}, @args);
            }
        },
        'get-acl' => {
            desc => 'Get ACL entry',
            args => '<id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_acl(@args);
            }
        },
        'list-acl' => {
            desc => 'List ACL entries',
            options => 'user=s object=s role=s',
            args => '[-user=<user-id>] [-object=<object-id>] [-role=<role>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_acl($params->{'options'});
            }
        },
        'list-permissions' => {
            desc => 'List permissions for authorized user to a object',
            args => '<object-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_permissions(@args);
            }
        },
        'set-object-user' => {
            desc => 'Change object user',
            args => '<object-id> <user-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                set_object_user(@args);
            }
        },
        'list-referenced-users' => {
            desc => 'List referenced users by any object',
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
    my ($options, @args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<user-id>' must be specified.");
        return;
    }
    my $request = {};
    if (defined($options->{'principal-name'})) {
        $request->{'principalName'} = RPC::XML::string->new($args[0]);
    }
    else {
        $request->{'userIds'} = [RPC::XML::string->new($args[0])];
    }

    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listUsers', $request);
    if ( defined($response) ) {
        my $user = $response->{'items'}->[0];
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('User Information');
        $object->add_attribute('User ID', {}, $user->{'userId'});
        $object->add_attribute('Principal Names', {'type' => 'collection'}, $user->{'principalNames'});
        $object->add_attribute('First Name', {}, $user->{'firstName'});
        $object->add_attribute('Last Name', {}, $user->{'lastName'});
        $object->add_attribute('Email', {}, $user->{'emails'});
        $object->add_attribute('Organization', {}, $user->{'organization'});
        console_print_text($object);
    }
}

sub list_users()
{
    my ($options, @args) = @_;
    my $request = {};
    if ( scalar(@args) >= 1 ) {
        $request->{'filter'} = $args[0];
    }
    if ( defined($options->{'group'}) ) {
        $request->{'groupIds'} = [$options->{'group'}];
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Authorization.listUsers', $request);
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'userId',   'title' => 'User ID'},
            {'field' => 'name',   'title' => 'Name'},
            {'field' => 'email',   'title' => 'Email'},
        ],
        'data' => []
    };
    foreach my $entry (@{$response->{'items'}}) {
        my $email = '';
        if ( defined($entry->{'email'}) ) {
            $email = $entry->{'email'};
        }
        push(@{$table->{'data'}}, {
            'userId' => $entry->{'userId'},
            'name' => $entry->{'firstName'} . ' ' . $entry->{'lastName'},
            'email' => $email
        });
    }
    console_print_table($table);
}

sub list_groups()
{
    my (@args) = @_;
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Authorization.listGroups');
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'id',          'title' => 'ID'},
            {'field' => 'parentId',    'title' => 'Parent ID'},
            {'field' => 'name',        'title' => 'Name'},
            {'field' => 'description', 'title' => 'Description'},
        ],
        'data' => []
    };
    foreach my $entry (@{$response}) {
        push(@{$table->{'data'}}, {
            'id' => $entry->{'id'},
            'parentId' => $entry->{'parentId'},
            'name' => $entry->{'name'},
            'description' => $entry->{'description'}
        });
    }
    console_print_table($table);
}

sub create_group()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<name> <description>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.createGroup', {
        'name' => RPC::XML::string->new($args[0]),
        'description' => RPC::XML::string->new($args[1])
    });
    if ( defined($response) && !ref($response) ) {
        console_print_info("Group '%s' has been created.", $response);
    }
}

sub delete_group()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Argument '<group-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.deleteGroup',
        RPC::XML::string->new($args[0])
    );
    if ( defined($response) ) {
        console_print_info("Group '%s' has been deleted.", $args[0]);
    }
}

sub add_group_user()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<group-id> <user-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.addGroupUser',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1])
    );
    if ( defined($response) ) {
        console_print_info("User '%s' has been added to group '%s'.", $args[1], $args[0]);
    }
}

sub remove_group_user()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<group-id> <user-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.removeGroupUser',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1])
    );
    if ( defined($response) ) {
        console_print_info("User '%s' has been removed from group '%s'.", $args[1], $args[0]);
    }
}

sub create_acl()
{
    my (@args) = @_;
    if ( scalar(@args) < 3 ) {
        console_print_error("Arguments '<user-id> <object-id> <role>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.createAclEntry', {
        'identityType' => 'USER',
        'identityPrincipalId' => RPC::XML::string->new($args[0]),
        'objectId' => RPC::XML::string->new($args[1]),
        'role' => RPC::XML::string->new($args[2])
    });
    if ( defined($response) && !ref($response) ) {
        console_print_info("ACL entry '%s' has been created.", $response);
    }
}

sub delete_acl()
{
    my ($options, @args) = @_;

    my $ids = [];

    my $user_id = {};
    my $objectId = {};
    my $role = {};
    if ( defined($options->{'user'}) ) {
        $user_id = RPC::XML::string->new($options->{'user'});
    }
    if ( defined($options->{'object'}) ) {
        $objectId = RPC::XML::string->new($options->{'object'});
    }
    if ( defined($options->{'role'}) ) {
        $role = RPC::XML::string->new($options->{'role'});
    }
    if ( ref($user_id) ne 'HASH' || ref($objectId) ne 'HASH' || ref($role) ne 'HASH' ) {
        my $application = Shongo::ClientCli->instance();
        my $response = $application->secure_request('Authorization.listAclEntries', $user_id, $objectId, $role);
        if ( defined($response) ) {
            foreach my $entry (@{$response}) {
                push(@{$ids}, $entry->{'id'});
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
        my $response = Shongo::ClientCli->instance()->secure_request('Authorization.deleteAclEntry',
            RPC::XML::string->new($id)
        );
        if ( defined($response) ) {
            console_print_info("ACL entry '%s' has been deleted.", $id);
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

    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listAclEntry', {
        'entryIds' => [RPC::XML::string->new($args[0])]
    });
    if ( defined($response) ) {
        my $entry = $response->{'items'}->[0];
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_name('ACL Entry');
        $object->add_attribute('Id', {}, $entry->{'id'});
        $object->add_attribute('User-id', {}, $entry->{'userId'});
        $object->add_attribute('Object', {}, $entry->{'objectId'});
        $object->add_attribute('Role', {}, $entry->{'role'});
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
    if ( defined($options->{'object'}) ) {
        $request->{'objectIds'} = [RPC::XML::string->new($options->{'object'})];
    }
    if ( defined($options->{'role'}) ) {
        $request->{'roles'} = [RPC::XML::string->new($options->{'role'})];
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Authorization.listAclEntries', $request);
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'id',        'title' => 'Id'},
            {'field' => 'user',      'title' => 'User'},
            {'field' => 'object',    'title' => 'Object'},
            {'field' => 'role',      'title' => 'Role'},
            {'field' => 'deletable', 'title' => 'Deletable'},
        ],
        'data' => []
    };
    foreach my $entry (@{$response->{'items'}}) {
        push(@{$table->{'data'}}, {
            'id' => $entry->{'id'},
            'user' => [$entry->{'userId'}, $application->format_user($entry->{'userId'})],
            'object' => $entry->{'objectId'},
            'role' => $entry->{'role'},
            'deletable' => $entry->{'deletable'} ? 'yes' : 'no',
        });
    }
    console_print_table($table);
}

sub list_permissions()
{
    my (@args) = @_;
    if ( scalar(@args) < 1 ) {
        console_print_error("Arguments '<object-id>' must be specified.");
        return;
    }
    my $objectId = $args[0];
    my $response = Shongo::ClientCli->instance()->secure_hash_request('Authorization.listObjectPermissions', {
        'objectIds' => [RPC::XML::string->new($objectId)],
    });
    my $table = {
        'columns' => [
            {'field' => 'permission', 'title' => 'Permission'}
        ],
        'data' => []
    };
    foreach my $permission (@{$response->{$objectId}->{'objectPermissions'}}) {
        push(@{$table->{'data'}}, {
            'permission' => $permission
        });
    }
    console_print_table($table);
}

sub set_object_user()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<object-id> <user-id>' must be specified.");
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.setObjectUser',
        RPC::XML::string->new($args[0]),
        RPC::XML::string->new($args[1])
    );
    if ( defined($response) && !ref($response) ) {
        console_print_info("Object '%s' user has been set to '%s'.", $args[0], $args[1]);
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
    foreach my $userId (sort(keys %{$response})) {
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