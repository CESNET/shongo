#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Specification;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::CompartmentSpecification;
use Shongo::Controller::API::DeviceResource;

#
# Specification types
#
our $Type = ordered_hash(
    'CompartmentSpecification' => 'Compartment',
    'ExternalEndpointSpecification' => 'External Endpoint',
    'ExistingEndpointSpecification' => 'Existing Resource',
    'LookupEndpointSpecification' => 'Lookup Resource',
    'PersonSpecification' => 'Person'
);

#
# Call initiation
#
our $CallInitiation = ordered_hash(
    'DEFAULT' => 'Default',
    'TERMINAL' => 'Terminal',
    'VIRTUAL_ROOM' => 'Virtual Room'
);

#
# Create a new instance of resource specification
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    if ( defined($type) && $type eq 'CompartmentSpecification' ) {
        return Shongo::Controller::API::CompartmentSpecification->new();
    }
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

sub select_type($)
{
    my ($type) = @_;

    return console_edit_enum('Select type of specification', $Type, $type);
}

#
# Create a new specification from this instance
#
sub create()
{
    my ($class, $type) = @_;

    my $specification = $type;
    if ( !defined($specification) ) {
        $specification = $class->select_type();
    }
    my $self = undef;
    if ( defined($specification) ) {
        if ($specification eq 'CompartmentSpecification') {
            $self = Shongo::Controller::API::CompartmentSpecification->new();
        } else {
            $self = Shongo::Controller::API::Specification->new();
            $self->{'class'} = $specification;
        }
        $self->modify();
        return $self;

    }
    return $self;
}

#
# Modify the specification
#
sub modify()
{
    my ($self) = @_;

    switch ($self->{'class'}) {
        case 'ExternalEndpointSpecification' {
            $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::DeviceResource::Technology, $self->{'technology'});
            $self->{'count'} = console_edit_value("Count", 1, "\\d+", $self->{'count'});
        }
        case 'ExistingEndpointSpecification' {
            $self->{'resourceIdentifier'} = console_edit_value("Resource identifier", 1, $Shongo::Controller::API::Common::IdentifierPattern, $self->{'resourceIdentifier'});
            return $self;
        }
        case 'LookupEndpointSpecification' {
            $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::DeviceResource::Technology, $self->{'technology'});
        }
        case 'PersonSpecification' {
            printf("TODO: Implement\n");
        }
    }
}

# @Override
sub get_name
{
    my ($self) = @_;
    if ( defined($self->{'class'}) && exists $Type->{$self->{'class'}} ) {
        return $Type->{$self->{'class'}};
    } else {
        return "Specification";
    }
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);

    switch ($self->{'class'}) {
        case 'ExternalEndpointSpecification' {
            $attributes->{'add'}('Technology', $Shongo::Controller::API::DeviceResource::Technology->{$self->{'technology'}});
            $attributes->{'add'}('Count', $self->{'count'});
        }
        case 'ExistingEndpointSpecification' {
            $attributes->{'add'}('Resource Identifier', $self->{'resourceIdentifier'});
        }
        case 'LookupEndpointSpecification' {
            $attributes->{'add'}('Technology', $Shongo::Controller::API::DeviceResource::Technology->{$self->{'technology'}});
        }
    }
}

1;