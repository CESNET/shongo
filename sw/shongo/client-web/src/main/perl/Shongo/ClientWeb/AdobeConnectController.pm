#
# Controller for Adobe Connect video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientWeb::AdobeConnectController;
use base qw(Shongo::ClientWeb::CommonController);

use strict;
use warnings;
use Shongo::Common;

sub new
{
    my $class = shift;
    my $self = Shongo::ClientWeb::CommonController->new('adobe-connect', @_);
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
    $self->list_reservation_requests(['ADOBE_CONNECT']);
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
                'alias',
                'participantCount',
            ],
            optional => [
                'periodicityEnd'
            ],
            constraint_methods => {
                'purpose' => qr/^SCIENCE|EDUCATION$/,
                'start' => 'datetime',
                'durationCount' => 'number',
                'periodicity' => qr/^none|daily|weekly$/,
                'periodicityEnd' => 'date',
                'participantCount' => 'number'
            }
        });
        if ( !%{$params->{'error'}} ) {
            my $specification = $self->parse_room_specification($params, ['ADOBE_CONNECT']);

            # Add participant
            my $participant = 'srom@cesnet.cz';#$self->{'application'}->get_user()->{'original_id'};
            # TODO: fill participant;
            if ( defined($participant) ) {
                $specification->{'roomSettings'} = [{
                    'class' => 'RoomSetting.AdobeConnect',
                    'participants' => [$participant]
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
    $params->{'aliasReservations'} = $self->get_reservations('AliasReservation', ['ADOBE_CONNECT']);
    foreach my $alias_reservation (@{$params->{'aliasReservations'}}) {
        $self->process_reservation_alias($alias_reservation);
    }
    $self->render_page('New reservation request', 'adobe-connect/create.html', $params);
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
                'value',
            ],
            constraint_methods => {
                'purpose' => qr/^SCIENCE|EDUCATION$/,
                'start' => 'datetime',
                'end' => ['datetime', Shongo::Web::Controller::constraint_interval(['start', 'end'])],
                'value' => {'name' => 'value', 'constraint_method' => qr/^[[:alpha:]][[:alnum:]_-]*$/},
            },
            msgs => {
                constraints => {
                    'value' => 'Invalid value (must be alphabetic followed by alphanumeric, _ or -)'
                }
            }
        });
        if ( !%{$params->{'error'}} ) {
            my $specification = {
                'class' => 'AliasSpecification',
                'technology' => 'ADOBE_CONNECT',
                'value' => $params->{'value'}
            };
            my $reservation_request = $self->parse_reservation_request($params, $specification);
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'jquery' => 1
    };
    $self->render_page('New reservation request', 'adobe-connect/create-alias.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->get_reservation_request($id);
    $self->render_page('Detail of reservation request', 'common/detail.html', {
        'technologies' => 'Adobe Connect',
        'request' => $request,
    });
}

# @Override
sub process_reservation_alias
{
    my ($self, $reservation_alias, $available) = @_;

    my $value = $reservation_alias->{'aliasValue'};
    foreach my $alias (@{$reservation_alias->{'aliases'}}) {
        if ( $alias->{'type'} eq 'ADOBE_CONNECT_NAME' ) {
            $value = $alias->{'value'};
        }
    }
    $reservation_alias->{'value'} = $value;

    $self->SUPER::process_reservation_alias($reservation_alias, $available);
}

1;