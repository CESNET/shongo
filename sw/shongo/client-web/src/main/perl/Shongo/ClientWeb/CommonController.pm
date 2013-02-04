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
use DateTime::Format::Duration;

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

sub list_reservation_requests
{
    my ($self, $technologies) = @_;

    # Alias requests
    my $alias_requests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies,
        'specificationClass' => ['AliasSpecification', 'AliasGroupSpecification']
    });
    foreach my $request_alias (@{$alias_requests}) {
        $self->process_reservation_request_summary($request_alias);
    }

    # Room requests
    my $room_requests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies,
        'specificationClass' => 'RoomSpecification'
    });
    foreach my $request_room (@{$room_requests}) {
        $self->process_reservation_request_summary($request_room);
    }
    $self->render_page('List of existing reservation requests', 'common/list.html', {
        'technologies' => join("/", map($Shongo::Common::Technology->{$_}, @{$technologies})),
        'roomRequests' => $room_requests,
        'aliasRequests' => $alias_requests
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
        $self->render_page('Delete existing reservation request', 'common/delete.html', {
            'id' => $id
        });
    }
}

#
# @param $params
# @param $technologies
# @return parsed specification
#
sub parse_room_specification
{
    my ($self, $params, $technologies) = @_;
    # Specification
    my $specification = {
        'class' => 'RoomSpecification',
        'participantCount' => $params->{'participantCount'},
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
    my $duration = undef;

    # Parse duration from type and count
    if ( defined($params->{'durationType'}) && defined($params->{'durationCount'}) ) {
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
    }
    # Parse duration from start and end
    if ( defined($params->{'start'}) && defined($params->{'end'}) ) {
        my $start = $params->{'start'};
        my $end = $params->{'end'};
        if ( $start =~ /^[^T]+$/ ) {
            $start .= 'T00:00:00';
        }
        if ( $end =~ /^[^T]+$/ ) {
            $end .= 'T23:59:59';
        }
        $start = DateTime::Format::ISO8601->parse_datetime($start);
        $end = DateTime::Format::ISO8601->parse_datetime($end);
        my $format = DateTime::Format::Duration->new(pattern => 'P%YY%mM%eDT%HH%MM%SS');
        $duration = $format->format_duration($end->subtract_datetime($start));
    }
    # If no duration was specified die
    if ( !defined($duration) ) {
        die("Unknown duration.");
    }

    # Setup request
    $request->{'description'} = $params->{'description'};
    $request->{'purpose'} = $params->{'purpose'};
    $request->{'specification'} = $specification;
    if ( !defined($params->{'periodicity'}) || $params->{'periodicity'} eq 'none') {
        $request->{'class'} = 'ReservationRequest';
        $request->{'slot'} = $params->{'start'} . '/' . $duration;
    }
    else {
        $request->{'class'} = 'ReservationRequestSet';
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

    # Setup provided alias reservation
    if ( defined($params->{'alias'}) && $params->{'alias'} ne 'none' ) {
        $request->{'providedReservationIds'} = [$params->{'alias'}];
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

    my $child_requests = [];
    if ( $request->{'class'} eq 'ReservationRequest' ) {
        # Requested slot
        if ( $request->{'slot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = $1;
            $request->{'duration'} = $2;
            $request->{'end'} = get_interval_end($1, $2);
        }

        # Child requests
        push(@{$child_requests}, $request);
    }
    elsif ( $request->{'class'} eq 'ReservationRequestSet' ) {
        # Requested slot
        if ( scalar(@{$request->{'slots'}}) != 1 ) {
            $self->error("Reservation request should have exactly one requested slot.");
        }
        my $slot = $request->{'slots'}->[0];
        $request->{'duration'} = $slot->{'duration'};
        if ( ref($slot->{'start'}) eq 'HASH' ) {
            $request->{'start'} = $slot->{'start'}->{'start'};
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
                $request->{'periodicityEnd'} = $slot->{'start'}->{'end'};
            }
        }
        else {
            $request->{'start'} = format_datetime($slot->{'start'});
        }

        # Child requests
        foreach my $child_request (@{$request->{'reservationRequests'}}) {
            # Requested slot
            if ( $child_request->{'slot'} =~ /(.*)\/(.*)/ ) {
                $child_request->{'start'} = $1;
                $child_request->{'duration'} = $2;
                $child_request->{'end'} = get_interval_end($1, $2);
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
    my $specification = $request->{'specification'};
    if ( !defined($specification) ) {
        $self->error("Reservation request should have specification defined.");
    }
    if ( $specification->{'class'} eq 'RoomSpecification' ) {
        $request->{'participantCount'} = $specification->{'participantCount'};
    }
    elsif ( $specification->{'class'} eq 'AliasSpecification' ) {
    }
    elsif ( $specification->{'class'} eq 'AliasGroupSpecification' ) {
    }
    else {
        $self->error("Reservation request has unknown specification '$specification->{'class'}'.");
    }
    $request->{'specification'} = $specification;

    # Provided reservations
    $request->{'providedReservations'} = [];
    foreach my $provided_reservation_id (@{$request->{'providedReservationIds'}}) {
        my $provided_reservation = $self->{'application'}->secure_request('Reservation.getReservation', $provided_reservation_id);
        $self->process_reservation($provided_reservation);
        push(@{$request->{'providedReservations'}}, $provided_reservation);
    }

    # Allocated reservations
    $request->{'childRequests'} = [];
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
        if ( defined($child_request->{'reservationId'}) ) {
            my $reservation = $self->{'application'}->secure_request('Reservation.getReservation', $child_request->{'reservationId'});
            $self->process_reservation($reservation, $state_code eq 'started');

            if ( $specification->{'class'} eq 'AliasSpecification' || $specification->{'class'} eq 'AliasGroupSpecification') {
                my $aliasUsageRequests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
                    'providedReservationId' => $request->{'reservationId'}
                });
                foreach my $aliasUsageRequest (@{$aliasUsageRequests}) {
                    $self->process_reservation_request_summary($aliasUsageRequest);
                }
                $child_request->{'aliasUsageRequests'} = $aliasUsageRequests;
            }
            $child_request->{'reservation'} = $reservation;
        }

        push(@{$request->{'childRequests'}}, $child_request);
    }
    return $request;
}

#
# @param $class
# @param $technology
# @return list of reservations
#
sub get_reservations
{
    my ($self, $class, $technology) = @_;
    my $filter = {};
    $filter->{'reservationClass'} = $class;
    if ( defined($technology) ) {
        $filter->{'technology'} = $technology;
    }
    my $reservations = $self->{'application'}->secure_request('Reservation.listReservations', $filter);
    foreach my $reservation (@{$reservations}) {
        $self->process_reservation($reservation);
    }
    return $reservations;
}

#
# @param $request_summary to be processed
#
sub process_reservation_request_summary
{
    my ($self, $request_summary) = @_;
    my $state_code = lc($request_summary->{'state'});
    $state_code =~ s/_/-/g;
    $request_summary->{'purpose'} = $Shongo::ClientWeb::CommonController::ReservationRequestPurpose->{$request_summary->{'purpose'}};
    $request_summary->{'stateCode'} = $state_code;
    $request_summary->{'state'} = $Shongo::ClientWeb::CommonController::ReservationRequestState->{$request_summary->{'state'}};
    if ( $request_summary->{'earliestSlot'} =~ /(.*)\/(.*)/ ) {
        $request_summary->{'start'} = $1;
        $request_summary->{'duration'} = $2;
    }
}

#
# @param $reservation to be processed
#
sub process_reservation
{
    my ($self, $reservation, $available) = @_;
    if ( $reservation->{'class'} eq 'AliasReservation' ) {
        $self->format_aliases($reservation, $reservation->{'aliases'}, $available);
    }
    elsif ( $reservation->{'class'} eq 'RoomReservation' ) {
        $self->format_aliases($reservation, $reservation->{'executable'}->{'aliases'}, $available);
    }
    elsif ( $reservation->{'class'} eq 'ExistingReservation' ) {
        my $reusedReservation = $reservation->{'reservation'};
        $self->process_reservation($reusedReservation, $available);
        delete $reservation->{'reservation'};
        for my $key (keys %{$reusedReservation}) {
            $reservation->{$key} = $reusedReservation->{$key};
        }
    }
    else {
        my $aliases = [];
        foreach my $child_reservation_id (@{$reservation->{'childReservationIds'}}) {
            my $child_reservation = $self->{'application'}->secure_request('Reservation.getReservation', $child_reservation_id);
            if ( $child_reservation->{'class'} eq 'AliasReservation' ) {
                push(@{$aliases}, @{$child_reservation->{'aliases'}});
            }
        }

        if ( scalar(@{$aliases}) > 0 ) {
            $self->format_aliases($reservation, $aliases, $available);
        }
    }
}

#
# @param $reference  reference to hash where the 'aliases' and 'aliasesDescription' keys should be placed
# @param $aliases    collection of aliases
# @param $available  specifies whether aliases are available now
#
sub format_aliases
{
    my ($self, $reference, $aliases, $available) = @_;
    my $aliases_text = '';
    my $aliases_description = '';
    my $previous_type = '';
    my @sorted_aliases = sort { $a->{'type'} cmp $b->{'type'} } @{$aliases};
    foreach my $alias (@sorted_aliases) {
        if ( $alias->{'type'} eq 'ROOM_NAME' ) {
            $reference->{'roomName'} = $alias->{'value'};
            next;
        }
        if ( length($aliases_text) > 0 ) {
            $aliases_text .= $self->format_selectable(',&nbsp;');
        }

        my $aliasValue = $self->format_selectable($alias->{'value'}, 'nowrap');
        if ( $alias->{'type'} eq 'H323_E164' ) {
            $aliases_text .= $aliasValue;

            $aliases_description .= '<dt>PSTN/phone:</dt><dd>' . $self->format_selectable('+420' . $alias->{'value'}) . '&nbsp;</dd>';
            $aliases_description .= '<dt>H.323 GDS number:</dt><dd>' . $self->format_selectable('(00420)' . $alias->{'value'}) . '&nbsp;</dd>';
        }
        elsif ( $alias->{'type'} eq 'H323_URI' ) {
            $aliases_text .= $aliasValue;

            $aliases_description .= '<dt>H.323 URI:</dt><dd>' . $aliasValue . '&nbsp;</dd>';
        }
        elsif ( $alias->{'type'} eq 'H323_IP' ) {
            $aliases_text .= $aliasValue;

            $aliases_description .= '<dt>H.323 IP:</dt><dd>' . $aliasValue . '&nbsp;</dd>';
        }
        elsif ( $alias->{'type'} eq 'SIP_URI' ) {
            $aliasValue = $self->format_selectable('sip:' . $alias->{'value'});
            $aliases_text .= $aliasValue;
            $aliases_description .= '<dt>SIP URI:</dt>';
            $aliases_description .= '<dd>' . $aliasValue . '&nbsp;</dd>';
        }
        elsif ( $alias->{'type'} eq 'SIP_IP' ) {
            $aliases_text .= $aliasValue;

            $aliases_description .= '<dt>SIP IP:</dt><dd>' . $aliasValue . '&nbsp;</dd>';
        }
        elsif ( $alias->{'type'} eq 'ADOBE_CONNECT_URI' ) {
            if ( $available ) {
                $aliases_text .= '<a class="nowrap" href="' . $alias->{'value'} . '">' . $alias->{'value'} . '</a>';
            }
            else {
                $aliases_text .= $aliasValue;
            }
            $aliases_description .= '<dt>Room URL:</dt><dd>' . $self->format_selectable($alias->{'value'}) . '&nbsp;</dd>';
        }
        else {
            $self->error("Unknown alias type '$alias->{'type'}'.");
        }
        $previous_type = $alias->{'type'};
    }
    if ( !$available ) {
        $aliases_text .= "<span class='muted nowrap' style='float:left'>&nbsp;(not available now)</span>";
    }
    if ( length($aliases_description) == 0 ){
        $aliases_description = undef;
    }
    else {
        $aliases_description = '<div class="popup"><dl class="dl-horizontal">' . $aliases_description . '</dl></div>';
    }
    $reference->{'aliases'} = $aliases_text;
    $reference->{'aliasesDescription'} = $aliases_description;
}

sub format_selectable
{
    my ($self, $text, $class) = @_;
    return '<span style="float:left" class="' . $class . '"">' . $text . '</span>';
}

1;