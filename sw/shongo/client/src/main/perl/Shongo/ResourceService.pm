#
# Resource class - Management of resources.
#
package Shongo::ResourceService;

use strict;
use warnings;

#
# Populate shell by options for management of resources.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    my @tree = (
        'resource' => 'Management of resources',
        'resource create' => {
            help => 'Create a new resource',
            opts => 'domain=s',
            exec => sub {
                my ($shell, %p) = @_;
                create_resource(%p);
            },
        },
        'resource modify' => {
            help => 'Modify an existing resource',
            opts => 'id=i',
            exec => sub {
                my ($shell, %p) = @_;
                modify_resource(%p);
            },
        },
        'resource delete' => {
            help => 'Delete an existing resource',
            opts => 'id=i',
            exec => sub {
                my ($shell, %p) = @_;
                delete_resource($p{"id"});
            },
        }
    );
    $shell->populate(@tree);
}

#
# Create a new resource.
#
# @param hash map of attributes, the domain must be presented
#
sub create_resource()
{
    my (%attributes) = @_;

    my $domain = $attributes{"domain"};
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
    my (%attributes) = @_;

    my $id = $attributes{"id"};
    if (defined($id) == 0) {
        print("[ERROR] You must specify 'id' for the resource to be modified.\n");
        return;
    }

    print("[TODO] Modify resource with id=$id\n");
}

#
# Delete an existing resource.
#
# @param id
#
sub delete_resource()
{
    my ($id) = @_;
    if (defined($id) == 0) {
        print("[ERROR] You must specify 'id' for the resource to be deleted.\n");
        return;
    }

    print("TODO: Delete resource with id=$id\n");
}

1;