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
# Populate shell by options for common management.
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
    if ( !defined($response) ) {
        return
    }
    my $table = {
        'columns' => [
            {'field' => 'name',         'title' => 'Name'},
            {'field' => 'organization', 'title' => 'Organization'},
            {'field' => 'status',       'title' => 'Status'},
        ],
        'data' => []
    };
    foreach my $domain (@{$response}) {
        push(@{$table->{'data'}}, {
            'name' => $domain->{'name'},
            'organization' => $domain->{'organization'},
            'status' => $Status->{$domain->{'status'}}
        });
    }
    console_print_table($table);
}

sub list_connectors()
{
    my $response = Shongo::ClientCli->instance()->secure_request(
        'Common.listConnectors'
    );
    if ( !defined($response) ) {
        return
    }
    my $table = {
        'columns' => [
            {'field' => 'agent',     'title' => 'Agent Name'},
            {'field' => 'resource',   'title' => 'Managed Resource'},
            {'field' => 'status', 'title' => 'Status'}
        ],
        'data' => []
    };
    foreach my $connector (@{$response}) {
        push(@{$table->{'data'}}, {
            'agent' => $connector->{'name'},
            'resource' => $connector->{'resourceId'},
            'status' => $Status->{$connector->{'status'}}
        });
    }
    console_print_table($table);
}

1;