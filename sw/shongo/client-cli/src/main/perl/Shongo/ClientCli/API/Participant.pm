#
# Participant
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Participant;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

our $Type = ordered_hash(
    'ExternalEndpointParticipant' => 'External Endpoint',
    'ExternalEndpointSetParticipant' => 'Set of External Endpoints',
    'ExistingEndpointParticipant' => 'Existing Endpoint',
    'LookupEndpointParticipant' => 'Lookup Resource',
    'PersonParticipant' => 'Person',
    'InvitedPersonParticipant' => 'Invited Person',
);

#
# Create a new instance of participant
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return participant class
#
sub select_type($)
{
    my ($type) = @_;
    return console_edit_enum('Select type of participant', $Type, $type);
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    my $participant = $attributes->{'class'};
    if ( !defined($participant) ) {
        $participant = $self->select_type();
    }
    if ( defined($participant) ) {
        $self->set_object_class($participant);
    }
}

# @Override
sub on_init()
{
    my ($self) = @_;

    my $class = $self->get_object_class();
    if ( !defined($class) ) {
        return;
    }

    if ( exists $Type->{$class} ) {
        $self->set_object_name($Type->{$class});
    }

    switch ($class) {
        case 'ExternalEndpointParticipant' {
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('alias', {
                'modify' => sub {
                    my $hasAlias = 0;
                    if ( defined($self->{'alias'}) ) {
                        $hasAlias = 1;
                    }
                    $hasAlias = console_edit_bool("Has alias", 1, $hasAlias);
                    if ( $hasAlias ) {
                        if ( !defined($self->{'alias'}) ) {
                            $self->{'alias'} = Shongo::ClientCli::API::Alias->create();
                        } else {
                            $self->{'alias'}->modify();
                        }
                    } else {
                        $self->{'alias'} = undef;
                    }
                    return $self->{'alias'};
                }
            });
        }
        case 'ExternalEndpointSetParticipant' {
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::ClientCli::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('count', {
                'type' => 'int',
                'required' => 1
            });
        }
        case 'ExistingEndpointParticipant' {
            $self->add_attribute('resourceId', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdPattern,
                'required' => 1
            });
        }
        case 'LookupEndpointParticipant' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::ClientCli::API::DeviceResource::Technology,
                'required' => 1
            });
        }
        case 'PersonParticipant' {
            $self->add_attribute('person', {
                'modify' => sub() {
                    my ($person) = @_;
                    if ( !defined($person) ) {
                        $person = Shongo::ClientCli::API::Person->new();
                    }
                    $person->modify();
                    return $person;
                },
                'required' => 1
            });
        }
    }
}

1;