#
# Management of compartments.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::CompartmentService;

use strict;
use warnings;
use Text::Table;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Compartment;

#
# Populate shell by options for management of reservations.
#
# @param shell
#
sub populate()
{
    my ($self, $shell) = @_;
    $shell->add_commands({
        'delete-compartment' => {
            desc => 'Delete an existing compartment',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                delete_compartment($args[0]);
            }
        },
        'list-compartments' => {
            desc => 'List summary of all existing compartments',
            opts => '',
            method => sub {
                my ($shell, $params, @args) = @_;
                list_compartments($params->{'options'});
            }
        },
        'get-compartment' => {
            desc => 'Get existing compartment',
            args => '[identifier]',
            method => sub {
                my ($shell, $params, @args) = @_;
                if (defined($args[0])) {
                    foreach my $identifier (split(/,/, $args[0])) {
                        get_compartment($identifier);
                    }
                } else {
                    get_compartment();
                }
            }
        },
    });
}

sub select_compartment($)
{
    my ($identifier) = @_;
    $identifier = console_read_value('Identifier of the compartment', 0, $Shongo::Common::IdentifierPattern, $identifier);
    return $identifier;
}

sub delete_compartment()
{
    my ($identifier) = @_;
    $identifier = select_compartment($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    Shongo::Controller->instance()->secure_request(
        'Compartment.deleteCompartment',
        RPC::XML::string->new($identifier)
    );
}

sub list_compartments()
{
    my $response = Shongo::Controller->instance()->secure_request(
        'Compartment.listCompartments'
    );
    if ( $response->is_fault() ) {
        return
    }
    my $table = Text::Table->new(
        \'| ', 'Identifier',
        \' | ', 'Slot',
        \' | ', 'State',
        \' |'
    );
    foreach my $compartment (@{$response->value()}) {
        $table->add(
            $compartment->{'identifier'},
            format_interval($compartment->{'slot'}),
            Shongo::Controller::API::Compartment::format_state($compartment->{'state'}, $Shongo::Controller::API::Compartment::State)
        );
    }
    console_print_table($table);
}

sub get_compartment()
{
    my ($identifier) = @_;
    $identifier = select_compartment($identifier);
    if ( !defined($identifier) ) {
        return;
    }
    my $result = Shongo::Controller->instance()->secure_request(
        'Compartment.getCompartment',
        RPC::XML::string->new($identifier)
    );
    if ( !$result->is_fault ) {
        my $compartment = Shongo::Controller::API::Compartment->from_hash($result);
        if ( defined($compartment) ) {
            console_print_text($compartment->to_string());
        }
    }
}

1;