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
# Create a new instance of domain
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Domain');
    $self->set_object_name('Domain');
    $self->add_attribute(
        'id', {
            'title' => 'Identifier',
            'editable' => 0
        }
    );
    $self->add_attribute(
        'name', {
            'required' => 1,
            'title' => 'Domain name',
        }
    );
    $self->add_attribute(
        'code', {
            'required' => 0,
            'title' => 'Domain code',
        }
    );
    $self->add_attribute(
        'organization', {
            'required' => 0,
            'title' => 'Domain organization',
        }
    );
    $self->add_attribute(
        'url', {
            'required' => 1,
            'title' => 'Url',
        }
    );
    $self->add_attribute(
        'port', {
            'required' => 1,
            'title' => 'Port',
        }
    );
    $self->add_attribute(
        'allocatable', {
            'required' => 1,
            'type' => 'bool',
            'title' => 'Use for local allocation',
        }
    );
    $self->add_attribute(
        'shareAuthorizationServer', {
            'required' => 0,
            'type' => 'bool',
            'title' => 'Uses the same AA server',
        }
    );
    $self->add_attribute(
        'certificatePath', {
            'required' => 0,
            'title' => 'Domain certificate file (for PKI auth)',
        }
    );
    $self->add_attribute(
        'passwordHash', {
            'required' => 0,
            'title' => 'Password hash (for basic auth)',
        }
    );
    return $self;
}

1;