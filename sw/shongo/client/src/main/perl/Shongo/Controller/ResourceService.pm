#
# Management of resources.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::ResourceService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Shell;
use Shongo::Controller::API::Resource;
use Shongo::Controller::API::DeviceResource;
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
        'create-resource' => {
            desc => 'Create a new resource',
            options => 'confirm',
            args => '[-confirm] [<json_attributes>]',
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
            args => '[identifier] [<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                modify_resource($args[0]);
            },
        },
        'delete-resource' => {
            desc => 'Delete an existing resource',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        delete_resource($identifier);
                    }
                } else {
                    delete_resource();
                }
            },
        },
        'list-resources' => {
            desc => 'List all existing resources',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_resources();
            },
        },
        'get-resource' => {
            desc => 'Get existing resource',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_resource($identifier);
                    }
                } else {
                    get_resource();
                }
            }
        },
        'get-resource-allocation' => {
            desc => 'Get information about resource allocations',
            options => 'interval=s',
            args => '[-interval] [identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_resource_allocation($identifier, $params->{'options'}->{'interval'});
                    }
                } else {
                    get_resource_allocation(undef, $params->{'options'}->{'interval'});
                }
            }
        },
    });
}

sub select_resource($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the resource', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
}

sub create_resource()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($resource) = @_;
        console_print_info("Creating resource...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Resource.createResource',
            $resource->to_xml()
        );
        if ( !$response->is_fault() ) {
            return $response->value();
        }
        return undef;
    };

    my $identifier = Shongo::Controller::API::Resource->create($attributes, $options);
    if ( defined($identifier) ) {
        console_print_info("Resource '%s' successfully created.", $identifier);
    }
}

sub modify_resource()
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

    my $options = {};
    $options->{'on_confirm'} = sub {
        my ($resource) = @_;
        console_print_info("Modifying resource...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Resource.modifyResource',
            $resource->to_xml()
        );
        if ( !$response->is_fault() ) {
            return $resource->{'identifier'};
        }
        return undef;
    };

    if ( !$result->is_fault ) {
        my $resource = Shongo::Controller::API::Resource->from_hash($result);
        if ( defined($resource) ) {
            $resource->modify(undef, $options);
        }
    }
}

sub delete_resource()
{
    my ($identifier) = @_;
    $identifier = select_resource($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    Shongo::Controller->instance()->secure_request(
        'Resource.deleteResource',
        RPC::XML::string->new($identifier)
    );
}

sub list_resources()
{
    my $response = Shongo::Controller->instance()->secure_request(
        'Resource.listResources'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Name', \' | ', 'Technologies', \' | ', 'Parent Resource', \' |');
    foreach my $resource (@{$response->value()}) {
        my $technologies = '';
        if (defined($resource->{'technologies'})) {
            foreach my $technology (split(/,/, $resource->{'technologies'})) {
                if ( length($technologies) ) {
                    $technologies .= ', ';
                }
                $technologies .= $Shongo::Controller::API::DeviceResource::Technology->{$technology};
            }
        }
        $table->add(
            $resource->{'identifier'},
            $resource->{'name'},
            $technologies,
            $resource->{'parentIdentifier'},
        );
    }
    console_print_table($table);
}

sub get_resource()
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
    if ( !$result->is_fault ) {
        my $resource = Shongo::Controller::API::Resource->from_hash($result);
        if ( defined($resource) ) {
            console_print_text($resource->to_string());
        }
    }
}

sub get_resource_allocation()
{
    my ($identifier, $interval) = @_;
    $identifier = select_resource($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    if (defined($interval)) {
        $interval = RPC::XML::string->new($interval);
    } else {
        $interval = RPC::XML::struct->new();
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Resource.getResourceAllocation',
        RPC::XML::string->new($identifier),
        $interval
    );
    if ( $result->is_fault ) {
        return
    }

    my $result_hash = $result->value();
    my $resource_allocation = Shongo::Controller::API::Object::->new();
    $resource_allocation->set_object_name('Resource Allocation');
    $resource_allocation->add_attribute('identifier');
    $resource_allocation->add_attribute('name');
    $resource_allocation->add_attribute('interval', {'type' => 'interval'});
    if ($result_hash->{'class'} eq 'VirtualRoomsResourceAllocation') {
        $resource_allocation->add_attribute('maximumPortCount', {'title' => 'Maximum Port Count'});
        $resource_allocation->add_attribute('availablePortCount', {'title' => 'Available Port Count'});
    }
    $resource_allocation->from_hash($result_hash);
    console_print_text($resource_allocation);

    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Slot', \' | ', 'Resource', \' | ', 'Type', \' |');
    foreach my $reservationXml (@{$resource_allocation->{'reservations'}}) {
        my $reservation = Shongo::Controller::API::Reservation->new($reservationXml->{'class'});
        $reservation->from_hash($reservationXml);
        $table->add(
            $reservation->{'identifier'},
            format_interval($reservation->{'slot'}),
            sprintf("%s (%s)", $reservation->{'resourceName'}, $reservation->{'resourceIdentifier'}),
            $reservation->to_string_short()
        );
    }
    printf(" %s\n", colored(uc("Reservations:"), $Shongo::Controller::API::Object::COLOR_HEADER));
    console_print_table($table, 1);
}

1;