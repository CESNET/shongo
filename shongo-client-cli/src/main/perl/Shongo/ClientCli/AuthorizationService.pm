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
use Shongo::ClientCli::API::Group;

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
        'get-user-settings' => {
            desc => 'Get user settings',
            args => '[<user-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_user_settings(@args);
            }
        },
        'modify-user-settings' => {
            desc => 'Modify user settings',
            args => '[<user-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_user_settings(@args);
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
            options => 'type=s',
            args => '[-type=USER|SYSTEM]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_groups($params->{'options'});
            }
        },
        'get-group' => {
            desc => 'Get existing group',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_group($id);
                    }
                } else {
                    get_group();
                }
            }
        },
        'create-group' => {
            desc => 'Create user group',
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    create_group($attributes, $params->{'options'});
                }
            }
        },
        'modify-group' => {
            desc => 'Modify user group',
            args => '<group-id>',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_group(@args);
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
            args => '[user/group:]<user-id> <object-id> <role>',
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
        $object->add_attribute('Email', {}, $user->{'email'});
        $object->add_attribute('Organization', {}, $user->{'organization'});
        console_print_text($object);
    }
}

sub get_user_settings_object
{
    my (@args) = @_;
    my $response;
    if ( scalar(@args) == 0 ) {
        $response = Shongo::ClientCli->instance()->secure_request('Authorization.getUserSettings');
    }
    else {
        $response = Shongo::ClientCli->instance()->secure_request('Authorization.getUserSettings',
            RPC::XML::string->new($args[0])
        );
    }
    if ( defined($response) ) {
        my $user_settings = $response;
        my $object = Shongo::ClientCli::API::Object->new();
        $object->set_object_class('UserSettings');
        $object->set_object_name('User Settings');
        $object->add_attribute('useWebService', {
            'title' => 'Use Web Service',
            'type' => 'bool'
        });
        $object->add_attribute('systemAdministratorNotifications', {
            'title' => 'System Administrator Notifications',
            'type' => 'bool'
        });
        $object->add_attribute('resourceAdministratorNotifications', {
            'title' => 'Resource Administrator Notifications',
            'type' => 'bool'
        });
        $object->from_hash($user_settings);
        return $object;
    }
}

sub get_user_settings()
{
    my (@args) = @_;
    my $object = get_user_settings_object(@args);
    if ( defined($object) ) {
        console_print_text($object);
    }
}

sub modify_user_settings()
{
    my (@args) = @_;
    my $object = get_user_settings_object(@args);
    if ( defined($object) ) {
        $object->modify(undef, {
            'on_confirm' => sub {
                my ($object) = @_;
                console_print_info("Modifying user settings...");
                if ( scalar(@args) == 0 ) {
                    Shongo::ClientCli->instance()->secure_request('Authorization.updateUserSettings',
                        $object->to_xml()
                    );
                }
                else {
                    Shongo::ClientCli->instance()->secure_request('Authorization.updateUserSettings',
                        RPC::XML::string->new($args[0]),
                        $object->to_xml()
                    );
                }
                return 1;
            }
        });
    }
}

sub list_users()
{
    my ($options, @args) = @_;
    my $request = {};
    if ( scalar(@args) >= 1 ) {
        $request->{'search'} = $args[0];
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
    my ($options, @args) = @_;
    my $application = Shongo::ClientCli->instance();
    my $request = {};
    if ( defined($options->{'type'}) ) {
        $request->{'groupTypes'} = [];
        foreach my $groupType (split(/,/, $options->{'type'})) {
            push(@{$request->{'groupTypes'}}, $groupType);
        }
    }
    my $response = $application->secure_hash_request('Authorization.listGroups', $request);
    if ( !defined($response) ) {
        return;
    }
    my $table = {
        'columns' => [
            {'field' => 'id',            'title' => 'ID'},
            {'field' => 'type',          'title' => 'Type'},
            {'field' => 'parentGroupId', 'title' => 'Parent ID'},
            {'field' => 'name',          'title' => 'Name'},
            {'field' => 'description',   'title' => 'Description'},
        ],
        'data' => []
    };
    foreach my $entry (@{$response->{'items'}}) {
        push(@{$table->{'data'}}, {
            'id' => $entry->{'id'},
            'type' => $entry->{'type'},
            'parentGroupId' => $entry->{'parentGroupId'},
            'name' => $entry->{'name'},
            'description' => $entry->{'description'}
        });
    }
    console_print_table($table);
}

sub get_group()
{
    my ($id) = @_;
    $id = select_group($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Authorization.getGroup',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        my $resource = Shongo::ClientCli::API::Group->from_hash($response);
        if ( defined($resource) ) {
            console_print_text($resource);
        }
    }
}

sub select_group
{
    my ($id, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'id'}) ) {
        $id = $attributes->{'id'};
    }
    $id = console_read_value('Identifier of the group', 0, undef, $id);
    return $id;
}

sub create_group()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($group) = @_;
        console_print_info("Creating group...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Authorization.createGroup',
            $group->to_xml()
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $id = Shongo::ClientCli::API::Group->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Group '%s' successfully created.", $id);
    }
}

