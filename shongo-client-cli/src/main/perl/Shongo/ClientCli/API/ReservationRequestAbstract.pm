#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ReservationRequestAbstract;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::ReservationRequest;
use Shongo::ClientCli::API::ReservationRequestSet;
use Shongo::ClientCli::API::AuxiliaryData;

# Enumeration of reservation request purpose
our $Purpose = ordered_hash(
    'USER' => 'User',
    'SCIENCE' => 'Science',
    'EDUCATION' => 'Education',
    'OWNER' => 'Owner',
    'MAINTENANCE' => 'Maintenance'
);

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('AbstractReservationRequest');
    $self->set_object_name('Reservation Request');
    $self->add_attribute('id', {
        'title' => 'Identifier',
        'editable' => 0
    });
    $self->add_attribute('userId', {
        'title' => 'Owner',
        'format' => sub { return Shongo::ClientCli->instance()->format_user(@_, 1); },
        'editable' => 0
    });
    $self->add_attribute('created', {
        'type' => 'datetime',
        'editable' => 0
    });
    $self->add_attribute('purpose', {
        'type' => 'enum',
        'enum' => $Purpose,
        'required' => 1
    });
    $self->add_attribute('priority', {
        'title' => 'Priority (default = 0)',
        'type' => 'int'
    });
    $self->add_attribute('description');
    $self->add_attribute('specification', {
        'complex' => 1,
        'modify' => sub {
            my ($specification) = @_;
            my $class = undef;
            if ( defined($specification) ) {
                $class = $specification->{'class'};
            }
            $class = Shongo::ClientCli::API::Specification::select_type($class);
            if ( !defined($specification) || !($class eq $specification->get_object_class()) ) {
                $specification = Shongo::ClientCli::API::Specification->create({'class' => $class});
            } else {
                $specification->modify();
            }
            return $specification;
        },
        'required' => 1
    });
    $self->add_attribute('reusedReservationRequestId', {
        'title' => 'Reused reservation request'
    });
    $self->add_attribute('reusement', {
        'type' => 'enum',
        'enum' => ordered_hash(
            'NONE' => 'None',
            'ARBITRARY' => 'Arbitrary',
            'OWNED' => 'Owned'
        )
    });
    $self->add_attribute('auxData', {
        'type' => 'collection',
        'item' => {
            'title' => 'Auxiliary Data',
            'class' => 'Shongo::ClientCli::API::AuxiliaryData',
            'short' => 1,
        },
        'optional' => 1,
    });

    return $self;
}

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    my $class = $attributes->{'class'};
    if ( !defined($class) ) {
        $class = console_read_enum('Select type of reservation request', ordered_hash(
            'ReservationRequest' => 'Single Reservation Request',
            'ReservationRequestSet' => 'Set of Reservation Requests'
        ));
    }
    if ($class eq 'ReservationRequest') {
        return Shongo::ClientCli::API::ReservationRequest->new();
    }
    elsif ($class eq 'ReservationRequestSet') {
        return Shongo::ClientCli::API::ReservationRequestSet->new();
    }
    die("Unknown reservation type type '$class'.");
}

# @Override
sub on_modify_confirm
{
    my ($self) = @_;

    return 1;
}

1;