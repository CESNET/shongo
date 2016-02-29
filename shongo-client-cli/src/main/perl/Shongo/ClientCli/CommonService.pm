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

# Enumerations
our $DomainStatus = ordered_hash('AVAILABLE' => 'Available', 'NOT_AVAILABLE' => 'Not-Available');
our $AgentState = ordered_hash('AVAILABLE' => 'Available', 'NOT_AVAILABLE' => 'Not-Available');
our $ConnectorState = ordered_hash('AVAILABLE' => 'Available', 'NOT_AVAILABLE' => 'Not-Available');

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
            {'field' => 'id',         'title' => 'Identifier'},
            {'field' => 'name',         'title' => 'Name'},
            {'field' => 'organization', 'title' => 'Organization'},
            {'field' => 'allocatable', 'title' => 'Allocatable'},
            {'field' => 'status',       'title' => 'Status'},
            {'field' => 'url',       'title' => 'Url'},
            {'field' => 'certificatePath',       'title' => 'Certificate'},
            {'field' => 'shareAuthorizationServer',       'title' => 'Shared AA'}
        ],
        'data' => []
    };
    foreach my $domain (@{$response}) {
        my $url = length $domain->{'url'} ? "$domain->{'url'}:$domain->{'port'}" : "none";
        my $id = length $domain->{'id'} ? $domain->{'id'} : "none";
        my $status = length $domain->{'status'} ? $domain->{'status'} : "ERROR";
        my $certificatePath = length $domain->{'certificatePath'} ? $domain->{'certificatePath'} : "none";
        push(@{$table->{'data'}}, {
            'id' => $id,
            'name' => $domain->{'name'},
            'organization' => $domain->{'organization'},
            'allocatable' => $domain->{'allocatable'} ? 'yes' : 'no',
            'status' => $status,
            'url' => $url,
            'certificatePath' => $certificatePath,
            'shareAuthorizationServer' => $domain->{'shareAuthorizationServer'} ? 'yes' : 'no'
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
            {'field' => 'agent',      'title' => 'Agent Name'},
            {'field' => 'agentState', 'title' => 'Agent State'},
            {'field' => 'resource',   'title' => 'Managed Resource'},
            {'field' => 'status',   'title' => 'Status'},
        ],
        'data' => []
    };
    foreach my $connector (@{$response}) {
        push(@{$table->{'data'}}, {
            'agent' => $connector->{'name'},
            'agentState' => [$connector->{'agentState'}, $AgentState->{$connector->{'agentState'}}],
            'resource' => $connector->{'resourceId'},
            'status' => [$connector->{'status'}->{'state'}, $ConnectorState->{$connector->{'status'}->{'state'}}]
        });
    }
    console_print_table($table);
}

1;
