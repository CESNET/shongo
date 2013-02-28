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
    $self->reset_back();
    $self->push_back();
    $self->list_reservation_requests(['H323', 'SIP']);
}

sub create_action
{
    my ($self) = @_;
    my $params = $self->get_params();
    if ( defined($self->get_param('confirmed')) ) {
        $params->{'error'} = $self->validate_form($params, {
            required => [
                'description',
                'purpose',
                'start',
                'durationCount',
                'periodicity',
                'alias',
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
                    'class' => 'H323RoomSetting',
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
    $params->{'aliasReservations'} = $self->get_reservations('AliasReservation', ['H323', 'SIP']);
    $self->push_back();
    $self->render_page('New reservation request', 'h323-sip/create.html', $params);
}

sub create_alias_action
{
    my ($self) = @_;
    my $params = $self->get_params();
    if ( defined($self->get_param('confirmed')) ) {
        $params->{'error'} = $self->validate_form($params, {
            required => [
                'roomName',
                'description',
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
                'class' => 'AliasSetSpecification',
                'aliases' => [{
                    'aliasTypes' => ['ROOM_NAME'],
                    'technologies' => ['H323', 'SIP'],
                    'value' => $params->{'roomName'}
                },{
                    'aliasTypes' => ['H323_E164']
                }]
            };
            my $reservation_request = $self->parse_reservation_request($params, $specification);
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'jquery' => 1
    };
    $self->render_page('New reservation request', 'h323-sip/create-alias.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->get_reservation_request($id);

    # Add PIN
    my $specification = $request->{'specification'};
    if ( $specification->{'class'} eq 'RoomSpecification' ) {
        if ( defined($specification->{'roomSettings'}) && @{$specification->{'roomSettings'}} > 0 ) {
            my $roomSetting = $specification->{'roomSettings'}->[0];
            if ( $roomSetting->{'class'} ne 'H323RoomSetting' ) {
                $self->error("Reservation request should contains only room setttings for 'H323' but '$roomSetting->{'class'}' was present.");
            }
            $request->{'pin'} = $roomSetting->{'pin'};
        }
        if ( !defined($request->{'pin'}) || $request->{'pin'} eq '' ) {
            $request->{'pin'} = '<span class="empty">None</span>';
        }
        push(@{$request->{'attributes'}}, {'name' => 'PIN', 'value' => $request->{'pin'}});
    }

    $self->push_back();
    $self->render_page('Detail of reservation request', 'common/detail.html', {
        'technologies' => 'H.323/SIP',
        'request' => $request,
    });
}

1;