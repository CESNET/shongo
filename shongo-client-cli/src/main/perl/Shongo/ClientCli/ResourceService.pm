#
# Management of resources.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::ResourceService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Shell;
use Shongo::ClientCli::API::Resource;
use Shongo::ClientCli::API::DeviceResource;
use Shongo::ClientCli::API::Alias;

#
# Tag types
#
our $TagType = ordered_hash(
    'DEFAULT' => 'Default',
    'NOTIFY_EMAIL' => 'Notify Email',
    'RESERVATION_DATA' => 'Reservation Data',
);

#
# Populate shell by options for management of resources.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'create-resource' => {
            desc => 'Create a new resource',
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    create_resource($attributes, $params->{'options'});
                }
            },
        },
        'modify-resource' => {
            desc => 'Modify an existing resource',
            args => '[id] [<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    modify_resource($args[0], $attributes, $params->{'options'});
                }
            },
        },
        'delete-resource' => {
            desc => 'Delete an existing resource',
            options => 'force',
            args => '[id] [--force]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        delete_resource($params->{'options'}, $id);
                    }
                } else {
                    delete_resource($params->{'options'});
                }
            },
        },
        'list-resources' => {
            desc => 'List all existing resources',
            options => 'user=s name=s tag=s domain=s',
            args => '[-user=*|<user-id>] [-name=<name>] [-tag=<name>] [-domain=<domain-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_resources($params->{'options'});
            },
        },
        'get-resource' => {
            desc => 'Get existing resource',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_resource($id);
                    }
                } else {
                    get_resource();
                }
            }
        },
        'get-resource-allocation' => {
            desc => 'Get information about resource allocations',
            options => 'interval=s',
            args => '[-interval] [id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        get_resource_allocation($id, $params->{'options'}->{'interval'});
                    }
                } else {
                    get_resource_allocation(undef, $params->{'options'}->{'interval'});
                }
            }
        },
        'list-tags' => {
            desc => 'List tags',
            options => 'resource=s',
            args => '[-resource=<resource-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_tags($params->{'options'});
            },
        },
        'create-tag' => {
            desc => 'Create a new tag',
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    create_tag($attributes, $params->{'options'});
                }
            },
        },
        'delete-tag' => {
            desc => 'Delete an existing tag',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        delete_tag($id);
                    }
                } else {
                    delete_tag();
                }
            },
        },
        'assign-tag' => {
            desc => 'Assign tag to resource',
            options => 'resource=s tag=s',
            args => '[<resource-id>] [<tag-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                assign_resource_tag(@args);
            },
        },
        'remove-tag' => {
            desc => 'Remove tag from resource',
            options => 'resource=s tag=s',
            args => '[<resource-id>] [<tag-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                remove_resource_tag(@args);
            },
        },
    });
}

sub select_resource
{
    my ($id, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'id'}) ) {
        $id = $attributes->{'id'};
    }
    $id = console_read_value('Identifier of the resource', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}

sub create_resource()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($resource) = @_;
        console_print_info("Creating resource...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.createResource',
            $resource->to_xml()
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $id = Shongo::ClientCli::API::Resource->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Resource '%s' successfully created.", $id);
    }
}

sub modify_resource()
{
    my ($id, $attributes, $options) = @_;
    $id = select_resource($id, $attributes);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Resource.getResource',
        RPC::XML::string->new($id)
    );

    $options->{'on_confirm'} = sub {
        my ($resource) = @_;
        console_print_info("Modifying resource...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.modifyResource',
            $resource->to_xml()
        );
        if ( defined($response) ) {
            return $resource->{'id'};
        }
        return undef;
    };

    if ( defined($response) ) {
        my $resource = Shongo::ClientCli::API::Resource->from_hash($response);
        if ( defined($resource) ) {
            my $new_id = $resource->modify($attributes, $options);
            if ( defined($new_id) ) {
                console_print_info("Resource '%s' successfully modified to '%s'.", $id, $new_id);
            }
        }
    }
}

