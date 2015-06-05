#
# Domain
#
# @author Ondrej Pavelka <pavelka@cesnet.cz>
#
package Shongo::ClientCli::API::Domain;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

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
    return $self;
}

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    my $class = $attributes->{'class'};
    if ( !defined($class) ) {
        $class = console_read_enum('Select type of resource', ordered_hash(
            'Resource' => 'Other Resource',
            'DeviceResource' => 'Device Resource'
        ));
    }
    if ($class eq 'Resource') {
        return Shongo::ClientCli::API::Resource->new();
    } elsif ($class eq 'DeviceResource') {
        return Shongo::ClientCli::API::DeviceResource->new();
    }
    die("Unknown resource type '$class'.");
}

1;