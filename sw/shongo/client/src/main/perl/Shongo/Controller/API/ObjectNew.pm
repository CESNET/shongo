#
# Base API object
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ObjectNew;

use strict;
use warnings;
use warnings::register;

use Shongo::Common;
use Shongo::Console;

our $COLOR_HEADER = "bold blue";
our $COLOR = "bold white";

#
# Mapping of API classes ('hash_class' => 'perl_class').
#
our $ClassMapping = {
};

#
# Create a new instance of object.
#
# @static
#
sub new
{
    my $class = shift;
    my $self = {};
    bless $self, $class;

    $self->{'__class'} = undef;
    $self->{'__name'} = 'Object';
    $self->{'__attributes'} = {};

    return $self;
}

#
# Set API class of the object.
#
# @param $name
#
sub set_object_class
{
    my ($self, $class) = @_;
    $self->{'__class'} = $class;
}

#
# Set name of the object for printing (e.g. "Resource" or "Person").
#
# @param $name
#
sub set_object_name
{
    my ($self, $name) = @_;
    $self->{'__name'} = $name;
}

#
# Set default value to data if not set.
#
# @param $data
# @param $property
# @param $default_value
# @static
#
sub set_default_value
{
    my ($data, $property, $default_value) = @_;
    if ( !defined($data->{$property}) ) {
        $data->{$property} = $default_value;
    }
}

#
# Add attribute definition to this object.
#
# @param $attribute_name name of the attribute
# @param $attribute hash of following values:
#
# 'title' => String
#  Title used for printing and editing the attribute value.
#
# 'display' => 'block'|'newline' (default 'block')
#  Specifies whether title and value should be rendered into 'block' (title on the left and value on the right) or
#  whether title should be placed in the previous line and the value on the 'newline'.
#
# 'editable' => 1|0 (default 1)
#  Specifies whether the attribute can be edited.
#
# 'required' => 1|0 (default 0)
#  Specifies whether the attribute must be filled when it is edited.
#
# 'type' => ... (default 'string')
#  Specifies the type of the attribute:
#   'int'        attribute value is integer
#   'string'     attribute value is string
#   'collection' attribute value is collection of items
#
# 'string-pattern' => String
#  When 'type' => 'string' then it specifies the regular expression which must the attribute value match.
#
# 'collection-menu' => 1|0 (default 0)
#  When 'type' => 'collection' then it specifies whether add, modify and delete actions should be added to sub menu
# (default value 0 means that actions are added to main action menu).
#
# 'collection-title' => String
#  When 'type' => 'collection' then it specifies the title of an item in collection.
#
# 'collection-add' => Callback
# 'collection-modify' => Callback
# 'collection-delete' => Callback
#  When 'type' => 'collection' then it specifies the callback for adding new items, modifying or deleting existing items.
#
# 'collection-class' => String
#  When 'type' => 'collection' then instead of specifying callbacks you can specify class which will be automatically
#  instanced in 'collection-add' and which will be modified in 'collection-modify' ('collection-delete' will be
#  kept empty, the item is always automatically removed from the collection and marked as 'deleted').
#
sub add_attribute
{
    my ($self, $name, $attribute) = @_;
    if ( !ref($attribute) ) {
        warnings::warn("Attribute definition should be hash reference.");
        return;
    }
    my $attribute_name = $name;
    $self->{'__attributes'}->{$attribute_name} = $attribute;
    set_default_value($attribute, 'display', 'block');
    set_default_value($attribute, 'editable', 1);
    set_default_value($attribute, 'required', 0);
    set_default_value($attribute, 'type', 'string');
    set_default_value($attribute, 'collection-menu', 0);

    if ( $attribute->{'collection-class'} ) {
        # Generate callbacks for given class
        $attribute->{'collection-add'} = sub {
            my $item = eval($attribute->{'collection-class'} . '->new()');
            $item->create();
            return $item;
        };
        $attribute->{'collection-modify'} = sub {
            my ($item) = @_;
            $item->modify();
            return $item;
        };
    }
}

