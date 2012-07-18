#
# Reservation request
#
package Shongo::Controller::API::Object;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;

#
# Create a new instance of object
#
# @static
#
sub new
{
    my $class = shift;
    my $self = {};
    bless $self, $class;

    $self->{'identifier'} = undef;
    $self->{'__to_xml_skip_attributes'} = {'class' => 1};

    return $self;
}

#
# Skip given attribute in to_xml calls
#
# @param $attribute_name
#
sub to_xml_skip_attribute
{
    my ($self, $attribute_name) = @_;
    $self->{'__to_xml_skip_attributes'}->{$attribute_name} = 1;
}

#
# Get number of items in collection. Collection can be stored as hash('new' => [], 'modified' => [], 'deleted' => []).
#
# @param $collection  collection name
# @return collection size
#
sub get_collection_size
{
    my ($self, $collection) = @_;
    if ( ref($self->{$collection}) eq 'ARRAY' ) {
        return scalar(@{$self->{$collection}});
    }
    elsif ( ref($self->{$collection}) eq 'HASH' ) {
        my $count = 0;
        if ( defined($self->{$collection}->{'modified'}) ) {
            $count += scalar(@{$self->{$collection}->{'modified'}});
        }
        if ( defined($self->{$collection}->{'new'}) ) {
            $count += scalar(@{$self->{$collection}->{'new'}});
        }
        return $count;
    }
    else {
        return 0;
    }
}

#
# Get collection items as array. Collection can be stored as hash('new' => [], 'modified' => [], 'deleted' => []).
#
# @param $collection  collection name
# @return array of items
#
sub get_collection_items
{
    my ($self, $collection) = @_;
    if ( ref($self->{$collection}) eq 'ARRAY' ) {
        return $self->{$collection};
    }
    elsif ( ref($self->{$collection}) eq 'HASH' ) {
        my $array = [];
        if ( defined($self->{$collection}->{'modified'}) ) {
            push($array, @{$self->{$collection}->{'modified'}});
        }
        if ( defined($self->{$collection}->{'new'}) ) {
            push($array, @{$self->{$collection}->{'new'}});
        }
        return $array;
    }
    else {
        return [];
    }
}

#
# Get item from collection by index
#
# @param $collection  collection naem
# @param $item_index  item index
#
sub get_collection_item
{
    my ($self, $collection, $item_index) = @_;
    if ( ref($self->{$collection}) eq 'ARRAY' ) {
        return $self->{$collection}->[$item_index];
    }
    elsif ( ref($self->{$collection}) eq 'HASH' ) {
        my $items = $self->get_collection_items($collection);
        return $items->[$item_index];
    }
    else {
        return undef;
    }
}

#
# Convert collection to hash
#
# @param $collection  collection name
#
sub convert_collection_to_hash
{
    my ($self, $collection) = @_;
    if ( ref($self->{$collection}) eq 'HASH' ) {
        # Do nothing
    }
    elsif ( ref($self->{$collection}) eq 'ARRAY' ) {
        $self->{$collection} = {'modified' => $self->{$collection}};
    }
    else {
        $self->{$collection} = {};
    };
}

#
# Add item to collection
#
# @param $collection  collection name
# @param $item        new item
#
sub add_collection_item
{
    my ($self, $collection, $item) = @_;
    $self->convert_collection_to_hash($collection);
    if ( !defined($self->{$collection}->{'new'}) ) {
        $self->{$collection}->{'new'} = [];
    }
    push($self->{$collection}->{'new'}, $item);
}

#
# Remove item from collection
#
sub remove_collection_item
{
    my ($self, $collection, $item_index) = @_;
    $self->convert_collection_to_hash($collection);
    if ( defined($self->{$collection}->{'modified'}) ) {
        my $modified_count = scalar(@{$self->{$collection}->{'modified'}});
        if ( $item_index < $modified_count ) {
            my $item = $self->{$collection}->{'modified'}[$item_index];
            splice($self->{$collection}->{'modified'}, $item_index, 1);
            if ( !defined($self->{$collection}->{'deleted'}) ) {
                $self->{$collection}->{'deleted'} = [];
            }
            push($self->{$collection}->{'deleted'}, $item);
            return;
        } else {
            $item_index -= $modified_count;
        }
    }
    if ( defined($self->{$collection}->{'new'}) ) {
        my $new_count = scalar(@{$self->{$collection}->{'new'}});
        if ( $item_index < $new_count ) {
            splice($self->{$collection}->{'new'}, $item_index, 1);
            return;
        }
    }
    console_print_error("Cannot delete item with index '%d' in collection '%s'.", $item_index, $collection);
}

#
# Convert $value to xml
#
# @param $value
#
sub to_xml_value
{
    my ($value) = @_;
    if ( ref($value) eq 'HASH' ) {
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            if ( !($item_name eq "class") ) {
                $hash->{$item_name} = to_xml_value($item_value);
            }
        }
        return RPC::XML::struct->new($hash);
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push($array, to_xml_value($item));
        }
        return RPC::XML::array->new(from => $array);
    }
    elsif ( ref($value) ) {
        return $value->to_xml($value);
    }
    else {
        return RPC::XML::string->new($value);
    }
}

#
# Convert object to xml
#
sub to_xml()
{
    my ($self) = @_;

    my $xml = {};
    foreach my $name (keys %{$self}) {
        my $value = $self->{$name};
        if ( !($name eq "__to_xml_skip_attributes") && !($self->{'__to_xml_skip_attributes'}->{$name}) ) {
            $xml->{$name} = to_xml_value($value);
        }
    }
    return RPC::XML::struct->new($xml);
}

#
# Convert $value from xml
#
# @param $value
#
sub from_xml_value
{
    my ($value) = @_;

    if ( ref($value) eq 'HASH' ) {
        if ( exists $value->{'class'} ) {
            my $object = eval('Shongo::Controller::API::' . $value->{'class'} . '->new()');
            if ( defined($object) ) {
                $object->from_xml($value);
                return $object;
            }
        }
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            if ( !($item_name eq "class") ) {
                $hash->{$item_name} = from_xml_value($item_value);
            }
        }
        return $hash;
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push($array, from_xml_value($item));
        }
        return $array;
    }
    else {
        return $value;
    }
}

#
# Convert object from xml
#
sub from_xml()
{
    my ($self, $xml) = @_;

    # Get hash from xml
    my $hash = $xml;
    if ( ref($hash) eq "RPC::XML::struct" ) {
        $hash = $xml->value();
    }

    # Convert hash to object
    foreach my $name (keys %{$hash}) {
        my $value = $hash->{$name};
        if ( !($name eq "class") && exists($self->{$name}) ) {
            $self->{$name} = from_xml_value($value);
        }
    }
    return $self;
}

1;