#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Resource;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli::API::Capability;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Resource');
    $self->set_object_name('Resource');
    $self->add_attribute('id', {
        'title' => 'Identifier',
        'editable' => 0
    });
    $self->add_attribute('userId', {
        'title' => 'Owner',
        'format' => sub { return Shongo::ClientCli->instance()->format_user(@_, 1); },
        'editable' => 0
    });
    $self->add_attribute('name', {
        'required' => 1
    });
    $self->add_attribute('description');
    $self->add_attribute('parentResourceId', {
        'title' => 'Parent',
        'string-pattern' => $Shongo::Common::IdPattern
    });
    $self->add_attribute('allocationOrder', {
        'title' => 'Allocation Order',
        'type' => 'int',
        'format' => sub {
            my ($attribute_value) = @_;
            if (defined($attribute_value)) {
                return $attribute_value;
            }
            else {
                return "last";
            }
        }
    });
    $self->add_attribute('allocatable', {
        'type' => 'bool',
        'required' => 1
    });
    $self->add_attribute('maximumFuture', {
        'title' => 'Maximum Future',
        'type' => 'period'
    });
    $self->add_attribute('calendarPublic', {
        'title' => 'Calendar Public',
        'type' => 'bool',
        'required' => 1
    });
    $self->add_attribute('calendarUriKey', {
        'title' => 'Calendar URI key',
        'editable' => 0
    });
    $self->add_attribute('confirmByOwner', {
        'title' => 'Confirm requests by owner',
        'type' => 'bool',
        'required' => 1
    });
    $self->add_attribute('remoteCalendarName', {
        'title' => 'Remote calendar name',
        });
    $self->add_attribute('childResourceIds', {
        'title' => 'Children',
        'format' => sub {
            my ($attribute_value) = @_;
            my $string = '';
            foreach my $id (@{$attribute_value}) {
                if ( length($string) > 0 ) {
                    $string .= ', ';
                }
                $string .= $id;
            }
            return $string;
        },
        'read-only' => 1
    });
    $self->add_attribute('administratorEmails', {
        'title' => 'Administrator Emails',
        'type' => 'collection',
        'item' => {
            'title' => 'Administrator Email',
            'add' => sub {
                console_read_value('Administrator Email', 1);
            }
        },
        'display-empty' => 1
    });
    $self->add_attribute('capabilities', {
        'type' => 'collection',
        'item' => {
            'title' => 'Capability',
            'class' => 'Shongo::ClientCli::API::Capability'
        },
        'display-empty' => 1
    });
    return $self;
}

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    my $class = $attributes->{'class'};
    if ( !defined($class) ) {
        $class = console_read_enum('Select type of resource', ordered_hash(
            'Resource' => 'Other Resource',
            'DeviceResource' => 'Device Resource'
        ));
    }
    if ($class eq 'Resource') {
        return Shongo::ClientCli::API::Resource->new();
    } elsif ($class eq 'DeviceResource') {
        return Shongo::ClientCli::API::DeviceResource->new();
    }
    die("Unknown resource type '$class'.");
}

1;