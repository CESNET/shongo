#
# Base API object
#
# @author Martin Srom <martin.srom@cesnet.cz>
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
    my ($self, $value) = @_;
    if ( ref($value) eq 'HASH' ) {
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            $hash->{$item_name} = $self->to_xml_value($item_value);
        }
        return RPC::XML::struct->new($hash);
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->to_xml_value($item));
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
            $xml->{$name} = $self->to_xml_value($value);
        }
    }
    return RPC::XML::struct->new($xml);
}

#
# Create new instance of value
#
# @param $class
# @param $attribute
#
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    return eval('Shongo::Controller::API::' . $class . '->new()');
}

#
# Convert $value from xml
#
# @param $value
#
sub from_xml_value
{
    my ($self, $value, $attribute) = @_;

    if ( ref($value) eq 'HASH' ) {
        if ( exists $value->{'class'} ) {
            my $object = $self->create_value_instance($value->{'class'}, $attribute);
            if ( defined($object) ) {
                $object->from_xml($value);
                return $object;
            }
        }
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            $hash->{$item_name} = $self->from_xml_value($item_value, $item_name);
        }
        return $hash;
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->from_xml_value($item, $attribute));
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

    if ( !ref($self) ) {
        if ( exists $hash->{'class'} ) {
            $self = eval('Shongo::Controller::API::' . $hash->{'class'} . '->new()');
            if (!defined($self)) {
                die("Cannot instantiate class '" . $hash->{'class'} . "'.");
            }
        } else {
            var_dump($hash);
            die("Cannot convert printed hash to object.");
        }
    }

    # Convert hash to object
    foreach my $name (keys %{$hash}) {
        my $value = $hash->{$name};
        $self->{$name} = $self->from_xml_value($value, $name);
    }
    return $self;
}

#
# Convert object to string
#
# @return string describing this object
#
sub to_string
{
    my ($self) = @_;

    my $string = " " . uc($self->to_string_name()) . "\n";
    $string .= $self->to_string_attributes();
    $string .= $self->to_string_collections();
    return $string;
}

#
# @return name of the object
#
sub to_string_name
{
    return "OBJECT"
}

#
# @return formatted attributes to string
#
sub to_string_attributes
{
    return "";
}

#
# @return formatted collections to string
#
sub to_string_collections
{
    return "";
}

1;