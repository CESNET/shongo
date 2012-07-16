#
# Resource class - Management of resources.
#
package Shongo::Controller::ResourceService;

use strict;
use warnings;

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
            options => 'domain=s',
            args => '[-domain]',
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
        }
    });
}

#
# Create a new resource.
#
# @param hash map of attributes, the domain must be presented
#
sub create_resource()
{
    my ($attributes) = @_;

    my $domain = $attributes->{"domain"};
    if (defined($domain) == 0) {
        print("[ERROR] You must specify 'domain' for a new resource.\n");
        return;
    }

    print("[TODO] Create resource with domain=${domain}.\n");
}

#
# Modify an existing resource.
#
# @param hash map of attributes, the id must be presented
#
sub modify_resource()
{
    my ($identifier) = @_;

    if (defined($identifier) == 0) {
        console_print_error("You must specify 'identifier' for the resource to be modified.\n");
        return;
    }

    print("[TODO] Modify resource with id=$identifier\n");
}

#
# Delete an existing resource.
#
# @param id
#
sub delete_resource()
{
    my ($identifier) = @_;
    if (defined($identifier) == 0) {
        console_print_error("You must specify 'identifier' for the resource to be deleted.\n");
        return;
    }

    print("TODO: Delete resource with id=$identifier\n");
}

1;