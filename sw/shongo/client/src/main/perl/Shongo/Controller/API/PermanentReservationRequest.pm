#
# Reservation request set
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::PermanentReservationRequest;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Specification;
use Shongo::Controller::API::ReservationRequest;

#
# Create a new instance of reservation request set
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::ReservationRequestAbstract->new(@_);
    bless $self, $class;

    $self->{'class'} = 'PermanentReservationRequest';
    $self->{'slots'} = [];
    $self->{'resourceReservations'} = [];

    $self->to_xml_skip_attribute('resourceReservations');

    return $self;
}

#
# Get count of requested slots in reservation request set
#
sub get_slots_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'slots'});
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    $self->SUPER::on_create($attributes);

    if ( $self->get_slots_count() == 0 ) {
        console_print_info("Fill requested slots:");
        $self->modify_slots();
    }
}

# @Override
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    push(@{$actions}, (
        'Modify requested slots' => sub {
            $self->modify_slots();
            return undef;
        }
    ));

    return $self->SUPER::on_modify_loop($actions);
}

# @Override
sub modify_attributes()
{
    my ($self, $edit) = @_;
    $self->SUPER::modify_attributes($edit);
    $self->{'resourceIdentifier'} = console_edit_value("Resource identifier", 1, $Shongo::Common::IdentifierPattern, $self->{'resourceIdentifier'});
}

#
# @param $slot to be modified
#
sub modify_slot($)
{
    my ($slot) = @_;

    if (ref($slot->{'start'}) && $slot->{'start'}->{'class'} eq 'PeriodicDateTime') {
        $slot->{'start'}->{'start'} = console_edit_value("Type a starting date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'}->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
        $slot->{'start'}->{'period'} = console_edit_value("Type a period", 0, $Shongo::Common::PeriodPattern, $slot->{'start'}->{'period'});
        $slot->{'start'}->{'end'} = console_edit_value("Ending date/time", 0, $Shongo::Common::DateTimePartialPattern, $slot->{'start'}->{'end'});
    } else {
        $slot->{'start'} = console_edit_value("Type a date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
    }
}

#
# Modify requested slots in the reservation request
#
sub modify_slots()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            console_print_text($self->get_slots());
        },
        sub {
            my @actions = (
                'Add new requested slot by absolute date/time' => sub {
                    my $slot = {};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $slot = {'start' => {'class' => 'PeriodicDateTime'}};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                }
            );
            if ( $self->get_slots_count() > 0 ) {
                push(@actions, 'Modify existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        modify_slot(get_collection_item($self->{'slots'}, $index - 1));
                    }
                    return undef;
                });
                push(@actions, 'Remove existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'slots'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying requested slots' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# @return report
#
sub get_report
{
    my ($self) = @_;
    my $color = 'blue';
    my $report = $self->{'report'};
    $report = format_report($report, get_term_width() - 23);
    return colored($report, $color);
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'specifications' ) {
        return Shongo::Controller::API::Specification->new($class);
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Permanent Reservation Request";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Resource Identifier', $self->{'resourceIdentifier'});
    $attributes->{'add'}('Report', $self->get_report());
    $attributes->{'add_collection'}($self->get_slots());

    my $collection = $attributes->{'add_collection'}('Created resource reservations');
    my $reservation_count = get_collection_size($self->{'resourceReservations'});
    if ( $reservation_count > 0 ) {
        for ( my $index = 0; $index < $reservation_count; $index++ ) {
            my $reservation = Shongo::Controller::API::Reservation->new();
            $reservation->from_xml(get_collection_item($self->{'resourceReservations'}, $index));
            $collection->{'add'}($reservation);
        }
    }
}

#
# @return collection of slots
#
sub get_slots()
{
    my ($self) = @_;
    my $collection = Shongo::Controller::API::Object::create_collection('Requested slots');
    for ( my $index = 0; $index < $self->get_slots_count(); $index++ ) {
        my $slot = get_collection_item($self->{'slots'}, $index);
        my $start = $slot->{'start'};
        my $duration = $slot->{'duration'};
        if ( ref($start) ) {
            my $startString = sprintf("(%s, %s", format_datetime($start->{'start'}), $start->{'period'});
            if ( defined($start->{'end'}) ) {
                $startString .= ", " . format_datetime_partial($start->{'end'});
            }
            $startString .= ")";
            $start = $startString;
        } else {
            $start = format_datetime($start);
        }
        $collection->{'add'}(sprintf("at '%s' for '%s'", $start, $duration));
    }
    return $collection;
}

1;