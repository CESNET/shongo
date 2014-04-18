#
# Executable Service Specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ExecutableServiceSpecification;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;

# Enumeration of available types
our $Type = ordered_hash(
    'RECORDING' => 'Recording',
    'STREAMING' => 'Streaming'
);

#
# Create a new instance
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_name('Executable Service');
    $self->set_object_class('ExecutableServiceSpecification');

    $self->add_attribute('type', {
        'required' => 1,
        'type' => 'enum',
        'enum' =>  $Type
    });
    $self->add_attribute('executableId', {
        'type' => 'string'
    });
    $self->add_attribute('enabled', {
        'type' => 'bool',
        'required' => 1
    });

    return $self;
}

1;