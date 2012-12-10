#
# Common controller for all video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb::CommonController;
use base qw(Shongo::Web::Controller);

use strict;
use warnings;
use Shongo::Common;

our $ReservationRequestPurpose = {
    'SCIENCE' => 'Science',
    'EDUCATION' => 'Education'
};

our $ReservationRequestState = {
    'NOT_ALLOCATED' => 'not allocated',
    'NOT_COMPLETE' => 'not allocated',
    'COMPLETE' => 'not allocated',
    'ALLOCATED' => 'allocated',
    'STARTED' => 'started',
    'STARTING_FAILED' => 'starting failed',
    'FINISHED' => 'finished',
    'ALLOCATION_FAILED' => 'allocation failed'
};

sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new(@_);
    bless $self, $class;

    return $self;
}

# @Override
sub pre_dispatch
{
    my ($self, $action) = @_;
    my $application = Shongo::ClientWeb->instance();
    if ( !defined($application->get_user()) ) {
        $application->redirect('/sign-in');
        return 0;
    }
    return $self->SUPER::pre_dispatch($action);
}

sub index_action
{
    my ($self) = @_;
    $self->redirect('list');
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
        $self->render_page('Delete existing reservation request', 'common/delete.html', {
            'id' => $id
        });
    }
}

sub list_reservation_requests
{
    my ($self, $title, $technologies) = @_;
    my $requests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies
    });
    foreach my $request (@{$requests}) {
        my $state_code = lc($request->{'state'});
        $state_code =~ s/_/-/g;
        $request->{'purpose'} = $ReservationRequestPurpose->{$request->{'purpose'}};
        $request->{'stateCode'} = $state_code;
        $request->{'state'} = $ReservationRequestState->{$request->{'state'}};
        if ( $request->{'earliestSlot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = format_datetime($1);
            $request->{'duration'} = format_period($2);
        }
    }
    $self->render_page($title, 'common/list.html', {
        'requests' => $requests
    });
}

#
# @param $params
# @param $technologies
# @return parsed specification
#
sub parse_specification
{
    my ($self, $params, $technologies) = @_;
    # Specification
    my $specification = {
        'class' => 'RoomSpecification',
        'participantCount' => $params->{'participantCount'},
        'withAlias' => 1,
        'technologies' => $technologies
    };
    return $specification;
}

#
# @param $params
# @param $specification
# @return parsed reservation request
#
sub parse_reservation_request
{
    my ($self, $params, $specification) = @_;
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
    return $request;
}

#
# @param $id
# @return detail of reservation request with given $id
#
sub get_reservation_request
{
    my ($self, $id) = @_;

    my $request = $self->{'application'}->secure_request('Reservation.getReservationRequest', $id);
    $request->{'purpose'} = $Shongo::ClientWeb::CommonController::ReservationRequestPurpose->{$request->{'purpose'}};

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
                #$self->error("Unknown reservation request periodicity '$slot->{'start'}->{'period'}'.");
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
    $request->{'specification'} = $specification;
    $request->{'participantCount'} = $specification->{'participantCount'};

    # Allocated reservations
    $request->{'reservations'} = [];
    foreach my $child_request (@{$child_requests}) {
        # State report
        if ( !($child_request->{'state'} eq 'ALLOCATION_FAILED') ) {
           $child_request->{'stateReport'} = undef;
        }

        # State
        my $state_code = lc($child_request->{'state'});
        $state_code =~ s/_/-/g;
        $child_request->{'stateCode'} = $state_code;
        $child_request->{'state'} = $Shongo::ClientWeb::CommonController::ReservationRequestState->{$child_request->{'state'}};

        # Allocated reservation
        if ( defined($child_request->{'reservationIdentifier'}) ) {
            my $reservation = $self->{'application'}->secure_request('Reservation.getReservation',
                    $child_request->{'reservationIdentifier'});
            if ( !($reservation->{'class'} eq 'RoomReservation') ) {
                $self->error("Allocated reservation should be for room but '$reservation->{'class'}' was present.");
            }

            my $aliases = '';
            my $aliases_description = '';
            foreach my $alias (@{$reservation->{'executable'}->{'aliases'}}) {
                if ( length($aliases) > 0 ) {
                    $aliases .= ', ';
                }
                if ( $alias->{'type'} eq 'H323_E164' ) {
                    $aliases .= $alias->{'value'};
                    $aliases_description .= '<dt>H.323 GDS number:</dt><dd>(00420)' . $alias->{'value'} . '</dd>';
                    $aliases_description .= '<dt>PSTN/phone:</dt><dd>+420' . $alias->{'value'} . '</dd>';
                }
                elsif ( $alias->{'type'} eq 'SIP_URI' ) {
                    $aliases .= 'sip:' . $alias->{'value'};
                    $aliases_description .= '<dt>SIP:</dt><dd>sip:' . $alias->{'value'} . '</dd>';
                }
                elsif ( $alias->{'type'} eq 'ADOBE_CONNECT_URI' ) {
                    my $url = $alias->{'value'};
                    if ( $state_code eq 'started' ) {
                        $aliases .= "<a href=\"$url\">$url</a>";
                    }
                    else {
                        $aliases .= "$url <span class='muted'>(not available now)</span>";
                    }
                }
                elsif ( $alias->{'type'} eq 'ADOBE_CONNECT_NAME' ) {
                    # skip
                }
                else {
                    $self->error("Unknown alias type '$alias->{'type'}'.");
                }
            }
            if ( length($aliases_description) == 0 ){
                $aliases_description = undef;
            }
            else {
                $aliases_description = '<div class="popup"><dl class="dl-horizontal">' . $aliases_description . '</dl></div>';
            }
            $child_request->{'aliases'} = $aliases;
            $child_request->{'aliasesDescription'} = $aliases_description;
        }

        push(@{$request->{'reservations'}}, $child_request);
    }
    return $request;
}

1;