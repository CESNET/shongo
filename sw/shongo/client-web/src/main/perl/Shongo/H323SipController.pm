#
# Controller for H.323/SIP video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::H323SipController;
use base qw(Shongo::CommonController);

use strict;
use warnings;
use Shongo::Common;

sub new
{
    my $class = shift;
    my $self = Shongo::CommonController->new('h323-sip', @_);
    bless $self, $class;

    return $self;
}

sub index_action
{
    my ($self) = @_;
    $self->redirect('list');
}

sub list_action
{
    my ($self) = @_;
    $self->list_reservation_requests('List of existing H323/SIP reservation requests', ['H323', 'SIP']);
}

sub create_action
{
    my ($self) = @_;
    my $params = $self->get_params();
    if ( defined($self->get_param('confirmed')) ) {
        $params->{'error'} = $self->validate_form($params, {
            required => [
                'name',
                'purpose',
                'start',
                'durationCount',
                'periodicity',
                'participantCount',
            ],
            optional => [
                'periodicityEnd',
                'pin'
            ],
            constraint_methods => {
                'purpose' => qr/^SCIENCE|EDUCATION$/,
                'start' => 'datetime',
                'durationCount' => 'number',
                'periodicity' => qr/^none|daily|weekly$/,
                'periodicityEnd' => 'date',
                'participantCount' => 'number',
                'pin' => 'number'
            }
        });
        if ( !%{$params->{'error'}} ) {
            my $request = {};
            # Duration
            my $duration = undef;
            if ( $params->{'durationType'} eq 'minute' ) {
                $duration = 'PT' . $params->{'durationCount'} . 'M';
            }
            elsif ( $params->{'durationType'} eq 'hour' ) {
                $duration = 'PT' . $params->{'durationCount'} . 'H';
            }
            elsif ( $params->{'durationType'} eq 'day' ) {
                $duration = 'P' . $params->{'durationCount'} . 'D';
            }
            else {
                die("Unknown duration type '$params->{'durationType'}'.");
            }
            # Specification
            my $specification = {
                'class' => 'RoomSpecification',
                'participantCount' => $params->{'participantCount'},
                'withAlias' => 1,
                'technologies' => ['H323', 'SIP']
            };
            # Setup pin to specification
            if ( length($params->{'pin'}) > 0 ) {
                $specification->{'roomSettings'} = [{
                    'class' => 'RoomSetting.H323',
                    'pin' => $params->{'pin'}
                }];
            }
            # Setup request
            $request->{'name'} = $params->{'name'};
            $request->{'purpose'} = $params->{'purpose'};
            if ( $params->{'periodicity'} eq 'none') {
                $request->{'class'} = 'ReservationRequest';
                $request->{'slot'} = $params->{'start'} . '/' . $duration;
                $request->{'specification'} = $specification;
            }
            else {
                $request->{'class'} = 'ReservationRequestSet';
                $request->{'specifications'} = [$specification];
                my $start = {
                    'class' => 'PeriodicDateTime',
                    'start' => $params->{'start'}
                };
                if ( $params->{'periodicity'} eq 'daily' ) {
                    $start->{'period'} = 'P1D';
                }
                elsif ( $params->{'periodicity'} eq 'weekly' ) {
                    $start->{'period'} = 'P1W';
                }
                else {
                    die("Unknown periodicity '$params->{'periodicity'}'.");
                }
                if ( length($params->{'periodicityEnd'}) > 0 ) {
                    $start->{'end'} = $params->{'periodicityEnd'};
                }
                $request->{'slots'} = [{
                    'start' => $start,
                    'duration' => $duration
                }];
            }
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'jquery' => 1
    };
    $self->render_page('New reservation request', 'h323-sip/create.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->{'application'}->secure_request('Reservation.getReservationRequest', $id);
    $request->{'purpose'} = $Shongo::CommonController::ReservationRequestPurpose->{$request->{'purpose'}};

    my $specification = undef;
    my $child_requests = [];
    if ( $request->{'class'} eq 'ReservationRequest' ) {
        # Requested slot
        if ( $request->{'slot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = format_datetime($1);
            $request->{'duration'} = format_period($2);
        }

        # Requested specification
        $specification = $request->{'specification'};

        # Child requests
        push(@{$child_requests}, $request);
    }
    elsif ( $request->{'class'} eq 'ReservationRequestSet' ) {
        # Requested slot
        if ( scalar(@{$request->{'slots'}}) != 1 ) {
            $self->error("Reservation request should have exactly one requested slot.");
        }
        my $slot = $request->{'slots'}->[0];
        $request->{'duration'} = format_period($slot->{'duration'});
        if ( ref($slot->{'start'}) eq 'HASH' ) {
            $request->{'start'} = format_datetime($slot->{'start'}->{'start'});
            if ( $slot->{'start'}->{'period'} eq 'P1D' ) {
                $request->{'periodicity'} = 'daily';
            }
            elsif ( $slot->{'start'}->{'period'} eq 'P1W' ) {
                $request->{'periodicity'} = 'weekly';
            }
            else {
                $self->error("Unknown reservation request periodicity '$slot->{'start'}->{'period'}'.");
            }
            if ( defined($request->{'periodicity'}) ) {
                $request->{'periodicityEnd'} = format_datetime_partial($slot->{'start'}->{'end'});
            }
        }
        else {
            $request->{'start'} = format_datetime($slot->{'start'});
        }

        # Requested specification
        if ( scalar(@{$request->{'specifications'}}) != 1 ) {
            $self->error("Reservation request should have exactly one specification.");
        }
        $specification = $request->{'specifications'}->[0];

        # Child requests
        foreach my $child_request (@{$request->{'reservationRequests'}}) {
            # Requested slot
            if ( $child_request->{'slot'} =~ /(.*)\/(.*)/ ) {
                $child_request->{'start'} = format_datetime($1);
                $child_request->{'duration'} = format_period($2);
            }
            push(@{$child_requests}, $child_request);
        }
    }
    else {
        $self->error("Unknown reservation request type '$request->{'class'}'.");
    }
    if ( !defined($request->{'periodicity'}) ) {
        $request->{'periodicity'} = 'none';
    }

    # Requested specification
    if ( !defined($specification) ) {
        $self->error("Reservation request should have specification defined.");
    }
    if ( !($specification->{'class'} eq 'RoomSpecification') ) {
        $self->error("Reservation request should have room specification but '$specification->{'class'}' was present.");
    }
    if ( !$specification->{'withAlias'} ) {
        $self->error("Reservation request should request virtual room with aliases.");
    }
    $request->{'participantCount'} = $specification->{'participantCount'};

    if ( defined($specification->{'roomSettings'}) && @{$specification->{'roomSettings'}} > 0 ) {
        my $roomSetting = $specification->{'roomSettings'}->[0];
        if ( $roomSetting->{'class'} ne 'RoomSetting.H323' ) {
            $self->error("Reservation request should contains only room setttings for 'H323' but '$roomSetting->{'class'}' was present.");
        }
        $request->{'pin'} = $roomSetting->{'pin'};
    }
    if ( !defined($request->{'pin'}) || $request->{'pin'} eq '' ) {
        $request->{'pin'} = 'none';
    }


    # Allocated reservations
    $request->{'reservations'} = [];
    foreach my $child_request (@{$child_requests}) {
        # State report
        if ( !($child_request->{'state'} eq 'ALLOCATION_FAILED') ) {
           $child_request->{'stateReport'} = undef;
        }

        # Allocated reservation
        if ( $child_request->{'state'} eq 'ALLOCATED' ) {
            my $reservation = $self->{'application'}->secure_request('Reservation.getReservation',
                    $child_request->{'reservationIdentifier'});
            if ( !($reservation->{'class'} eq 'RoomReservation') ) {
                $self->error("Allocated reservation should be for room but '$reservation->{'class'}' was present.");
            }

            my $aliases = '';
            foreach my $alias (@{$reservation->{'executable'}->{'aliases'}}) {
                $aliases .= $alias->{'value'};
            }
            $child_request->{'aliases'} = $aliases;

        }

        # State
        my $state_class = lc($child_request->{'state'});
        $state_class =~ s/_/-/g;
        $child_request->{'stateClass'} = $state_class;
        $child_request->{'state'} = $Shongo::CommonController::ReservationRequestState->{$child_request->{'state'}};

        push(@{$request->{'reservations'}}, $child_request);
    }

    $self->render_page('Detail of existing H323/SIP reservation request', 'h323-sip/detail.html', {
        'request' => $request
    });
}

sub delete_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $confirmed = $self->get_param('confirmed');
    if ( defined($confirmed) ) {
        $self->{'application'}->secure_request('Reservation.deleteReservationRequest', $id);
        $self->redirect('list');
    }
    else {
        $self->render_page('Delete existing H323/SIP reservation request', 'h323-sip/delete.html', {
            'id' => $id
        });
    }
}

1;