#
# @param $attribute_name
# @return 1 if attribute with given $attribute_name exists,
#         0 otherwise
#
sub has_attribute
{
    my ($self, $attribute_name) = @_;
    if ( !defined($self->{'__attributes'}->{$attribute_name}) ) {
        return 0;
    }
    return 1;
}

#
# @param $attribute_name
# @return attribute with given $attribute_name
#
sub get_attribute
{
    my ($self, $attribute_name) = @_;
    if ( !defined($self->{'__attributes'}->{$attribute_name}) ) {
        warnings::warn("Attribute '$attribute_name' not defined in '$self->{'__class'}'.");
        return undef;
    }
    return $self->{'__attributes'}->{$attribute_name};
}

#
# @param $attribute_name
# @return title for attribute with given $attribute_name
#
sub get_attribute_title
{
    my ($self, $attribute_name) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( defined($attribute) && defined($attribute->{'title'}) ) {
        return $attribute->{title};
    }
    return ucfirst(lc($attribute_name));
}

#
# Set attribute value.
#
# @param $attribute_name  name of attribute which value should be set
# @param $attribute_value new value for the attribute
#
sub set
{
    my ($self, $attribute_name, $attribute_value) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( !defined($attribute) ) {
        return;
    }
    $self->{$attribute_name} = $attribute_value;
}

#
# Get attribute value.
#
# @param $attribute_name name of the attribute
# @return value of attribute with given $attribute_name
#
sub get
{
    my ($self, $attribute_name) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( !defined($attribute) ) {
        return undef;
    }
    return $self->{$attribute_name};
}

#
# Format value to string.
#
# @param $value   value which should be formatted
# @param $options options
# @return formatted value to string
#
sub format_value
{
    my ($self, $value, $options) = @_;
    if ( !defined($value) ) {
        $value = '';
    }
    elsif( ref($value) ) {
        my $items = undef;
        if( ref($value) eq 'ARRAY' ) {
            my ($item) = @_;
            $items = get_collection_items($value);
        }
        elsif( ref($value) eq 'HASH' && (defined($value->{'modified'}) || defined($value->{'new'}) || defined($value->{'deleted'})) ) {
            $items = get_collection_items($value);
        }
        if ( defined($items) ) {
            if ( @{$items} > 0 ) {
                $value = '';
                for ( my $index = 0; $index < scalar(@{$items}); $index++ ) {
                    my $item = @{$items}[$index];
                    if ( defined($options->{'format_callback'}) ) {
                        $item = $options->{'format_callback'}($item);
                    }
                    $item = $self->format_value($item);
                    $item = text_indent_lines($item, 4, 0);
                    if ( $options->{'single_line'} ) {
                        if ( length($value) > 0 ) {
                            $value .= ", ";
                        }
                        $value .= sprintf("%s", $item);
                    } else {
                        if ( length($value) > 0 ) {
                            $value .= "\n";
                        }
                        $value .= sprintf("%s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
                    }
                }
            }
            else {
                $value = "-- None --";
            }
        }
        else {
            $value = $value->to_string();
        }
    }
    return $value;
}

#
# @param $attribute_name of attribute whose value should be formatted
# @return formatted attribute value
#
sub format_attribute_value
{
    my ($self, $attribute_name, $single_line) = @_;
    if ( !defined($single_line) ) {
        $single_line = 0;
    }
    my $attribute = $self->get_attribute($attribute_name);
    my $attribute_value = $self->get($attribute_name);
    if ( $attribute->{'type'} eq 'collection' && !defined($attribute_value) ) {
        $attribute_value = [];
    }
    return $self->format_value($attribute_value, {
        'single_line' => $single_line,
        'format_callback' => $attribute->{'format_callback'}
    });
}

#
# Format attributes to string.
#
# @return formatted string of attributes
#
sub format_attributes
{
    my ($self, $single_line) = @_;

    # determine maximum attribute title length
    my $max_length = 0;
    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute = $self->get_attribute($attribute_name);
        # max length of title is determined only from 'display' => 'block'
        if ( $attribute->{'display'} eq 'block' ) {
            my $attribute_title = $self->get_attribute_title($attribute_name);
            my $attribute_value = $self->get($attribute_name);
            my $attribute_title_length = length($attribute_title);
            if ( defined($attribute_value) && !($attribute_value eq '') && $attribute_title_length > $max_length ) {
                $max_length = $attribute_title_length;
            }
        }
    }

    # format attributes to string
    my $string = '';
    my $format = sprintf("%%%ds", $max_length);
    $max_length += 3;
    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute = $self->get_attribute($attribute_name);
        my $attribute_title = $self->get_attribute_title($attribute_name);
        my $attribute_value = $self->format_attribute_value($attribute_name, $single_line);
        if ( defined($attribute_value) && length($attribute_value) > 0 ) {
            if ( $single_line ) {
                if ( length($string) > 0 ) {
                    $string .= ", ";
                }
                $string .= colored(sprintf($format, lc($attribute_title)), $COLOR) . ': ' . $attribute_value;
            } else {
                if ( length($string) > 0 ) {
                    $string .= "\n";
                }
                if ( $attribute->{'display'} eq 'block' ) {
                    $attribute_value = text_indent_lines($attribute_value, $max_length, 0);
                    $string .= ' ' . colored(sprintf($format, $attribute_title), $COLOR) . ': ' . $attribute_value;
                    if ( defined($attribute->{'description'}) ) {
                        $string .= sprintf("\n%s", text_indent_lines($attribute->{'description'}, $max_length));
                    }
                }
                else {
                    $attribute_value = text_indent_lines($attribute_value, 2, 1);
                    $string .= ' ' . colored($attribute_title, $COLOR) . ":\n";
                    $string .= $attribute_value;
                    if ( defined($attribute->{'description'}) ) {
                        $string .= sprintf("\n%s", $attribute->{'description'});
                    }
                }
            }
        }
    }

    return $string;
}

