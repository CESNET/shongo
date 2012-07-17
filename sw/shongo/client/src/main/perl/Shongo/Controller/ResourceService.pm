#
# Resource class - Management of resources.
#
package Shongo::Controller::ResourceService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

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
            options => 'name=s',
            args => '[-name]',
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
        }
    });
}

sub select_resource($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the resource', 0, '\\d|shongo:.+:\\d', $identifier);
    return $identifier;
}

sub create_resource()
{
    my ($attributes) = @_;

    my $identifier = Shongo::Controller::API::Resource->new()->create($attributes);
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
        my $resource = Shongo::Controller::API::Resource->new()->from_xml($result);
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
    my $table = Text::Table->new(\'| ', 'Identifier', \' | ', 'Name', \' |');
    foreach my $resource (@{$response->value()}) {
        $table->add(
            $resource->{'identifier'},
            $resource->{'name'}
        );
    }
    console_print_table($table);
}

1;