#
# Reservation request set
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestSet;
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
    my $self = Shongo::Controller::API::ReservationRequestNormal->new(@_);
    bless $self, $class;

    $self->set_object_class('ReservationRequestSet');
    $self->set_object_name('Set of Reservation Requests');

    $self->add_attribute('slots', {
        'type' => 'collection',
        'collection-title' => 'Requested Slot',
        'collection-add' => {
            'Add new requested slot by absolute date/time' => sub {
                my $slot = {};
                modify_slot($slot);
                return $slot;
            },
            'Add new requested slot by periodic date/time' => sub {
                my $slot = {'start' => {'class' => 'PeriodicDateTime'}};
                modify_slot($slot);
                return $slot;
            }
        },
        'collection-modify' => sub {
            my ($slot) = @_;
            modify_slot($slot);
            return $slot;
        },
        'format' => sub {
            my ($slot) = @_;
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
            return sprintf("at '%s' for '%s'", $start, $duration);
        },
        'display' => 'newline'
    });
    $self->add_attribute('specifications', {
        'type' => 'collection',
        'collection-title' => 'specification',
        'collection-class' => 'Shongo::Controller::API::Specification',
        'display' => 'newline'
    });
    $self->add_attribute('reservationRequests', {
        'type' => 'collection',
        'title' => 'Reservation Requests',
        'format' => sub() {
            my ($reservation_request) = @_;
            my $item = sprintf("%s (%s) %s\n" . colored("specification", $Shongo::Controller::API::Object::COLOR) . ": %s",
                format_interval($reservation_request->{'slot'}),
                $reservation_request->{'identifier'},
                $reservation_request->get_state(),
                $Shongo::Controller::API::Specification::Type->{$reservation_request->{'specification'}->{'class'}}
            );
            if ( $reservation_request->{'state'} eq 'ALLOCATED' ) {
                $item .= sprintf("\n  " . colored("reservation", $Shongo::Controller::API::Object::COLOR) . ": %s", $reservation_request->{'reservationIdentifier'});
            }
        },
        'display' => 'newline',
        'read-only' => 1
    });

    return $self;
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

1;