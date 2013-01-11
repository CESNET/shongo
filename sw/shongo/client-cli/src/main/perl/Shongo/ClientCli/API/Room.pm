#
# Room
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Room;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

our $Option = ordered_hash(
    'DESCRIPTION'           => {'title' => 'Description',           'type' => 'string'},
    'PIN'                   => {'title' => 'Pin',                   'type' => 'string'},
    'LISTED_PUBLICLY'       => {'title' => 'Listed Publicly',       'type' => 'bool'}
);

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

    $self->set_object_name('Room');
    $self->set_object_class('Room');
    $self->add_attribute('name', {'required' => 1});
    $self->add_attribute('description');
    $self->add_attribute('licenseCount', {'title' => 'License Count', 'type' => 'int', 'required' => 1});
    $self->add_attribute(
        'technologies', {
            'type' => 'collection',
            'item' => {
                'title' => 'Technology',
                'enum' => $Shongo::Common::Technology
            },
            'required' => 1
        }
    );
    $self->add_attribute('aliases', {
        'type' => 'collection',
        'item' => {
            'title' => 'Alias',
            'class' => 'Shongo::ClientCli::API::Alias',
            'short' => 1
        }
    });
    $self->add_attribute('options', {
        'type' => 'map',
        'item' => {
            'title' => 'Option',
            'format' => sub {
                my ($item_key, $item_value) = @_;
                if ( defined($Option->{$item_key}) ) {
                    $item_key = $Option->{$item_key}->{'title'};
                }
                return "$item_key: $item_value";
            },
            'add' => sub {
                my $options = [];
                foreach my $item_key (ordered_hash_keys($Option)) {
                    push(@{$options}, $item_key => $Option->{$item_key}->{'title'});
                }
                my $item_key = console_read_enum('Select', ordered_hash($options));
                if ( !defined($Option->{$item_key}) ) {
                    return;
                }
                my $option = $Option->{$item_key};
                my $item_value = Shongo::ClientCli::API::Object->modify_attribute_value($option->{'title'}, undef, $option, 0);
                return ($item_key, $item_value);
            },
            'modify' => sub {
                my ($item_key, $item_value) = @_;
                my $option = $Option->{$item_key};
                $item_value = Shongo::ClientCli::API::Object->modify_attribute_value($option->{'title'}, $item_value, $option, 1);
                return $item_value;
            }
        },
        'display-empty' => 1
    });

    return $self;
}

1;