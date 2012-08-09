#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ResourceSpecification;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

#
# Resource specificaiton types
#
our $Type = ordered_hash(
    'ExternalEndpointSpecification' => 'External Endpoint',
    'ExistingResourceSpecification' => 'Existing Resource',
    'LookupResourceSpecification' => 'Lookup Resource'
);

#
# Create a new instance of resource specification
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# Create a new resource specification from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    my $specification = console_read_enum('Select type of resource specification', $Type);
    if ( defined($specification) ) {
        $self->{'class'} = $specification;
        $self->modify();
        return $self;

    }
    return $self;
}

#
# Modify the resource specification
#
sub modify()
{
    my ($self) = @_;

    switch ($self->{'class'}) {
        case 'ExternalEndpointSpecification' {
            $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::Resource::Technology, $self->{'technology'});
            $self->{'count'} = console_edit_value("Count", 1, "\\d+", $self->{'count'});
        }
        case 'ExistingResourceSpecification' {
            $self->{'resourceIdentifier'} = console_edit_value("Resource identifier", 1, $Shongo::Controller::API::Common::IdentifierPattern, $self->{'resourceIdentifier'});
            return $self;
        }
        case 'LookupResourceSpecification' {
            $self->{'technology'} = console_edit_enum("Select technology", $Shongo::Controller::API::Resource::Technology, $self->{'technology'});
        }
    }
}

# @Override
sub to_string()
{
    my ($self) = @_;

    my $string = $Type->{$self->{'class'}} . ' ';
    switch ($self->{'class'}) {
        case 'ExternalEndpointSpecification' {
            $string .= sprintf("technology: %s, count: %d\n",
                $Shongo::Controller::API::Resource::Technology->{$self->{'technology'}},
                $self->{'count'}
            );
        }
        case 'ExistingResourceSpecification' {
            $string .= sprintf("identifier: %s\n", $self->{'resourceIdentifier'});
        }
        case 'LookupResourceSpecification' {
            $string .= sprintf("technology: %s\n",
                $Shongo::Controller::API::Resource::Technology->{$self->{'technology'}}
            );
        }
        else {
            $string .= sprintf("unknown specification ");
        }
    }
    return $string;
}

1;