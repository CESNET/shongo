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
    'H323_E164' => 'H.323 Phone Number',
    'H323_IDENTIFIER' => 'H.323 Identifier',
    'SIP_URI' => 'SIP URI',
    'ADOBE_CONNECT_NAME' => 'Adobe Connect Room Name',
    'ADOBE_CONNECT_URI' => 'Adobe Connect URI'
);

# Regular expression patters for type values
our $TypePattern = {
    'H323_E164' => '\\d+',
    'H323_IDENTIFIER' => '.+',
    'SIP_URI' => '.+@.+',
    'ADOBE_CONNECT_NAME' => '.*',
    'ADOBE_CONNECT_URI' => '.+@.+'
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