#
# Convert object to string (could be multi-line).
#
# @return string describing this object
#
sub to_string
{
    my ($self) = @_;

    my $content = '';
    $content .= $self->format_attributes(0);
    # add "|" to the beginning of each line
    my $prefix = colored('|', $COLOR_HEADER);
    $content =~ s/\n *$//g;
    $content =~ s/\n/\n$prefix/g;
    if ( length($content) > 0 ) {
        # add "|" to the first line
        $content = $prefix . $content;
        # add ending newline
        $content .= "\n";
    }

    my $string = '';
    $string .= colored(uc($self->{'__name'}), $COLOR_HEADER) . "\n";
    $string .= $content;
    return $string;
}

#
# Convert object to string (should be single-line).
#
# @return string describing this object
#
sub to_string_short
{
    my ($self) = @_;
    my $string = '';
    $string .= colored(uc($self->{'__name'}), $COLOR_HEADER);
    $string .= colored('(', $COLOR_HEADER);
    $string .= $self->format_attributes(1);
    $string .= colored(')', $COLOR_HEADER);
    return $string;
}

#
# Create an object from this instance
#
# @param $attributes hash containing attributes
#
sub create()
{
    my ($self, $attributes) = @_;

    if ( defined($attributes) && ref($attributes) eq 'HASH' ) {
        $self->from_hash($attributes);
    }

    # modify required attributes
    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'required'} == 1 && !defined($self->get($attribute_name)) ) {
            $self->modify_attribute($attribute_name, 0);
        }
    }

    while ( $self->modify_loop('creation of ' . lc($self->{'__name'})) ) {
        my $result = $self->on_create_confirm();
        if ( defined($result) ) {
            return $result;
        }
    }
    return undef;
}

#
# Event called when the creation is confirmed.
#
# @return identifier of the object if the creation succeeds,
#         undef otherwise
#
sub on_create_confirm
{
    my ($self) = @_;
    console_print_info("Creating " . lc($self->{'__name'}) . "...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Resource.createResource',
        $self->to_xml()
    );
    if ( $response->is_fault() ) {
        return undef;
    }
    return $response->value();
}

#
# Modify this object.
#
sub modify
{
    my ($self) = @_;
    while ( $self->modify_loop('modification of ' . lc($self->{'__name'})) ) {
        if ( on_modify_confirm() ) {
            return;
        }
    }
}

