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
    $self->reset_back();
    $self->push_back();
    $self->list_reservation_requests(['ADOBE_CONNECT']);
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
            my $reservation_request = $self->parse_reservation_request($params);
            my $specification = $self->parse_room_specification($params, ['ADOBE_CONNECT']);
            $reservation_request->{'specification'} = $specification;
            $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
            $self->redirect('list');
        }
    }
    $params->{'options'} = {
        'ui' => 1
    };
    $params->{'aliasReservations'} = $self->get_reservations('AliasReservation', ['ADOBE_CONNECT']);
    $self->push_back();
    $self->render_page('New reservation request', 'adobe-connect/create.html', $params);
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
                'value' => {'name' => 'value', 'constraint_method' => qr/^[[:alpha:]][[:alnum:]_-]*$/},
            },
            msgs => {
                constraints => {
                    'value' => 'Invalid value (must be alphabetic followed by alphanumeric, _ or -)'
                }
            }
        });
        if ( !%{$params->{'error'}} ) {
            my $reservation_request = $self->parse_reservation_request($params);
            my $specification = {
                'class' => 'AliasSpecification',
                'aliasTypes' => ['ROOM_NAME'],
                'technologies' => ['ADOBE_CONNECT'],
                'value' => $params->{'roomName'}
            };
            $reservation_request->{'specification'} = $specification;

            my $result = $self->{'application'}->secure_request('Reservation.checkSpecificationAvailability', $specification, $reservation_request->{'slot'});
            if ( $result ne '1' && $result =~ 'already allocated' ) {
                $params->{'error'}->{'roomName'} = $self->format_form_error('Room name is already used in specified time slot.');
            }
            else {
                $self->{'application'}->secure_request('Reservation.createReservationRequest', $reservation_request);
                $self->redirect('list');
            }
        }
    }
    $params->{'options'} = {
        'ui' => 1
    };
    $self->render_page('New reservation request', 'adobe-connect/create-alias.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->get_reservation_request($id);

    $self->push_back();
    $self->render_page('Detail of reservation request', 'common/detail.html', {
        'technologies' => 'Adobe Connect',
        'request' => $request,
    });
}

1;