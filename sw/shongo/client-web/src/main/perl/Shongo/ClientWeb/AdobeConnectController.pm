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
    $self->render_page_content('TODO', "TODO: Adobe Connect");
}

sub detail_action
{
    my ($self) = @_;
    $self->render_page_content('TODO', "TODO: Adobe Connect");
}

sub delete_action
{
    my ($self) = @_;
    $self->render_page_content('TODO', "TODO: Adobe Connect");
}

1;