sub modify_group()
{
    my ($id, $attributes, $options) = @_;
    $id = select_group($id, $attributes);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Authorization.getGroup',
        RPC::XML::string->new($id)
    );

    $options->{'on_confirm'} = sub {
        my ($resource) = @_;
        console_print_info("Modifying group...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Authorization.modifyGroup',
            $resource->to_xml()
        );
        if ( defined($response) ) {
            return $resource->{'id'};
        }
        return undef;
    };

    if ( defined($response) ) {
        my $resource = Shongo::ClientCli::API::Group->from_hash($response);
        if ( defined($resource) ) {
            $resource->modify($attributes, $options);
        }
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
        console_print_error("Arguments '[user|group:]<user-id> <object-id> <role>' must be specified.");
        return;
    }
    my $principalId =  RPC::XML::string->new($args[0]);
    my $principalType = 'USER';

    my $type = (split /:/, $$principalId)[0];
    my $formatedId = (split /:/, $$principalId)[1];

    if ( $type eq 'group' ) {
        $principalType = 'GROUP';
    }
    else {
        if ( ! $type eq 'user' ) {
            $formatedId = $$principalId;
        }
    }

    my $response = Shongo::ClientCli->instance()->secure_request('Authorization.createAclEntry', {
        'identityType' => $principalType,
        'identityPrincipalId' => $formatedId,
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
            {'field' => 'id',                  'title' => 'Id'},
            {'field' => 'identityType',        'title' => 'Type'},
            {'field' => 'identityPrincipalId', 'title' => 'User/Group'},
            {'field' => 'object',              'title' => 'Object'},
            {'field' => 'role',                'title' => 'Role'},
            {'field' => 'deletable',           'title' => 'Deletable'},
        ],
        'data' => []
    };
    if ( !Shongo::ClientCli::is_scripting() ) {
        splice(@{$table->{'columns'}}, 1, 1);
    }
    foreach my $entry (@{$response->{'items'}}) {
        my $identity = $entry->{'identityPrincipalId'};
        if ( $entry->{'identityType'} eq 'USER' ) {
            $identity = $application->format_user($entry->{'identityPrincipalId'});
        }
        elsif ( $entry->{'identityType'} eq 'GROUP' ) {
            $identity = $application->format_group($entry->{'identityPrincipalId'});
        }
        push(@{$table->{'data'}}, {
            'id' => $entry->{'id'},
            'identityType' => $entry->{'identityType'},
            'identityPrincipalId' => [$entry->{'identityPrincipalId'}, $identity],
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
            {'field' => 'name', 'title' => 'Name'},
            {'field' => 'email', 'title' => 'Email'},
            {'field' => 'description', 'title' => 'Referenced In'}
        ],
        'data' => []
    };
    foreach my $referenced_user (@{$response}) {
        my $user = $referenced_user->{'userInformation'};
        my $name = '';
        if ( defined($user->{'firstName'}) ) {
            $name .= $user->{'firstName'};
        }
        if ( defined($user->{'lastName'}) ) {
            if ( $name ne '' ) {
                $name .= ' ';
            }
            $name .= $user->{'lastName'};
        }
        my $email = '';
        if ( defined($user->{'email'}) ) {
            $email = $user->{'email'};
        }
        my $description = '';
        if ( defined($referenced_user->{'reservationRequestCount'}) && $referenced_user->{'reservationRequestCount'} > 0 ) {
            if ($description ne '' ) {
                $description .= ', ';
            }
            $description .= $referenced_user->{'reservationRequestCount'} . ' requests';
        }
        if ( defined($referenced_user->{'resourceCount'}) && $referenced_user->{'resourceCount'} > 0 ) {
            if ($description ne '' ) {
                $description .= ', ';
            }
            $description .= $referenced_user->{'resourceCount'} . ' resources';
        }
        if ( defined($referenced_user->{'userSettingsCount'}) && $referenced_user->{'userSettingsCount'} > 0 ) {
            if ($description ne '' ) {
                $description .= ', ';
            }
            $description .= $referenced_user->{'userSettingsCount'} . ' settings';
        }
        if ( defined($referenced_user->{'aclEntryCount'}) && $referenced_user->{'aclEntryCount'} > 0 ) {
            if ($description ne '' ) {
                $description .= ', ';
            }
            $description .= $referenced_user->{'aclEntryCount'} . ' acls';
        }
        if ( defined($referenced_user->{'userPersonCount'}) && $referenced_user->{'userPersonCount'} > 0 ) {
            if ($description ne '' ) {
                $description .= ', ';
            }
            $description .= $referenced_user->{'userPersonCount'} . ' persons';
        }
        push(@{$table->{'data'}}, {
            'userId' => $user->{'userId'},
            'name' => $name,
            'email' => $email,
            'description' => $description
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
    if ( ref($response) && !%{$response} ) {
        console_print_info("User-id '%s' has been modified to '%s'.", $args[0], $args[1]);
    }
}

1;