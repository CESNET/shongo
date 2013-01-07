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
    my $aliasRequests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies,
        'specificationClass' => 'AliasSpecification'
    });
    foreach my $aliasRequest (@{$aliasRequests}) {
        my $state_code = lc($aliasRequest->{'state'});
        $state_code =~ s/_/-/g;
        $aliasRequest->{'purpose'} = $Shongo::ClientWeb::CommonController::ReservationRequestPurpose->{$aliasRequest->{'purpose'}};
        $aliasRequest->{'stateCode'} = $state_code;
        $aliasRequest->{'state'} = $Shongo::ClientWeb::CommonController::ReservationRequestState->{$aliasRequest->{'state'}};
        $aliasRequest->{'interval'} = format_interval_date($aliasRequest->{'earliestSlot'}, 1);
    }

    # Room requests
    my $roomRequests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => $technologies,
        'specificationClass' => 'RoomSpecification'
    });
    foreach my $roomRequest (@{$roomRequests}) {
        my $state_code = lc($roomRequest->{'state'});
        $state_code =~ s/_/-/g;
        $roomRequest->{'purpose'} = $Shongo::ClientWeb::CommonController::ReservationRequestPurpose->{$roomRequest->{'purpose'}};
        $roomRequest->{'stateCode'} = $state_code;
        $roomRequest->{'state'} = $Shongo::ClientWeb::CommonController::ReservationRequestState->{$roomRequest->{'state'}};
        if ( $roomRequest->{'earliestSlot'} =~ /(.*)\/(.*)/ ) {
            $roomRequest->{'start'} = format_datetime($1);
            $roomRequest->{'duration'} = format_period($2);
        }
    }
    $self->render_page('List of existing reservation requests', 'common/list.html', {
        'technologies' => join("/", map($Shongo::Common::Technology->{$_}, @{$technologies})),
        'roomRequests' => $roomRequests,
        'aliasRequests' => $aliasRequests
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
            $duration = 'P' . $params->{'durationC  ount'} . 'D';
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
    $request->{'name'} = $params->{'name'};
    $request->{'purpose'} = $params->{'purpose'};
    if ( !defined($params->{'periodicity'}) || $params->{'periodicity'} eq 'none') {
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

    my $specification = undef;
    my $child_requests = [];
    if ( $request->{'class'} eq 'ReservationRequest' ) {
        # Requested slot
        if ( $request->{'slot'} =~ /(.*)\/(.*)/ ) {
            $request->{'start'} = $1;
            $request->{'duration'} = $2;
            $request->{'end'} = get_interval_end($1, $2);
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
                #$self->error("Unknown reservation request periodicity '$slot->{'start'}->{'period'}'.");
            }
            if ( defined($request->{'periodicity'}) ) {
                $request->{'periodicityEnd'} = $slot->{'start'}->{'end'};
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
    if ( !defined($specification) ) {
        $self->error("Reservation request should have specification defined.");
    }
    if ( $specification->{'class'} eq 'RoomSpecification' ) {
        if ( !$specification->{'withAlias'} ) {
            $self->error("Reservation request should request virtual room with aliases.");
        }
        $request->{'participantCount'} = $specification->{'participantCount'};
    }
    elsif ( $specification->{'class'} eq 'AliasSpecification' ) {
        # TODO: fill some attributes
    }
    else {
        $self->error("Reservation request should have room specification but '$specification->{'class'}' was present.");
    }
    $request->{'specification'} = $specification;

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
        if ( defined($child_request->{'reservationId'}) ) {
            my $reservation = $self->{'application'}->secure_request('Reservation.getReservation', $child_request->{'reservationId'});
            if ( $specification->{'class'} eq 'RoomSpecification' ) {
                if ( !($reservation->{'class'} eq 'RoomReservation') ) {
                    $self->error("Allocated reservation should be room but '$reservation->{'class'}' was present.");
                }
                $self->format_aliases($child_request, $reservation->{'executable'}->{'aliases'}, $state_code eq 'started');
            }
            elsif ( $specification->{'class'} eq 'AliasSpecification' ) {
                $self->format_aliases($child_request, $reservation->{'aliases'}, $state_code eq 'started');
            }
        }

        push(@{$request->{'reservations'}}, $child_request);
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
    return $reservations;
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
    foreach my $alias (@{$aliases}) {
        if ( length($aliases_text) > 0 ) {
            $aliases_text .= ', ';
        }
        if ( $alias->{'type'} eq 'H323_E164' ) {
            $aliases_text .= $alias->{'value'};

            $aliases_description .= '<dt>H.323 GDS number:</dt><dd>(00420)' . $alias->{'value'} . '</dd>';
            $aliases_description .= '<dt>PSTN/phone:</dt><dd>+420' . $alias->{'value'} . '</dd>';
        }
        elsif ( $alias->{'type'} eq 'H323_IDENTIFIER' ) {
            $aliases_text .= $alias->{'value'};

            $aliases_description .= '<dt>H.323 Identifier:</dt><dd>' . $alias->{'value'} . '</dd>';
        }
        elsif ( $alias->{'type'} eq 'H323_URI' ) {
            $aliases_text .= $alias->{'value'};

            $aliases_description .= '<dt>H.323 IP:</dt><dd>' . $alias->{'value'} . '</dd>';
        }
        elsif ( $alias->{'type'} eq 'SIP_URI' ) {
            $aliases_text .= 'sip:' . $alias->{'value'};
            $aliases_description .= '<dt>SIP:</dt><dd>sip:' . $alias->{'value'} . '</dd>';
        }
        elsif ( $alias->{'type'} eq 'ADOBE_CONNECT_URI' ) {
            my $url = $alias->{'value'};
            if ( $available ) {
                $aliases_text .= "<a href=\"$url\">$url</a>";
            }
            else {
                $aliases_text .= "$url";
            }
        }
        elsif ( $alias->{'type'} eq 'ADOBE_CONNECT_NAME' ) {
            # skip
        }
        else {
            $self->error("Unknown alias type '$alias->{'type'}'.");
        }
    }
    if ( !$available ) {
        $aliases_text .= " <span class='muted'>(not available now)</span>";
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

1;