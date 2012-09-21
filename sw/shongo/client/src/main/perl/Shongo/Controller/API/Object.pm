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

our $COLOR_HEADER = "bold blue";
our $COLOR = "bold white";

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
    elsif ( !defined($value) || $value eq NULL ) {
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
# @return name of the object
#
sub get_name
{
    my ($self) = @_;
    return "OBJECT"
}

#
# Get attributes for this object
#
# @param $attributes to be populated
#
sub get_attributes
{   my ($self, $attributes) = @_;
}

#
# Create collection
#
# @param $name
#
sub create_collection
{
    my ($self, $name, $item_to_string_callback) = @_;
    my $collection = {};
    $collection->{'name'} = $name;
    $collection->{'item_to_string_callback'} = $item_to_string_callback;
    $collection->{'items'} = [];
    $collection->{'add'} = sub{
        my ($item) = @_;
        push(@{$collection->{'items'}}, $item);
    };
    $collection->{'to_string'} = sub{
        my ($item) = @_;
        my $string = colored($collection->{'name'}, $COLOR) . ':';
        if ( @{$collection->{'items'}} > 0 ) {
            for ( my $index = 0; $index < scalar(@{$collection->{'items'}}); $index++ ) {
                my $item = @{$collection->{'items'}}[$index];
                if ( defined($collection->{'item_to_string_callback'}) ) {
                    $item = $collection->{'item_to_string_callback'}($item);
                }
                elsif ( ref($item) ) {
                    $item = $item->to_string();
                }
                $item = text_indent_lines($item, 4, 0);
                $string .= sprintf("\n %s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
            }
        }
        else {
            $string .= "\n -- None --";
        }
        return $string;
    };
    return $collection;
}

#
# @return formatted collections to string
#
sub to_string_collections
{
    my ($self) = @_;
    return "";
}

#
# Convert object to string
#
# @return string describing this object
#
sub to_string
{
    my ($self) = @_;

    # get attributes for this object
    my $attributes = {};
    $attributes->{'attributes'} = [];
    $attributes->{'collections'} = [];
    $attributes->{'single_line'} = 0;
    $attributes->{'add'} = sub{
        my ($name, $value, $description) = @_;
        push(@{$attributes->{'attributes'}}, {'name' => $name, 'value' => $value, 'description' => $description});
    };
    $attributes->{'add_collection'} = sub{
        my ($name) = @_;
        if ( ref($name) ) {
            push(@{$attributes->{'collections'}}, $name);
            return;
        }
        my $collection = $self->create_collection($name);
        push(@{$attributes->{'collections'}}, $collection);
        return $collection;
    };
    $self->get_attributes($attributes);

    # determine maximum attribute name length
    my $max_length = 0;
    foreach my $attribute (@{$attributes->{'attributes'}}) {
        my $length = length($attribute->{'name'});
        if ( defined($attribute->{'value'}) && !($attribute->{'value'} eq '') && $length > $max_length ) {
            $max_length = $length;
        }
    }
    if ( $attributes->{'single_line'} ) {
        $max_length = 0;
    }

    # format attributes to string
    my $string = '';
    my $format = sprintf("%%%ds", $max_length);
    $max_length += 3;
    foreach my $attribute (@{$attributes->{'attributes'}}) {
        my $value = $attribute->{'value'};
        if( ref($value) ) {
            $value = $value->to_string();
        }
        $value = text_indent_lines($value, $max_length, 0);
        if ( defined($value) && length($value) > 0 ) {
            if ( $attributes->{'single_line'} ) {
                if ( length($string) > 0 ) {
                    $string .= ", ";
                }
                $string .= colored(sprintf($format, lc($attribute->{'name'})), $COLOR) . ': ' . $value;
            } else {
                if ( length($string) > 0 ) {
                    $string .= "\n";
                }
                $string .= ' ' . colored(sprintf($format, $attribute->{'name'}), $COLOR) . ': ' . $value;
                if ( defined($attribute->{'description'}) ) {
                    $string .= sprintf("\n%s", text_indent_lines($attribute->{'description'}, $max_length));
                }
            }
        }
    }

    # format collections to string
    foreach my $collection (@{$attributes->{'collections'}}) {
        my $collection = $collection->{'to_string'}();
        if ( length($string) > 0 ) {
            $string .= "\n";
        }
        $collection = text_indent_lines($collection, 1);
        $string .= $collection;
    }

    my $prefix = '';
    if ( $attributes->{'single_line'} ) {
        # enclose in "(...)"
        $string = colored('(', $COLOR_HEADER) . $string . colored(')', $COLOR_HEADER);
    } else {
        # add "|" to the beginning of each line
        $prefix = colored('|', $COLOR_HEADER);
        $string =~ s/\n *$//g;
        $string =~ s/\n/\n$prefix/g;
    }
    if ( length($string) > 0 ) {
        # add "|" to the first line
        $string = $prefix . $string;
        # add ending newline
        $string .= "\n";
    }
    if ( !$attributes->{'single_line'} ) {
        # break attributes to the new line
        $string = "\n" . $string;
    }
    # prepend header
    $string = colored(uc($self->get_name()), $COLOR_HEADER) . $string;
    return $string;
}

#
# Convert object to string
#
# @return string describing this object
#
sub to_string_short
{
    my ($self) = @_;
    return $self->get_name();
}

1;