#
# Event called when the modification is confirmed.
#
# @return 1 when the modification succeeds,
#         0 otherwise
#
sub on_modify_confirm
{
    my ($self) = @_;
    console_print_info("Modifying " . lc($self->{'__name'}) . "...");
    my $response = Shongo::Controller->instance()->secure_request(
        'Resource.modifyResource',
        $self->to_xml()
    );
    if ( $response->is_fault() ) {
        return 0;
    }
    return 1;
}

#
# Run modify loop.
#
# @param $message
#
sub modify_loop()
{
    my ($self, $message) = @_;
    console_action_loop(
        sub {
            console_print_text($self);
        },
        sub {
            my @actions = (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                }
            );
            $self->on_modify_loop(\@actions);
            push(@actions, (
                'Confirm ' . $message => sub {
                    return 1;
                },
                'Cancel ' . $message => sub {
                    return 0;
                }
            ));
            return ordered_hash(@actions);
        }
    );
}

#
# On modify loop.
#
# @param $actions array of actions
#
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && $attribute->{'type'} eq 'collection' ) {
            # Collection by sub menu
            if ( $attribute->{'collection-menu'} == 1 ) {
                my $collection_title = $self->get_attribute_title($attribute_name);
                push(@{$actions}, (
                    'Modify ' . lc($collection_title) => sub {
                        $self->modify_attribute($attribute_name);
                    }
                ));
            }
            # Collection into main menu
            else {
                $self->modify_collection_add_actions($attribute_name, $actions);
            }
        }
    }
}

#
# Add actions for modifying collection to given $actions.
#
# @param $attribute_name collection name
# @param $actions where the actions should be added
#
sub modify_collection_add_actions
{
    my ($self, $attribute_name, $actions) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 || !($attribute->{'type'} eq 'collection') ) {
        warnings::warn("Collection '$attribute_name' is not editable or 'collection' type.");
        return;
    }
    my $item_title = 'Item';
    if ( defined($attribute->{'collection-title'}) ) {
        $item_title = $attribute->{'collection-title'};
    }
    $item_title = lc($item_title);
    push(@{$actions}, 'Add new ' . $item_title => sub {
        my $item = undef;
        if ( defined($attribute->{'collection-add'}) ) {
            $item = $attribute->{'collection-add'}();
        }
        if ( defined($item) ) {
            if ( !defined($self->{$attribute_name}) ) {
                $self->{$attribute_name} = [];
            }
            add_collection_item(\$self->{$attribute_name}, $item);
        }
        return undef;
    });
    my $collection_size = get_collection_size($self->{$attribute_name});
    if ( $collection_size > 0 ) {
        push(@{$actions}, 'Modify existing ' . $item_title => sub {
            my $index = console_read_choice("Type a number of " . $item_title, 0, $collection_size);
            if ( defined($index) ) {
                my $item = get_collection_item($self->{$attribute_name}, $index - 1);
                if ( defined($attribute->{'collection-modify'}) ) {
                    $item = $attribute->{'collection-modify'}($item);
                }
            }
            return undef;
        });
        push(@{$actions}, 'Remove existing ' . $item_title => sub {
            my $index = console_read_choice("Type a number of " . $item_title, 0, $collection_size);
            if ( defined($index) ) {
                my $item = get_collection_item($self->{$attribute_name}, $index - 1);
                if ( defined($attribute->{'collection-delete'}) ) {
                    $item = $attribute->{'collection-delete'}($item);
                }
                remove_collection_item(\$self->{$attribute_name}, $index - 1);
            }
            return undef;
        });
    }
}

