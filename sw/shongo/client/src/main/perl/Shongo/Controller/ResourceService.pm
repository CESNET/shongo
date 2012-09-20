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
            options => 'name=s technology=s@ capability=s@',
            args => '[-name] [-technology]',
            method => sub {
                my ($shell, $params, @args) = @_;
                create_resource($params->{'options'});
            },
        },
        'modify-resource' => {
            desc => 'Modify an existing resource',
            args => '[identifier]',
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
                delete_resource($args[0]);
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
    my ($attributes) = @_;

    my $type = console_read_enum('Select type of resource', ordered_hash(
        'Resource' => 'Other Resource',
        'DeviceResource' => 'Device Resource'
    ));
    if ( !defined($type) ) {
        return;
    }
    my $identifier = undef;
    if ($type eq 'Resource') {
        $identifier = Shongo::Controller::API::Resource->new()->create($attributes);
    } elsif ($type eq 'DeviceResource') {
        $identifier = Shongo::Controller::API::DeviceResource->new()->create($attributes);
    }
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
    if ( !$result->is_fault ) {
        my $resource = Shongo::Controller::API::Resource->from_xml($result);
        if ( defined($resource) ) {
            $resource->modify();
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
        my $resource = Shongo::Controller::API::Resource->from_xml($result);
        if ( defined($resource) ) {
            printf("\n%s\n", $resource->to_string());
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
    my $resource_allocation = $result->value();
    print("\n RESOURCE ALLOCATION\n");
    printf("           Identifier: %s\n", $resource_allocation->{'identifier'});
    printf("                 Name: %s\n", $resource_allocation->{'name'});
    printf("             Interval: %s\n", format_interval($resource_allocation->{'interval'}));
    if ($resource_allocation->{'class'} eq 'VirtualRoomsResourceAllocation') {
        printf("   Maximum Port Count: %d\n", $resource_allocation->{'maximumPortCount'});
        printf(" Available Port Count: %d\n", $resource_allocation->{'availablePortCount'});
    }
    print(" Reservations:\n");
    my $index = 0;
    foreach my $reservationXml (@{$resource_allocation->{'reservations'}}) {
        my $reservation = Shongo::Controller::API::Reservation->new($reservationXml->{'class'});
        $reservation->from_xml($reservationXml);
        $reservation->fetch_child_reservations(1);
        $index++;
        printf(" %d)%s\n", $index, indent_block($reservation->to_string(), 0, 4));
    }
    if ($index == 0) {
        print("  -- None -- \n\n");
    }
}

1;