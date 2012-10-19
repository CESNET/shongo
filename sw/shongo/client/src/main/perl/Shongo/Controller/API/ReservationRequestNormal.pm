#
# Abstract reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestNormal;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

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
    my $self = Shongo::Controller::API::ReservationRequestAbstract->new(@_);
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

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    $self->{'purpose'} = $attributes->{'purpose'};
    $self->SUPER::on_create($attributes);
}

# @Override
sub modify_attributes()
{
    my ($self, $edit) = @_;
    $self->SUPER::modify_attributes($edit);
    $self->{'purpose'} = console_auto_enum($edit, 'Select reservation purpose', $Purpose, $self->{'purpose'});
}

# @Override
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    $self->SUPER::on_modify_loop($actions);

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