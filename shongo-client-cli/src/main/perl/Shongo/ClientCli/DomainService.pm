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

    my $domain = Shongo::ClientCli::API::Object->new();

    $domain->set_object_class('Domain');
    $domain->set_object_name('Domain');
    $domain->add_attribute(
        'id', {
            'title' => 'Identifier',
            'editable' => 0
        }
    );
    $domain->add_attribute(
        'name', {
            'required' => 1,
            'title' => 'Domain name',
        }
    );
    $domain->add_attribute(
        'code', {
            'required' => 1,
            'title' => 'Domain code',
        }
    );
    $domain->add_attribute(
        'organization', {
            'required' => 1,
            'title' => 'Domain organization',
        }
    );
    $domain->add_attribute(
        'url', {
            'required' => 1,
            'title' => 'Url',
        }
    );
    $domain->add_attribute(
        'port', {
            'required' => 1,
            'title' => 'Port',
        }
    );
    $domain->add_attribute(
        'allocatable', {
            'required' => 1,
            'type' => 'bool',
            'title' => 'Use for local allocation',
        }
    );
    $domain->add_attribute(
        'certificatePath', {
            'required' => 0,
            'title' => 'Domain certificate file (for PKI auth)',
        }
    );
    $domain->add_attribute(
        'passwordHash', {
            'required' => 0,
            'title' => 'Password hash (for basic auth)',
        }
    );

    my $id = $domain->create($attributes, $options);
    if ( defined($id) ) {
        console_print_info("Domain '%s' successfully added.", $id);
    }
}

sub remove_domain()
{
    my ($id) = @_;
    $id = select_resource($id);
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

1;