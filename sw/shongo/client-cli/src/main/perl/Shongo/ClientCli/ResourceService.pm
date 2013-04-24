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
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        delete_resource($id);
                    }
                } else {
                    delete_resource();
                }
            },
        },
        'list-resources' => {
            desc => 'List all existing resources',
            options => 'user=s',
            args => '[-user=*|<user-id>]',
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
            $resource->modify($attributes, $options);
        }
    }
}

sub delete_resource()
{
    my ($id) = @_;
    $id = select_resource($id);
    if ( !defined($id) ) {
        return;
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
        $filter->{'userId'} = $options->{'user'};
    }
    my $application = Shongo::ClientCli->instance();
    my $response = $application->secure_request('Resource.listResources', $filter);
    if ( !defined($response) ) {
        return
    }

    my $table = {
        'columns' => [
            {'field' => 'id',   'title' => 'Identifier'},
            {'field' => 'user', 'title' => 'User'},
            {'field' => 'name', 'title' => 'Name'},
            {'field' => 'technologies', 'title' => 'Technologies'},
            {'field' => 'parent', 'title' => 'Parent Resource'},
        ],
        'data' => []
    };
    foreach my $resource (@{$response}) {
        my $technologies = '';
        if (defined($resource->{'technologies'})) {
            foreach my $technology (split(/,/, $resource->{'technologies'})) {
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

1;