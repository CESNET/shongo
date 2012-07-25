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

    $self->{'__to_xml_skip_attributes'} = {};

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
            $hash->{$item_name} = to_xml_value($item_value);
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
    elsif ( !defined($value) ) {
        return RPC::XML::struct->new();
    }
    else {
        return $value;
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
            $hash->{$item_name} = from_xml_value($item_value);
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
        if ( !($name eq "class") ) {
            $self->{$name} = from_xml_value($value);
        }
    }
    return $self;
}

1;