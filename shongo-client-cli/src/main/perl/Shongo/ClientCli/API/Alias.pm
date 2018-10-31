#
# Alias for devices providing virtual rooms
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
    'ROOM_NAME' => 'Room Name',
    'H323_E164' => 'H.323 Phone Number',
    'H323_URI' => 'H.323 URI',
    'H323_IP' => 'H.323 IP',
    'SIP_URI' => 'SIP URI',
    'SIP_IP' => 'SIP IP',
    'CS_DIAL_STRING' => 'ClearSea Dial String',
    'ADOBE_CONNECT_URI' => 'Adobe Connect URI',
    'FREEPBX_CONFERENCE_NUMBER' => 'FreePBX Conference Number',
    'SKYPE_URI' => 'Skype URI',
    'WEB_CLIENT_URI' => 'Web Client URI',
    'ROOM_NUMBER' => 'Room Number',
    'PEXIP_ROOM_NUMBER_URI' => 'Pexip Room Number Uri',
    'PEXIP_PHONE_NUMBER_URI' => 'Pexip Phone Number Uri'
);

# Regular expression patters for type values
our $TypePattern = {
    'ROOM_NAME' => '.*',
    'H323_E164' => '\\d+',
    'H323_URI' => '.*',
    'H323_IP' => '.*',
    'SIP_URI' => '.+@.+',
    'SIP_IP' => '.*',
    'CS_DIAL_STRING' => '.+@.+',
    'ADOBE_CONNECT_URI' => '.+',
    'FREEPBX_CONFERENCE_NUMBER' => '\\d+',
    'SKYPE_URI' => '.+@.+',
    'WEB_CLIENT_URI' => '.*',
    'ROOM_NUMBER' => '.*',
    'PEXIP_ROOM_NUMBER_URI' => '.+@.+',
    'PEXIP_PHONE_NUMBER_URI' => '.+@.+'
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
    $self->add_attribute('type', {
        'required' => 1,
        'type' => 'enum',
        'enum' =>  $Type
    });
    $self->add_attribute('value', {
        'required' => 1,
        'type' => 'string',
        'string-pattern' => sub {
            if ( !defined($self->get('type')) ) {
                return undef;
            }
            return $TypePattern->{$self->get('type')};
        }
    });

    return $self;
}

1;
