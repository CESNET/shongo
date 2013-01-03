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
    $self->list_reservation_requests('List of existing Adobe Connect reservation requests', ['ADOBE_CONNECT']);
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
    $self->render_page('New reservation request', 'adobe-connect/create.html', $params);
}

sub detail_action
{
    my ($self) = @_;
    my $id = $self->get_param_required('id');
    my $request = $self->get_reservation_request($id);
    $self->render_page('Detail of existing Adobe Connect reservation request', 'adobe-connect/detail.html', {
        'request' => $request
    });
}

1;