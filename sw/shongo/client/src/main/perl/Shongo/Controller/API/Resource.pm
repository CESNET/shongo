#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Resource;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Capability;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Resource');
    $self->set_object_name('Resource');
    $self->add_attribute(
        'identifier', {
            'editable' => 0
        }
    );
    $self->add_attribute(
        'name', {
            'required' => 1
        }
    );
    $self->add_attribute('description');
    $self->add_attribute(
        'parentIdentifier', {
            'title' => 'Parent',
            'string-pattern' => $Shongo::Common::IdentifierPattern
        }
    );
    $self->add_attribute(
        'allocatable', {
            'type' => 'bool'
        }
    );
    $self->add_attribute(
        'maximumFuture', {
            'title' => 'Maximum Future',
            'type' => 'period'
        }
    );
    $self->add_attribute(
        'childResourceIdentifiers', {
            'title' => 'Children',
            'format' => sub {
                my $string = '';
                foreach my $identifier (@{$self->{'childResourceIdentifiers'}}) {
                    if ( length($string) > 0 ) {
                        $string .= ', ';
                    }
                    $string .= $identifier;
                }
                return $string;
            },
            'read-only' => 1
        }
    );
    $self->add_attribute(
        'capabilities', {
            'type' => 'collection',
            'collection-title' => 'Capability',
            'collection-class' => 'Shongo::Controller::API::Capability',
            'required' => 1
        }
    );
    return $self;
}

# @Override
sub on_create_confirm
{
    my ($self) = @_;
    console_print_info("Creating resource...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Resource.createResource',
        $self->to_xml()
    );
    if ( !$response->is_fault() ) {
        return $response->value();
    }
    return undef;
}

# @Override
sub on_modify_confirm
{
    my ($self) = @_;
    console_print_info("Modifying resource...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Resource.modifyResource',
        $self->to_xml()
    );
    if ( $response->is_fault() ) {
        return 0;
    }
    return 1;
}

1;