sub delete_resource()
{
    my ($options, $id) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
    }

    if ( defined($options->{'force'}) ) {
        my $reservation_requests = {};
        # add reservation requests
        my $response = Shongo::ClientCli->instance()->secure_hash_request('Reservation.listReservationRequests', {
            'specificationResourceId' => $id,
            'history' => 1
        });
        if ( !defined($response) ) {
            return;
        }
        foreach my $reservation_request (@{$response->{'items'}}) {
            $reservation_requests->{$reservation_request->{'id'}} = undef;
        }
        # add reservation requests from reservations
        $response = Shongo::ClientCli->instance()->secure_hash_request('Reservation.listReservations', {
            'resourceIds' => [$id]
        });
        if ( !defined($response) ) {
            return;
        }
        foreach my $reservation (@{$response->{'items'}}) {
            $reservation_requests->{$reservation->{'reservationRequestId'}} = undef;
        }

        # Delete reservation requests
        if (scalar(keys %{$reservation_requests}) > 0) {
            console_print_info("Reservation Requests referencing the resource:");
            foreach my $reservation_request_id (keys %{$reservation_requests}) {
                console_print_info(" %s", $reservation_request_id);
            }
            my $result = console_read("Do you want to delete the reservation requests? [y/n]");
            if ($result eq 'y') {
                foreach my $reservation_request_id (keys %{$reservation_requests}) {
                    Shongo::ClientCli->instance()->secure_request(
                        'Reservation.deleteReservationRequestHard',
                        RPC::XML::string->new($reservation_request_id)
                    );
                }
            }
        }

        # Delete executables
        $response = Shongo::ClientCli->instance()->secure_hash_request('Executable.listExecutables', {
            'resourceId' => $id,
            'history' => 1
        });
        if ( !defined($response) ) {
            return
        }
        if (scalar(@{$response->{'items'}}) > 0) {
            console_print_info("Executables referencing the resource:");
            foreach my $executable (@{$response->{'items'}}) {
                console_print_info(" %s", $executable->{'id'});
            }
            my $result = console_read("Do you want to delete the executables? [y/n]");
            if ($result eq 'y') {
                foreach my $executable (@{$response->{'items'}}) {
                    Shongo::ClientCli->instance()->secure_request(
                        'Executable.deleteExecutable',
                        RPC::XML::string->new($executable->{'id'})
                    );
                }
            }
        }
    }

    Shongo::ClientCli->instance()->secure_request(
        'Resource.deleteResource',
        RPC::XML::string->new($id)
    );
}

sub list_resources()
{
    my ($options) = @_;
    my $filter = {};
    if ( defined($options->{'user'}) ) {
        $filter->{'userIds'} = [$options->{'user'}];
    }
    if ( defined($options->{'name'}) ) {
        $filter->{'name'} = $options->{'name'};
    }
    if ( defined($options->{'tag'}) ) {
        $filter->{'tagName'} = $options->{'tag'};
    }
    if ( defined($options->{'domain'}) ) {
        $filter->{'domainId'} = $options->{'domain'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Resource.listResources', $filter);
    if ( !defined($response) ) {
        return
    }

    my $table = {
        'columns' => [
            {'field' => 'id',           'title' => 'Identifier'},
            {'field' => 'user',         'title' => 'User'},
            {'field' => 'name',         'title' => 'Name'},
            {'field' => 'allocatable',  'title' => 'Allocatable'},
            {'field' => 'calendarPublic','title' => 'Calendar Public'},
            {'field' => 'confirmByOwner','title' => 'Confirm request'},
            {'field' => 'remoteCalendarName', 'title' => 'Remote calendar name'},
            {'field' => 'order',        'title' => 'Order'},
            {'field' => 'technologies', 'title' => 'Technologies'},
            {'field' => 'parent',       'title' => 'Parent Resource'},
        ],
        'data' => []
    };
    foreach my $resource (@{$response->{'items'}}) {
        my $technologies = '';
        if (defined($resource->{'technologies'})) {
            foreach my $technology (@{$resource->{'technologies'}}) {
                if ( length($technologies) ) {
                    $technologies .= ', ';
                }
                $technologies .= $Shongo::ClientCli::API::DeviceResource::Technology->{$technology};
            }
        }
        push(@{$table->{'data'}}, {
            'id' => $resource->{'id'},
            'user' => [$resource->{'userId'}, $application->format_user($resource->{'userId'})],
            'name' => $resource->{'name'},
            'allocatable' => $resource->{'allocatable'} ? 'yes' : 'no',
            'calendarPublic' => $resource->{'calendarPublic'} ? 'yes' : 'no',
            'confirmByOwner' => $resource->{'confirmByOwner'} ? 'yes' : 'no',
            'remoteCalendarName' => $resource->{'remoteCalendarName'},
            'order' => $resource->{'allocationOrder'},
            'technologies' => [$resource->{'technologies'}, $technologies],
            'parent' => $resource->{'parentResourceId'},
        });
    }
    console_print_table($table);
}

sub get_resource()
{
    my ($id) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Resource.getResource',
        RPC::XML::string->new($id)
    );
    if ( defined($response) ) {
        my $resource = Shongo::ClientCli::API::Resource->from_hash($response);
        if ( defined($resource) ) {
            console_print_text($resource);
        }
    }
}

sub get_resource_allocation()
{
    my ($id, $interval) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
    }
    if (defined($interval)) {
        $interval = RPC::XML::string->new($interval);
    } else {
        $interval = RPC::XML::struct->new();
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Resource.getResourceAllocation',
        RPC::XML::string->new($id),
        $interval
    );
    if ( !defined($response) ) {
        return
    }

    my $resource_allocation = Shongo::ClientCli::API::Object::->new();
    $resource_allocation->set_object_name('Resource Allocation');
    $resource_allocation->add_attribute('id', {'title' => 'Identifier'});
    $resource_allocation->add_attribute('name');
    $resource_allocation->add_attribute('interval', {'type' => 'interval'});
    if ($response->{'class'} eq 'RoomProviderResourceAllocation') {
        $resource_allocation->add_attribute('maximumLicenseCount', {'title' => 'Maximum License Count'});
        $resource_allocation->add_attribute('availableLicenseCount', {'title' => 'Available License Count'});
    }
    $resource_allocation->from_hash($response);
    console_print_text($resource_allocation);


    my $table = {
        'columns' => [
            {'field' => 'id',   'title' => 'Identifier'},
            {'field' => 'slot', 'title' => 'Slot'},
            {'field' => 'resource', 'title' => 'Resource'},
            {'field' => 'type', 'title' => 'Type'},
        ],
        'data' => []
    };
    foreach my $reservationXml (@{$resource_allocation->{'reservations'}}) {
        my $reservation = Shongo::ClientCli::API::Reservation->new($reservationXml->{'class'});
        $reservation->from_hash($reservationXml);
        push(@{$table->{'data'}}, {
            'id' => $reservation->{'id'},
            'slot' => [$reservation->{'slot'}, interval_format($reservation->{'slot'})],
            'resource' => [$reservation->{'resourceId'}, sprintf("%s (%s)", $reservation->{'resourceName'}, $reservation->{'resourceId'})],
            'type' => [$reservation->{'class'}, $reservation->to_string_short()]
        });
    }
    if ( !Shongo::ClientCli::is_scripting() ) {
        printf(" %s\n", colored(uc("Reservations:"), $Shongo::ClientCli::API::Object::COLOR_HEADER));
    }
    console_print_table($table, 1);
}

