#
# Controller for H.323/SIP video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb::H323SipController;
use base qw(Shongo::ClientWeb::CommonController);

use strict;
use warnings;
use Shongo::Common;

sub new
{
    my $class = shift;
    my $self = Shongo::ClientWeb::CommonController->new('h323-sip', @_);
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

    # Alias requests
    my $aliasRequests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => ['H323', 'SIP'],
        'specification' => 'AliasSpecification'
    });
    foreach my $aliasRequest (@{$aliasRequests}) {
        my $state_code = lc($aliasRequest->{'state'});
        $state_code =~ s/_/-/g;
        $aliasRequest->{'purpose'} = $Shongo::ClientWeb::CommonController::ReservationRequestPurpose->{$aliasRequest->{'purpose'}};
        $aliasRequest->{'stateCode'} = $state_code;
        $aliasRequest->{'state'} = $Shongo::ClientWeb::CommonController::ReservationRequestState->{$aliasRequest->{'state'}};
        $aliasRequest->{'interval'} = format_interval($aliasRequest->{'earliestSlot'}, 1);
    }

    # Room requests
    my $roomRequests = $self->{'application'}->secure_request('Reservation.listReservationRequests', {
        'technology' => ['H323', 'SIP'],
        'specification' => 'RoomSpecification'
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
    $self->render_page('List of existing H323/SIP reservation requests', 'h323-sip/list.html', {
        'roomRequests' => $roomRequests,
        'aliasRequests' => $aliasRequests
    });
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
            my $specification = $self->parse_room_specification($params, ['H323', 'SIP']);

            # Add PIN
            if ( length($params->{'pin'}) > 0 ) {
                $specification->{'roomSettings'} = [{
                    'class' => 'RoomSetting.H323',
                    'pin' => $params->{'pin'}
                }];
            }

            my $reservation_request = $self->parse_reservation_request($params, $specification);
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'jquery' => 1
    };
    $self->render_page('New reservation request', 'h323-sip/create.html', $params);
}

sub create_alias_action
{
    my ($self) = @_;
    my $params = $self->get_params();
    if ( defined($self->get_param('confirmed')) ) {
        $params->{'error'} = $self->validate_form($params, {
            required => [
                'name',
                'purpose',
                'start',
                'end',
            ],
            constraint_methods => {
                'purpose' => qr/^SCIENCE|EDUCATION$/,
                'start' => 'datetime',
                'end' => ['datetime', Shongo::Web::Controller::constraint_interval(['start', 'end'])],
            }
        });
        if ( !%{$params->{'error'}} ) {
            my $specification = {
                'class' => 'AliasSpecification',
                'technology' => 'H323'
            };
            my $reservation_request = $self->parse_reservation_request($params, $specification);
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'jquery' => 1
    };
    $self->render_page('New H323/SIP alias reservation request', 'h323-sip/create-alias.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->get_reservation_request($id);

    # Add PIN
    my $specification = $request->{'specification'};
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

    $self->render_page('Detail of existing H323/SIP reservation request', 'h323-sip/detail.html', {
        'request' => $request
    });
}

1;