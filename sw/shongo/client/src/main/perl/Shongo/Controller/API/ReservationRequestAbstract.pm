#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestAbstract;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

# Enumeration of reservation request type
our $Type = ordered_hash('NORMAL' => 'Normal', 'PERMANENT' => 'Permanent');

# Enumeration of reservation request purpose
our $Purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');

#
# Create a new instance of reservation request
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
# Get count of provided reservations
#
sub get_provided_reservations_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'providedReservationIdentifiers'});
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->on_create($attributes);

    while ( $self->modify_loop('creation of reservation request') ) {
        console_print_info("Creating reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.createReservationRequest',
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return $response->value();
        }
    }
    return undef;
}

#
# On create
#
sub on_create
{
    my ($self, $attributes) = @_;

    $self->{'type'} = $attributes->{'type'};
    $self->{'name'} = $attributes->{'name'};
    $self->{'purpose'} = $attributes->{'purpose'};
    $self->modify_attributes(0);
}

#
# Modify the reservation request
#
sub modify()
{
    my ($self) = @_;

    while ( $self->modify_loop('modification of reservation request') ) {
        console_print_info("Modifying reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.modifyReservationRequest',
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return;
        }
    }
}

#
# Run modify loop
#
sub modify_loop()
{
    my ($self, $message) = @_;
    console_action_loop(
        sub {
            printf("\n%s\n", $self->to_string());
        },
        sub {
            my @actions = (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                },
            );
            $self->on_modify_loop(\@actions);
            push(@actions, (
                'Confirm ' . $message => sub {
                    return 1;
                },
                'Cancel ' . $message => sub {
                    return 0;
                }
            ));
            return ordered_hash(@actions);
        }
    );
}

#
# On modify loop
#
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    push(@{$actions}, (
        'Modify provided reservations' => sub {
            $self->modify_provided_reservations();
            return undef;
        }
    ));
}

#
# @param $providedReservationIdentifier to be modified
#
sub modify_provided_reservation($)
{
    my ($providedReservationIdentifier) = @_;

    return console_edit_value("Reservation identifier", 1, $Shongo::Common::IdentifierPattern, $providedReservationIdentifier);
}

#
# Modify requested slots in the reservation request
#
sub modify_provided_reservations()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            console_print_text($self->get_provided_reservations());
        },
        sub {
            my @actions = (
                'Add new provided reservation' => sub {
                    my $providedReservationIdentifier = modify_provided_reservation('');
                    if ( defined($providedReservationIdentifier) ) {
                        add_collection_item(\$self->{'providedReservationIdentifiers'}, $providedReservationIdentifier);
                    }
                    return undef;
                },
            );
            if ( $self->get_provided_reservations_count() > 0 ) {
                push(@actions, 'Remove existing provided reservations' => sub {
                    my $index = console_read_choice("Type a number of provided reservation", 0, $self->get_provided_reservations_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'providedReservationIdentifiers'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying provided reservations' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# Modify attributes
#
sub modify_attributes()
{
    my ($self, $edit) = @_;

    $self->{'type'} = console_auto_enum($edit, 'Select reservation type', $Type, $self->{'type'});
    $self->{'name'} = console_auto_value($edit, 'Name of the reservation', 1, undef, $self->{'name'});
    $self->{'purpose'} = console_auto_enum($edit, 'Select reservation purpose', $Purpose, $self->{'purpose'});
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Reservation Request";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Identifier', $self->{'identifier'});
    $attributes->{'add'}('Created', format_datetime($self->{'created'}));
    $attributes->{'add'}('Type', $Type->{$self->{'type'}});
    $attributes->{'add'}('Name', $self->{'name'});
    $attributes->{'add'}('Purpose', $Purpose->{$self->{'purpose'}});
    $attributes->{'add_collection'}($self->get_provided_reservations());
}

#
# @return collection of provided reservations
#
sub get_provided_reservations()
{
    my ($self) = @_;
    my $collection = Shongo::Controller::API::Object::create_collection('Provided reservations');
    for ( my $index = 0; $index < $self->get_provided_reservations_count(); $index++ ) {
        my $providedReservationIdentifier = get_collection_item($self->{'providedReservationIdentifiers'}, $index);
        $collection->{'add'}(sprintf("identifier: %s", $providedReservationIdentifier));
    }
    return $collection;
}

1;