sub create_tag()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($tag) = @_;
        console_print_info("Creating tag...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.createTag',
            $tag->to_xml()
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $tag = Shongo::ClientCli::API::Object->new();

    $tag->set_object_class('Tag');
    $tag->set_object_name('Tag');
    $tag->add_attribute(
        'id', {
            'title' => 'Identifier',
            'editable' => 0
        }
    );
    $tag->add_attribute(
        'name', {
            'required' => 1,
            'title' => 'Tag name',
        }
    );
    $tag->add_attribute(
        'type', {
            'required' => 1,
            'title' => 'Tag type',
            'type' => 'enum',
            'enum' => $Shongo::ClientCli::ResourceService::TagType,
        }
    );
    $tag->add_attribute(
        'data', {
            'title' => 'Tag data',
        }
    );

    my $id = $tag->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Tag '%s' successfully created.", $id);
    }
}

sub delete_tag()
{
    my ($id) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
    }
    Shongo::ClientCli->instance()->secure_request(
        'Resource.deleteTag',
        RPC::XML::string->new($id)
    );
}

sub list_tags()
{
    my ($options) = @_;
    my $filter = {};
    if ( defined($options->{'resource'}) ) {
        $filter->{'resourceId'} = $options->{'resource'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_hash_request('Resource.listTags', $filter);
    if ( !defined($response) ) {
        return
    }

    my $table = {
        'columns' => [
            {'field' => 'id',           'title' => 'Identifier'},
            {'field' => 'name',         'title' => 'Name'},
            {'field' => 'type',         'title' => 'Type'},
            {'field' => 'data',         'title' => 'Data'},
        ],
        'data' => []
    };
    foreach my $tag (@{$response}) {
        push(@{$table->{'data'}}, {
            'id' => $tag->{'id'},
            'name' => $tag->{'name'},
            'type' => $tag->{'type'},
            'data' => $tag->{'data'},
        });
    }
    console_print_table($table);
}

sub assign_resource_tag()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<resource-id> <tag-id>' must be specified.");
        return;
    }
    my $resource_id = $args[0];
    my $tag_id = $args[1];
    Shongo::ClientCli->instance()->secure_request(
        'Resource.assignResourceTag',
        RPC::XML::string->new($resource_id),
        RPC::XML::string->new($tag_id),
    );
}

sub remove_resource_tag()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<resource-id> <tag-id>' must be specified.");
        return;
    }
    my $resource_id = $args[0];
    my $tag_id = $args[1];
    Shongo::ClientCli->instance()->secure_request(
        'Resource.removeResourceTag',
        RPC::XML::string->new($resource_id),
        RPC::XML::string->new($tag_id),
    );
}

1;