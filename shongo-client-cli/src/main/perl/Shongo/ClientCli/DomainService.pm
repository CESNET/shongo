#
# Management of domains.
#
# @author Ondrej Pavelka <pavelka@cesnet.cz>
#
package Shongo::ClientCli::DomainService;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Shell;
use Shongo::ClientCli::API::Domain;

#
# Populate shell by options for management of resources.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'add-domain' => {
            desc => 'Add a foreign domain',
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    add_domain($attributes, $params->{'options'});
                }
            },
        },
        'modify-domain' => {
            desc => 'Modify an existing domain',
            args => '[id] [<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                my $attributes = Shongo::Shell::parse_attributes($params);
                if ( defined($attributes) ) {
                    modify_domain($args[0], $attributes, $params->{'options'});
                }
            },
        },
        'remove-domain' => {
            desc => 'Remove a foreign domain',
            args => '[id]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $id (split(/,/, $args[0])) {
                        remove_domain($id);
                    }
                } else {
                    remove_domain();
                }
            },
        },
        'add-domain-resource' => {
            desc => 'Assign resource to domain',
            args => '[<json_attributes>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                add_domain_resource(@args);
            },
        },
        'remove-domain-resource' => {
            desc => 'Remove resource from domain',
            options => 'domain=s resource=s',
            args => '[<domain-id>] [<resource-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                remove_domain_resource(@args);
            },
        },
        'get-password-hash' => {
            desc => 'Print local domain password hash',
            options => '',
            args => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                get_password_hash();
            },
        },
        'list-domain-resources' => {
            desc => 'List all allocatable foreign resources',
            options => 'domain=s',
            args => '[-domain=<domain-id>]',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_domain_resources($params->{'options'});
            },
        },
    });
}

sub add_domain()
{
    my ($attributes, $options) = @_;

    $options->{'on_confirm'} = sub {
        my ($domain) = @_;
        console_print_info("Adding domain...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.createDomain',
            $domain->to_xml()
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $domain = Shongo::ClientCli::API::Domain->new();

    my $id = $domain->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Domain '%s' successfully added.", $id);
    }
}

sub select_domain
{
    my ($id, $attributes) = @_;
    if ( defined($attributes) && defined($attributes->{'id'}) ) {
        $id = $attributes->{'id'};
    }
    $id = console_read_value('Identifier of the domain', 0, $Shongo::Common::IdPattern, $id);
    return $id;
}

sub modify_domain()
{
    my ($id, $attributes, $options) = @_;
    $id = select_domain($id, $attributes);
    if ( !defined($id) ) {
        return;
    }
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Resource.getDomain',
        RPC::XML::string->new($id)
    );

    $options->{'on_confirm'} = sub {
        my ($domain) = @_;
        console_print_info("Modifying domain...");
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.modifyDomain',
            $domain->to_xml()
        );
        if ( defined($response) ) {
            return $domain->{'id'};
        }
        return undef;
    };

    if ( defined($response) ) {
        my $domain = Shongo::ClientCli::API::Resource->from_hash($response);
        if ( defined($domain) ) {
            my $new_id = $domain->modify($attributes, $options);
            if ( defined($new_id) ) {
                console_print_info("Domain '%s' successfully modified to '%s'.", $id, $new_id);
            }
        }
    }
}

sub remove_domain()
{
    my ($id) = @_;
    $id = select_domain($id);
    if ( !defined($id) ) {
        return;
    }
    Shongo::ClientCli->instance()->secure_request(
        'Resource.deleteDomain',
        RPC::XML::string->new($id)
    );
}

sub add_domain_resource()
{
    my ($attributes, $options) = @_;
    my (@args) = @_;

    $options->{'on_confirm'} = sub {
        my ($domain_resource) = @_;

        console_print_info("Adding resource to domain...");
				my $domain = $domain_resource->{'domain'};
				my $resource = $domain_resource->{'resource'};
				delete $domain_resource->{'domain'};
        delete $domain_resource->{'resource'};
        my $response = Shongo::ClientCli->instance()->secure_request(
            'Resource.addDomainResource',
            $domain_resource->to_xml(),
            RPC::XML::string->new($domain),
            RPC::XML::string->new($resource),
        );
        if ( defined($response) ) {
            return $response;
        }
        return undef;
    };

    my $domain_resource = Shongo::ClientCli::API::Object->new();

    $domain_resource->set_object_class('DomainResource');
    $domain_resource->set_object_name('DomainResource');
    $domain_resource->add_attribute(
        'id', {
            'title' => 'Identifier',
            'editable' => 0
        }
    );
    $domain_resource->add_attribute(
        'domain', {
            'required' => 1,
            'title' => 'Domain ID',
        }
    );
    $domain_resource->add_attribute(
        'resource', {
            'required' => 1,
            'title' => 'Resource ID',
        }
    );
    $domain_resource->add_attribute(
        'licenseCount', {
            'required' => 1,
            'title' => 'License count',
        }
    );
    $domain_resource->add_attribute(
        'price', {
            'required' => 1,
            'title' => 'Price',
        }
    );
    $domain_resource->add_attribute(
        'priority', {
            'required' => 1,
            'title' => 'Priority',
        }
    );
    $domain_resource->add_attribute(
        'priority', {
            'required' => 1,
            'title' => 'Priority',
        }
    );
    $domain_resource->add_attribute(
        'type', {
            'required' => 0,
            'title' => 'Type',
        }
    );

    $domain_resource->create($attributes, $options);
}

sub remove_domain_resource()
{
    my (@args) = @_;
    if ( scalar(@args) < 2 ) {
        console_print_error("Arguments '<domain-id> <resource-id>' must be specified.");
        return;
    }
    my $domain_id = $args[0];
    my $resource_id = $args[1];
    Shongo::ClientCli->instance()->secure_request(
        'Resource.removeDomainResource',
        RPC::XML::string->new($domain_id),
        RPC::XML::string->new($resource_id),
    );
}

sub get_password_hash()
{
    my $password_hash = Shongo::ClientCli->instance()->secure_request('Resource.getLocalDomainPasswordHash');
    console_print_info("Domain's password hash '%s'", $password_hash);
}

sub list_domain_resources()
{
        my ($options) = @_;
        my $filter = {};
        if ( defined($options->{'domain'}) ) {
            $filter->{'domain'} = [$options->{'domain'}];
        }
        my $application = Shongo::ClientCli->instance();
        my $response = $application->secure_hash_request('Resource.listForeignResources', $filter);
        if ( !defined($response) ) {
            return
        }

        my $table = {
            'columns' => [
                {'field' => 'id',           'title' => 'Identifier'},
                {'field' => 'name',         'title' => 'Name'},
                {'field' => 'description',  'title' => 'Description'},
                {'field' => 'type',        'title' => 'Type'},
                {'field' => 'available',  'title' => 'Available'},
                {'field' => 'calendarPublic','title' => 'Calendar Public'},
            ],
            'data' => []
        };
        foreach my $resource (@{$response->{'items'}}) {
            push(@{$table->{'data'}}, {
                'id' => $resource->{'id'},
                'name' => $resource->{'name'},
                'description' => $resource->{'description'},
                'available' => $resource->{'available'} ? 'yes' : 'no',
                'calendarPublic' => $resource->{'calendarPublic'} ? 'yes' : 'no',
                'type' => $resource->{'type'},
            });
        }
        console_print_table($table);
}

1;