sub modify_attribute
{
    my ($self, $attribute_name, $is_editing) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 ) {
        warnings::warn("Attribute '$attribute_name' is not editable.");
        return;
    }

    my $attribute_value = $self->get($attribute_name);
    my $attribute_title = $self->get_attribute_title($attribute_name);
    my $attribute_required = $attribute->{'required'};
    if ( $attribute->{'type'} eq 'int' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            '^\\d+$',
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value)
    }
    elsif ( $attribute->{'type'} eq 'string' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $attribute->{'string-pattern'},
            $attribute_value
        );
        $self->set($attribute_name, $attribute_value);
    }
    elsif ( $attribute->{'type'} eq 'collection' ) {
        my $collection_title = $self->get_attribute_title($attribute_name);
        console_action_loop(
            sub {
                my $string = colored($collection_title, $COLOR) . ":\n";
                $string .= $self->format_attribute_value($attribute_name);
                console_print_text($string);
            },
            sub {
                my @menu_actions = ();
                $self->modify_collection_add_actions($attribute_name, \@menu_actions);
                push(@menu_actions, 'Finish modifying ' . lc($collection_title) => sub {
                    return 0;
                });
                return ordered_hash(@menu_actions);
            }
        );
        return undef;
    }
}

#
# Modify attributes
#
# @param $edit
#
sub modify_attributes
{
    my ($self, $is_editing) = @_;
    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && !($attribute->{'type'} eq 'collection') ) {
            $self->modify_attribute($attribute_name, $is_editing);
        }
    }
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
        return $value->to_hash($value);
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
    if ( defined($self->{'__class'}) ) {
        $xml->{'class'} = RPC::XML::string->new($self->{'__class'});
    }
    while ( my ($attribute_name, $attribute) = each(%{$self->{'__attributes'}}) ) {
        my $attribute_value = $self->get($attribute_name);
        $xml->{$attribute_name} = $self->to_xml_value($attribute_value);
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
sub from_hash_value
{
    my ($self, $value, $attribute) = @_;

    if ( ref($value) eq 'HASH' ) {
        if ( exists $value->{'class'} ) {
            my $object = $self->create_value_instance($value->{'class'}, $attribute);
            if ( defined($object) ) {
                $object->from_hash($value);
                return $object;
            }
        }
        my $hash = {};
        foreach my $item_name (keys %{$value}) {
            my $item_value = $value->{$item_name};
            $hash->{$item_name} = $self->from_hash_value($item_value, $item_name);
        }
        return $hash;
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->from_hash_value($item, $attribute));
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
sub from_hash()
{
    my ($self, $hash) = @_;

    # Get hash from xml
    if ( ref($hash) eq "RPC::XML::struct" ) {
        $hash = $hash->value();
    }

    if ( !ref($self) ) {
        $self = undef;
        if ( exists $hash->{'class'} ) {
            my $class = $hash->{'class'};
            if ( defined($ClassMapping->{$class}) ) {
                $class = $ClassMapping->{$class};
            }
            else {
                $class = 'Shongo::Controller::API::' . $class;
            }
            $self = eval($class . '->new()');
            if (!defined($self)) {
                die("Cannot instantiate class '" . $class . "'.");
            }
        }
    }
    if ( !defined($self) ) {
        var_dump($hash);
        die("Cannot convert printed hash to Object.");
    }

    # Convert hash to object
    foreach my $attribute_name (keys %{$hash}) {
        my $attribute_value = $hash->{$attribute_name};
        $attribute_value = $self->from_hash_value($attribute_value, $attribute_name);
        if ( $self->has_attribute($attribute_name) ) {
            $self->set($attribute_name, $attribute_value);
        }
    }
    return $self;
}





sub test
{
    my $object = Shongo::Controller::API::ObjectNew->new();

    # Init class
    $object->set_object_class('TestClass');
    $object->set_object_name('Test');
    $object->add_attribute(
        'identifier', {
            'required' => 1,
            'type' => 'int'
        }
    );
    $object->add_attribute(
        'name', {
            'required' => 1,
            'title' => 'Testing Name',
        }
    );
    $object->add_attribute(
        'items', {
            'type' => 'collection',
            'collection-class' => 'Shongo::Controller::API::Alias',
            'required' => 1
        }
    );

    # Init instance
    #$object->set('identifier', '1');
    #$object->set('name', 'Test 1');
    #$object->set('items', ['Item 1', 'Item 2', 'Item 3']);
    $object->create({
        'identifier' => '1',
        #'name' => 'Test 1',
        #'items' => ['Item 1', 'Item 2', 'Item 3']
    });
}

1;