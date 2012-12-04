#
# Common services.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::CommonService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;

# Enumeration of status
our $Status = ordered_hash('AVAILABLE' => 'Available', 'NOT_AVAILABLE' => 'Not-Available');

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'list-domains' => {
            desc => 'List known domains to the controller',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_domains($params->{'options'});
            }
        },
        'list-connectors' => {
            desc => 'List known connectors to the controller',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_connectors($params->{'options'});
            }
        }
    });
}

sub list_domains()
{
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Common.listDomains'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(\'| ', 'Name', \' | ', 'Organization', \' | ', 'Status', \' |');
    foreach my $domain (@{$response->value()}) {
        $table->add(
            $domain->{'name'},
            $domain->{'organization'},
            $Status->{$domain->{'status'}}
        );
    }
    console_print_table($table);
}

sub list_connectors()
{
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Common.listConnectors'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(\'| ', 'Agent Name', \' | ', 'Managed Resource', \' | ', 'Status', \' |');
    foreach my $connector (@{$response->value()}) {
        $table->add(
            $connector->{'name'},
            $connector->{'resourceIdentifier'},
            $Status->{$connector->{'status'}}
        );
    }
    console_print_table($table);
}

1;