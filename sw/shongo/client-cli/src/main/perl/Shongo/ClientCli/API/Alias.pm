#
# Alias for video conference devices
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Alias;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::DeviceResource;

# Enumeration of alias types
our $Type = ordered_hash(
    'E164' => 'E.164 Phone Number',
    'IDENTIFIER' => 'String Identifier',
    'URI' => 'URI'
);

# Regular expression patters for type values
our $TypePattern = {
    'E164' => '\\d+',
    'IDENTIFIER' => '.+',
    'URI' => '.+@.+'
};

#
# Create a new instance of alias
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Alias');
    $self->set_object_name('Alias');
    $self->add_attribute(
        'technology', {
            'required' => 1,
            'type' => 'enum',
            'enum' =>  $Shongo::ClientCli::API::DeviceResource::Technology
        }
    );
    $self->add_attribute(
        'type', {
            'required' => 1,
            'type' => 'enum',
            'enum' =>  $Type
        }
    );
    $self->add_attribute(
        'value', {
            'required' => 1,
            'type' => 'string',
            'string-pattern' => sub {
                if ( !defined($self->get('type')) ) {
                    return undef;
                }
                return $TypePattern->{$self->get('type')};
            }
        }
    );

    return $self;
}

1;