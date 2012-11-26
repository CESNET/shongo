#
# Controller for Adobe Connect video conferences.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::AdobeConnectController;
use base qw(Shongo::Web::Controller);

use strict;
use warnings;
use Shongo::Common;

sub new
{
    my $class = shift;
    my $self = Shongo::Web::Controller->new('adobe-connect', @_);
    bless $self, $class;

    return $self;
}

sub index_action
{
    my ($self) = @_;
    $self->render_page_content('TODO', "TODO: Adobe Connect");
}

1;