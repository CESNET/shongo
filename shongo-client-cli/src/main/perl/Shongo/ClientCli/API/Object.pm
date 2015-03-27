#
# Base API object.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Object;

use strict;
use warnings;
use warnings::register;

use Shongo::Common;
use Shongo::Console;
use Shongo::ClientCli;

our $COLOR_HEADER = "bold blue";
our $COLOR = "bold white";
our $COLLECTION_EMPTY = "-- None --";

#
# Mapping of API classes ('hash_class' => 'perl_class').
#
our $ClassMapping = {
    '^.*Reservation$' => 'Shongo::ClientCli::API::Reservation',
    '^.*Specification$' => 'Shongo::ClientCli::API::Specification',
    '^RoomAvailability$' => 'Shongo::ClientCli::API::RoomAvailability',
    '^RoomEstablishment$' => 'Shongo::ClientCli::API::RoomEstablishment',
    '^.*Participant$' => 'Shongo::ClientCli::API::Participant',
    '^.*Person$' => 'Shongo::ClientCli::API::Person',
    '^.*Executable$' => 'Shongo::ClientCli::API::Executable',
    '^ExecutableServiceSpecification$' => 'Shongo::ClientCli::API::ExecutableServiceSpecification',
    '^ValueProvider\.(Pattern|Filtered)$' => 'Shongo::ClientCli::API::ValueProvider'
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
    $self->{'__name'} = undef;
    $self->{'__attributes'} = {};
    $self->{'__attributes_order'} = [];
    $self->{'__attributes_preserve'} = {};
    $self->{'__attributes_filled'} = {};

    $self->add_attribute_preserve('id');

    return $self;
}

#
# Initialize this object (e.g., add attributes).
#
sub init()
{
    my ($self) = @_;
    if ( !defined($self->{'__initialized'}) ) {
        $self->{'__initialized'} = 1;
        $self->on_init();

        # Order columns
        @{$self->{'__attributes_order'}} = sort
            {$self->{'__attributes'}->{$a}->{'order'} <=> $self->{'__attributes'}->{$b}->{'order'}}
            @{$self->{'__attributes_order'}};
    }
}

#
# Event called when the object should be initialized (e.g., added attributes).
#
sub on_init()
{
    my ($self) = @_
}

#
# @return class of the object (API hash class)
#
sub get_object_class
{
    my ($self) = @_;
    return $self->{'__class'};
}

#
# Set API class of the object (API hash class).
#
# @param $name
#
sub set_object_class
{
    my ($self, $class) = @_;
    $self->{'__class'} = $class;
}

#
# @return name of the object
#
sub get_object_name
{
    my ($self) = @_;
    return $self->{'__name'};
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
# 'display-empty' => 1|0 (default 0)
#  Specifies whether empty values should be displayed.
#
# 'editable' => 1|0 (default 1)
#  Specifies whether the attribute can be edited.
#
# 'read-only' => 1|0 (default 0)
#  Specifies that attribute should not appended to hash/xml created from the object. If attribute is set as 'read-only' it
#  automatically forces 'editable' => 0.
#
# 'required' => 1|0 (default 0)
#  Specifies whether the attribute must be filled when it is edited.

# 'format' => Callback
#  Specifies the callback which replaces the default implementation of formatting attribute value to string.

# 'modify' => Callback
#  Specifies the callback which replaces the default implementation of attribute modification.
#
# 'type' => ... (default 'string')
#  Specifies the type of the attribute:
#   'int'               attribute value is integer
#   'bool'              attribute value is boolean (1|0)
#   'string'            attribute value is string
#   'enum'              attribute value is enumeration
#   'period'            attribute value is ISO8601 period
#   'datetime'          attribute value is ISO8601 date/time
#   'datetime-partial'  attribute value is partial ISO8601 date/time (components can be omitted from the end)
#   'class'             attribute value is complex object
#   'collection'        attribute value is collection of items
#   'map'               attribute value is map of item keys and values
#
# 'complex' => 1|0 (default 0)
#  Specifies whether attribute is complex and it should not be edited in "modify attributes" but it should be edited
#  as custom action in modify loop. ('type' => 'collection' or 'type' => 'map' is automatically 'complex')
#
# 'string-pattern' => String|Callback
#  When 'type' => 'string' then it specifies the regular expression which must the attribute value match.
#
# 'enum' => String
#  When 'type' => 'enum' then it specifies the enumeration values (hash of 'value' => 'title')
#
# 'class' => String
#  When 'type' => 'class' then it specifies the class which will be create/edited in the attribute value
#
# 'item' => String
#  When 'type' => 'collection' or 'type' => 'map' then it specifies additionally options:
#
#     'title' => String
#      It specifies the title of an item in collection.
#
#     'short' => 1|0 (default 0)
#      It specifies whether items should be rendered as short.
#
#     'add' => Callback
#     'modify' => Callback
#     'delete' => Callback
#      It specifies the callback for adding new items, modifying or deleting existing items.
#
#     'class' => String
#      Instead of specifying callbacks you can specify class which will be automatically
#      instanced in 'add' and which will be modified in 'modify' ('delete' will be
#      kept empty, the item is always automatically removed from the collection and marked as 'deleted').

#     'enum' => Hash
#      Instead of specifying callbacks you can specify hash of enumeration values which
#      will be used as values inside the collection and the callbacks are automatically added.
#
# @param $value value to be set to the attribute
#
sub add_attribute
{
    my ($self, $name, $attribute, $value) = @_;
    if ( !defined($attribute) ) {
        $attribute = {};
    }
    if ( !ref($attribute) ) {
        warnings::warn("Attribute definition should be hash reference.");
        return;
    }
    my $attribute_name = $name;
    $self->{'__attributes'}->{$attribute_name} = $attribute;
    push(@{$self->{'__attributes_order'}}, $attribute_name);
    set_default_value($attribute, 'order', 0);
    set_default_value($attribute, 'display', 'block');
    set_default_value($attribute, 'display-empty', 0);
    set_default_value($attribute, 'editable', 1);
    set_default_value($attribute, 'read-only', 0);
    set_default_value($attribute, 'required', 0);
    set_default_value($attribute, 'type', 'string');

    my $default_complex_value = 0;
    if ( $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map' ) {
        $default_complex_value = 1;
    }
    set_default_value($attribute, 'complex', $default_complex_value);

    if ( $attribute->{'read-only'} == 1) {
        $attribute->{'editable'} = 0;
    }

    # Initialize collection or map "item"
    if ( $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map' ) {
        if ( !defined($attribute->{'item'}) ) {
            $attribute->{'item'} = {};
        }
        set_default_value($attribute->{'item'}, 'short', 0);
    }

    # Generate "add" and/or "modify" callback(s) for "type" => "collection"
    if ( $attribute->{'type'} eq 'collection' ) {
        if ( defined($attribute->{'item'}->{'class'}) ) {
            # Generate callbacks for given class
            $attribute->{'item'}->{'add'} = sub {
                my $perlClass = get_perl_class($attribute->{'item'}->{'class'});
                my $className = $attribute->{'item'}->{'class'};
                $className =~ s/.+:://g;
                my $item = $perlClass->create({'class' => $className});
                return $item;
            };
            $attribute->{'item'}->{'modify'} = sub {
                my ($item) = @_;
                $item->modify();
                return $item;
            };
        }
        elsif ( defined($attribute->{'item'}->{'enum'}) ) {
            # Generate callbacks for given class
            $attribute->{'item'}->{'add'} = sub {
                my $available_values = [];
                my $attribute_value = $self->get($attribute_name);
                my %values_hash = ();
                if ( defined($attribute_value) ) {
                    %values_hash = map { $_ => 1 } @{$attribute_value};
                }
                my $count = 0;
                foreach my $key (ordered_hash_keys($attribute->{'item'}->{'enum'})) {
                    if ( !exists($values_hash{$key}) ) {
                        push(@{$available_values}, $key => $attribute->{'item'}->{'enum'}->{$key});
                        $count++;
                    }
                }
                if ( $count == 0 ) {
                    console_print_error("No available value.");
                    return undef;
                }
                return console_read_enum('Select', ordered_hash($available_values));
            };
        }
        if ( !defined($attribute->{'item'}->{'add'}) && !defined($attribute->{'item'}->{'modify'}) ) {
            $attribute->{'editable'} = 0;
        }
    }

    # Set default value for the attribute
    if ( defined($value) ) {
        $self->set($name, $value);
    }
}

#
# Add attribute which should not be displayed or edited but which should be loaded from hash and stored to xml.
#
# @param $attribute_name
#
sub add_attribute_preserve
{
    my ($self, $attribute_name) = @_;
    $self->{'__attributes_preserve'}->{$attribute_name} = 1;
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
    #if ($attribute_value) {
	#$attribute_value =~ s/\\n/\n/g;
    #}
    $self->{$attribute_name} = $attribute_value;
    $self->{'__attributes_filled'}->{$attribute_name} = 1;
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
    #if ($self->{$attribute_name}) {
    #    my $attribute_value = $self->{$attribute_name};
    #    $attribute_value =~ s/\n/\\n/g;
    #    return $attribute_value;
    #}
    return $self->{$attribute_name};
}

#
# Create an object from this instance
#
# @param $attributes hash containing attributes
# @param $options see modify_loop
# @static
#
sub create()
{
    my ($self, $attributes, $options) = @_;

    if ( !ref($self) ) {
        $self = $self->new();
    }
    my $on_create_result = $self->on_create($attributes);
    if ( defined($on_create_result) ) {
        # Result is new instance
        if ( ref($on_create_result) ) {
            $self = $on_create_result;
        }
        # Result is class
        else {
            $self->set_object_class($on_create_result);
            if ( defined($attributes) && defined($attributes->{'class'}) ) {
                $attributes->{'class'} = $on_create_result;
            }
        }
    }
    $self->init();

    if ( defined($attributes) ) {
        $self->from_hash($attributes, 1);
    }

    my $auto_confirm = Shongo::ClientCli->is_scripting();
    while ( $auto_confirm || $self->modify_loop(0, $options) ) {
        if ( defined($options->{'on_confirm'}) ) {
            my $result = $options->{'on_confirm'}($self);
            if ( defined($result) ) {
                return $result;
            }
        }
        else {
            return $self;
        }
        # Something failed and auto confirm mean that we should exit
        if ( $auto_confirm ) {
            return undef;
        }
    }
    return undef;
}

#
# Event call when the object is creating.
#
# @param $attributes hash containing attributes
# @return 1 of succeeds,
#         otherwise 0
#
sub on_create
{
    my ($self, $attributes) = @_;
    return undef;
}

#
# Modify this object.
#
# @param $attributes
# @param $options see modify_loop
#
sub modify
{
    my ($self, $attributes, $options) = @_;

    if ( defined($attributes) ) {
        $self->from_hash($attributes);
    }

    my $auto_confirm = Shongo::ClientCli->is_scripting();
    while ( $auto_confirm || $self->modify_loop(1, $options) ) {
        if ( defined($options->{'on_confirm'}) ) {
            my $result = $options->{'on_confirm'}($self);
            if ( defined($result) ) {
                return $result;
            }
        }
        else {
            return $self;
        }
        # Something failed and auto confirm mean that we should exit
        if ( $auto_confirm ) {
            return undef;
        }
    }
    return undef;
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
    return 1;
}

#
# @return 1 if modify loop with actions is needed (it happens when some attribute is 'complex' and/or 'collection' or 'map')
#         0 otherwise
#
sub is_modify_loop_needed
{
    my ($self) = @_;
    foreach my $attribute_name (keys %{$self->{'__attributes'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && ($attribute->{'complex'} == 1 || $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map') ) {
            return 1;
        }
    }
    return 0;
}

#
# Run modify loop.
#
# @param $is_editing specifies whether the loop is started for modification of object and not for it's creation
# @param $options
#
sub modify_loop()
{
    my ($self, $is_editing, $options) = @_;
    if ( !$self->is_modify_loop_needed() ) {
        $self->modify_attributes($is_editing);
        # Always confirm modifying
        return 1;
    }
    else {
        # modify required attributes
        foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
            my $attribute = $self->get_attribute($attribute_name);
            if ( $attribute->{'editable'} == 1 && $attribute->{'required'} == 1 && !defined($self->get($attribute_name)) ) {
                $self->modify_attribute($attribute_name, 0);
            }
        }
    }

    my $message = 'modification of ' . lc($self->get_object_name());
    if ( $is_editing == 0 ) {
        $message = 'creation of ' . lc($self->get_object_name());
    }

    return console_action_loop(
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
            if ( !defined($options->{'on_confirm'}) ) {
                push(@actions, (
                    'Finish ' . $message => sub {
                        return 1;
                    }
                ));
            }
            else {
                push(@actions, (
                    'Confirm ' . $message => sub {
                        return 1;
                    },
                    'Cancel ' . $message => sub {
                        return 0;
                    }
                ));
            }
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

    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 ) {
            if ( $attribute->{'complex'} == 1 ) {
                my $collection_title = lc($self->get_attribute_title($attribute_name));
                push(@{$actions}, (
                    'Modify ' . $collection_title => sub {
                        $self->modify_attribute($attribute_name);
                        return undef;
                    }
                ));
            }
            elsif ( $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map' ) {
                $self->modify_attribute_items_add_actions($attribute_name, $actions);
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
sub modify_attribute_items_add_actions
{
    my ($self, $attribute_name, $actions) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 || !($attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map') ) {
        warnings::warn("Collection '$attribute_name' is not editable or 'collection' or 'map' type.");
        return;
    }
    my $attribute_title = $self->get_attribute_title($attribute_name);
    my $item_title = 'Item';
    if ( defined($attribute->{'item'}->{'title'}) ) {
        $item_title = $attribute->{'item'}->{'title'};
    }
    $item_title = lc($item_title);
    # Push add action (if handler exists)
    if ( defined($attribute->{'item'}->{'add'}) ) {
        my $add_handlers = {};
        # Multiple add handlers
        if ( ref($attribute->{'item'}->{'add'}) eq 'HASH' ) {
            $add_handlers = $attribute->{'item'}->{'add'};
        }
        # Single add handler
        else {
            $add_handlers->{'Add new ' . $item_title} = $attribute->{'item'}->{'add'};
        }
        # Add all add handlers
        foreach my $title (keys %{$add_handlers}) {
            push(@{$actions}, $title => sub {
                my $handler = $add_handlers->{$title};
                # Add item to collection
                if ( $attribute->{'type'} eq 'collection' ) {
                    my $item = &$handler();
                    if ( defined($item) ) {
                        if ( !defined($self->{$attribute_name}) ) {
                            $self->{$attribute_name} = [];
                        }
                        push(@{$self->{$attribute_name}}, $item);
                    }
                }
                # Add item to map
                else {
                    my ($itemKey, $itemValue) = &$handler();
                    if ( defined($itemKey) ) {
                        if ( !defined($self->{$attribute_name}) ) {
                            $self->{$attribute_name} = {};
                        }
                        $self->{$attribute_name}->{$itemKey} = $itemValue;
                    }
                }
                return undef;
            });
        }
    }

    my $item_count = 0;
    if ( $attribute->{'type'} eq 'collection' ) {
        $item_count = get_collection_size($self->{$attribute_name});
    }
    elsif ( $attribute->{'type'} eq 'map' ) {
        $item_count = get_map_size($self->{$attribute_name});
    }
    if ( $item_count > 0 ) {
        # Push modify action (if handler exists)
        if ( defined($attribute->{'item'}->{'modify'}) ) {
            push(@{$actions}, 'Modify existing ' . $item_title => sub {
                my $index = console_read_choice("Type a number of " . $item_title, 0, $item_count);
                if ( defined($index) ) {
                    # Modify item in collection
                    if ( $attribute->{'type'} eq 'collection' ) {
                        my $item = $self->{$attribute_name}->[$index - 1];
                        $item = $attribute->{'item'}->{'modify'}($item);
                        $self->{$attribute_name}->[$index - 1] = $item;
                    }
                    # Modify item in map
                    else {
                        my $item_key = get_map_item_key($self->{$attribute_name}, $index - 1);
                        my $item_value = get_map_item_value($self->{$attribute_name}, $item_key);
                        $item_value = $attribute->{'item'}->{'modify'}($item_key, $item_value);
                        $self->{$attribute_name}->{$item_key} = $item_value;
                    }
                }
                return undef;
            });
        }
        # Push delete action (if handler add or modify exists)
        if ( defined($attribute->{'item'}->{'add'}) || defined($attribute->{'item'}->{'modify'}) ) {
            push(@{$actions}, 'Remove existing ' . $item_title => sub {
                my $index = console_read_choice("Type a number of " . $item_title, 0, $item_count);
                if ( defined($index) ) {
                    # Remove item from collection
                    if ( $attribute->{'type'} eq 'collection' ) {
                        my $item = $self->{$attribute_name}->[$index - 1];
                        if ( defined($attribute->{'item'}->{'delete'}) ) {
                            $attribute->{'item'}->{'delete'}($item);
                        }
                        splice(@{$self->{$attribute_name}}, $index - 1, 1);
                    }
                    # Remove item from map
                    else {
                        my $item_key = get_map_item_key($self->{$attribute_name}, $index - 1);
                        if ( defined($attribute->{'item'}->{'delete'}) ) {
                            $attribute->{'item'}->{'delete'}($item_key);
                        }
                        delete $self->{$attribute_name}->{$item_key};
                    }
                }
                return undef;
            });
        }
    }
}

#
# Modify interval value
#
# @param $interval
#
sub modify_interval
{
    my ($interval) = @_;
    my $start = undef;
    my $end = undef;
    my $duration = undef;
    if ( defined($interval) && $interval =~ m/(.*)\/(.*)/ ) {
        $start = $1;
        $end = $2;
        $duration = iso8601_period_format(interval_get_duration($1, $2));
    }
    my $start_new = console_edit_value("Type a start date/time", 1, $Shongo::Common::DateTimeOrInfinitePattern, $start);
    if ( $start_new ne '*' ) {
        $start_new = datetime_fill_timezone($start_new);
        my $duration_new = console_edit_value("Type a duration", 0, $Shongo::Common::PeriodPattern, $duration);
        if ( !defined($duration) || $duration_new ne $duration || $start_new ne $start ) {
            if ( !defined($duration_new) || $duration_new eq '' ) {
                $end = $start_new;
            }
            else {
                $end = datetime_add_duration($start_new, $duration_new);
            }
        }
    }
    $end = console_edit_value("Type a end date/time", 1, $Shongo::Common::DateTimeOrInfinitePattern, $end);
    if ( $end ne '*' ) {
        $end = datetime_fill_timezone($end);
    }
    return $start_new . '/' . $end;
}

#
# Modify attribute value
#
sub modify_attribute_value
{
    my ($self, $attribute_title, $attribute_value, $attribute, $is_editing) = @_;
    my $attribute_required = $attribute->{'required'};
    if ( $attribute->{'type'} eq 'int' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            '^\\d+$',
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'string' ) {
        my $string_pattern = $attribute->{'string-pattern'};
        if ( ref($string_pattern) eq 'CODE' ) {
            $string_pattern = &$string_pattern();
        }
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $string_pattern,
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'enum' ) {
        $attribute_value = console_auto_enum(
            $is_editing,
            $attribute_title,
            $attribute->{'enum'},
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'bool' ) {
        $attribute_value = console_edit_bool(
             $attribute_title,
             $attribute_required,
             $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'period' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::PeriodPattern,
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'datetime' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::DateTimePattern,
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'datetime-partial' ) {
        $attribute_value = console_auto_value(
            $is_editing,
            $attribute_title,
            $attribute_required,
            $Shongo::Common::DateTimePartialPattern,
            $attribute_value
        );
    }
    elsif ( $attribute->{'type'} eq 'interval' ) {
        $attribute_value = modify_interval($attribute_value);
    }
    elsif ( $attribute->{'type'} eq 'class' ) {
        if ( !defined($attribute_value) ) {
            $attribute_value = eval($attribute->{'class'} . '->create()');
        } else {
            $attribute_value->modify();
        }
    }
    return $attribute_value;
}

#
# Modify attribute value
#
# @param $attribute_name specifies which attribute
# @param $is_editing     specifies whether value should be edited or newly created
#
sub modify_attribute
{
    my ($self, $attribute_name, $is_editing) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    if ( $attribute->{'editable'} == 0 ) {
        warnings::warn("Attribute '$attribute_name' is not editable.");
        return;
    }

    my $attribute_value = $self->get($attribute_name);
    if ( defined($attribute->{'modify'}) ) {
        $attribute_value = $attribute->{'modify'}($attribute_value);
        $self->set($attribute_name, $attribute_value);
        return;
    }

    my $attribute_title = $self->get_attribute_title($attribute_name);

    if ( $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map' ) {
        my $collection_title = $self->get_attribute_title($attribute_name);
        console_action_loop(
            sub {
                my $string = $self->format_attribute_value($attribute_name);
                if ( !defined($string) ) {
                    $string = $COLLECTION_EMPTY;
                }
                $string = colored($collection_title, $COLOR) . ":\n" . $string;
                console_print_text($string);
            },
            sub {
                my @menu_actions = ();
                $self->modify_attribute_items_add_actions($attribute_name, \@menu_actions);
                push(@menu_actions, 'Finish modifying ' . lc($collection_title) => sub {
                    return 0;
                });
                return ordered_hash(@menu_actions);
            }
        );
        return undef;
    }
    else {
        $attribute_value = $self->modify_attribute_value($attribute_title, $attribute_value, $attribute, $is_editing);
        $self->set($attribute_name, $attribute_value);
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
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'editable'} == 1 && !($attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map') && $attribute->{'complex'} == 0) {
            $self->modify_attribute($attribute_name, $is_editing);
        }
    }
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
    # Format value as (key,value) pair
    if (ref($value) eq 'HASH' && defined($value->{'__key'}) ) {
        if ( defined($options->{'format'}) && ref($options->{'format'}) eq 'CODE' ) {
            return $options->{'format'}($value->{'__key'}, $value->{'__value'});
        }
        else {
            return $value->{'__key'} . ':' . $value->{'__value'};
        }
    }
    elsif ( defined($options->{'format'}) && ref($options->{'format'}) eq 'CODE' ) {
        return $options->{'format'}($value, $options);
    }
    elsif ( defined($options->{'enum'}) && defined($value) && defined($options->{'enum'}->{$value}) ) {
        return $options->{'enum'}->{$value};
    }
    if ( !defined($value) ) {
        $value = '';
    }
    elsif( ref($value) ) {
        my $items = undef;
        # get array items
        if( ref($value) eq 'ARRAY' ) {
            my ($item) = @_;
            $items = $value;
        }
        # get map items
        elsif( ref($value) eq 'HASH' ) {
            my $map_items = $value;
            $items = [];
            foreach my $itemKey (keys %{$map_items}) {
                my $itemValue = $map_items->{$itemKey};
                push(@{$items}, {'__key' => $itemKey, '__value' => $itemValue});
            }
        }
        if ( defined($items) ) {
            if ( @{$items} > 0 ) {
                $value = '';
                for ( my $index = 0; $index < scalar(@{$items}); $index++ ) {
                    my $item = @{$items}[$index];
                    $item = $self->format_value($item, $options->{'item'});
                    if ( $options->{'short'} ) {
                        if ( length($value) > 0 ) {
                            $value .= ", ";
                        }
                        $value .= sprintf("%s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
                    } else {
                        $item = text_indent_lines($item, 3, 0);
                        if ( length($value) > 0 ) {
                            $value .= "\n";
                        }
                        $value .= sprintf("%s %s", colored(sprintf("%d)", $index + 1), $COLOR), $item);
                    }
                }
            }
            else {
                $value = $COLLECTION_EMPTY;
            }
        }
        else {
            if ( $options->{'short'} ) {
                $value = $value->to_string_short();
            }
            else {
                if ( !(ref($value) eq 'HASH') && $value->can('to_string') ) {
                    $value = $value->to_string(1);
                }
                else {
                    var_dump($value);
                    warnings::warn("Previous value hasn't method to_string.");
                }
            }
        }
    }
    return $value;
}

#
# @param $attribute_name of attribute whose value should be formatted
# @param $single_line specifies whether attribute should be formatted to single line
# @return formatted attribute value
#
sub format_attribute_value
{
    my ($self, $attribute_name, $single_line, $sub_call) = @_;
    my $attribute = $self->get_attribute($attribute_name);
    my $options = {};
    if ( !defined($single_line) ) {
        $single_line = 0;
    }
    $options->{'short'} = $single_line;
    $options->{'sub-call'} = $sub_call;
    $options->{'format'} = $attribute->{'format'};

    my $attribute_value = $self->get($attribute_name);
    if ( $attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map' ) {
        $options->{'item'} = $attribute->{'item'};
        if ( !defined($attribute_value) ) {
            $attribute_value = [];
        }
        if ( $attribute->{'display-empty'} == 0 ) {
            # Check for none items
            my $item_count = 0;
            if ( $attribute->{'type'} eq 'collection' ) {
                $item_count = get_collection_size($self->{$attribute_name});
            }
            elsif ( $attribute->{'type'} eq 'map' ) {
                $item_count = get_map_size($self->{$attribute_name});
            }
            if ( $item_count == 0 ) {
                return undef;
            }
        }
    }
    elsif ( $attribute->{'type'} eq 'enum' && defined($attribute->{'enum'}) ) {
        if ( defined($attribute_value) ) {
            $attribute_value = $attribute->{'enum'}->{$attribute_value};
        }
    }
    elsif ( $attribute->{'type'} eq 'interval' ) {
        if ( defined($attribute_value) ) {
            $attribute_value = interval_format($attribute_value);
        }
    }
    elsif ( $attribute->{'type'} eq 'datetime' ) {
        if ( defined($attribute_value) ) {
            $attribute_value = datetime_format($attribute_value);
        }
    }
    elsif ( $attribute->{'type'} eq 'bool' ) {
        if ( defined($attribute_value) && $attribute_value == 1 ) {
            $attribute_value = 'yes';
        }
        else {
            $attribute_value = 'no';
        }
    }
    return $self->format_value($attribute_value, $options);
}

#
# Format attributes to string.
#
# @return formatted string of attributes
#
sub format_attributes
{
    my ($self, $single_line, $sub_call) = @_;

    # determine maximum attribute title length
    my $max_length = 0;
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
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
    if ( $single_line ) {
        $max_length = 0;
    }

    # format attributes to string
    my $string = '';
    my $format = sprintf("%%%ds", $max_length);
    $max_length += 3;
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        my $attribute_title = $self->get_attribute_title($attribute_name);
        my $attribute_value = $self->format_attribute_value($attribute_name, $single_line, $sub_call);

        if ( $attribute->{'display-empty'} == 1 || defined($attribute_value) && length($attribute_value) > 0 ) {
            if ( !defined($attribute_value) ) {
                $attribute_value = '';
            }
	    #$attribute_value =~ s/\\n/\n/g;
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
    my ($self, $sub_call) = @_;

    my $content = '';
    $content .= $self->format_attributes(0, $sub_call);

    my $name = $self->{'__name'};
    if ( !defined($name) ) {
        return $content;
    }

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
# Convert $value while converting to HASH
#
# @param $value
#
sub to_hash_value
{
    my ($self, $value, $options) = @_;
    if ( ref($value) eq 'HASH' ) {
        my $hash = {};
        foreach my $key (keys %{$value}) {
            if ( $key eq '__array' ) {
                next;
            }
            $hash->{$key} = $self->to_hash_value($value->{$key}, $options);
        }
        return $hash;
    }
    elsif ( ref($value) eq 'ARRAY' ) {
        my $array = [];
        foreach my $item ( @{$value} ) {
            push(@{$array}, $self->to_hash_value($item, $options));
        }
        return $array;
    }
    elsif ( ref($value) ) {
        return $value->to_hash($options);
    }
    elsif ( !defined($value) || $value eq NULL ) {
        return undef;
    }
    else {
        return $value;
    }
}

#
# Convert object to HASH
#
# @param $options hash of options:
#        'changes' => do not whole collection but only changes
#
sub to_hash()
{
    my ($self, $options) = @_;

    my %options_backup = ();
    if ( defined($options) ) {
        %options_backup = %{$options};
    }
    $options = {
        'changes' => 0
    };
    @{$options}{keys %options_backup} = values %options_backup;

    my $hash = {};
    if ( defined($self->{'__class'}) ) {
        $hash->{'class'} = $self->{'__class'};
    }
    foreach my $attribute_name (@{$self->{'__attributes_order'}}) {
        my $attribute = $self->get_attribute($attribute_name);
        if ( $attribute->{'read-only'} == 0 ) {
            if ( defined($self->{$attribute_name}) ) {
                my $attribute_value = $self->get($attribute_name);
                # Store attribute to xml
                $hash->{$attribute_name} = $self->to_hash_value($attribute_value, $options);
            }
            elsif ( defined($self->{'__attributes_filled'}->{$attribute_name}) ) {
                # Store null (represented as empty hash)
                $hash->{$attribute_name} = undef;
            }
        }
    }
    foreach my $attribute_name (keys %{$self->{'__attributes_preserve'}}) {
        if ( !defined($self->{$attribute_name}) ) {
            next;
        }
        $hash->{$attribute_name} = $self->to_hash_value($self->{$attribute_name}, $options);
    }
    return $hash;
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
        foreach my $key (keys %{$value}) {
            $hash->{$key} = $self->to_xml_value($value->{$key});
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
        return $value->to_xml();
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
    return $self->to_xml_value($self->to_hash({'changes' => 1}));
}

#
# @param $class
# @return perl class for given hash $class
#
sub get_perl_class
{
    my ($class) = @_;
    if ( $class =~ /^Shongo::ClientCli::API::/ ) {
        return $class;
    }
    foreach my $key (keys %{$ClassMapping}) {
        my $value = $ClassMapping->{$key};
        if ( $class =~ /$key/ ) {
            return $value;
        }
    }
    return 'Shongo::ClientCli::API::' . $class;
}

#
# Create new instance of value
#
# @param $class
# @param $attribute
#
sub create_instance
{
    my ($class, $attribute) = @_;
    my $perl_class = get_perl_class($class);
    my $instance = eval($perl_class . '->new()');
    if ( !defined($instance) && defined($attribute) && ($attribute->{'type'} eq 'collection' || $attribute->{'type'} eq 'map')
         && defined($attribute->{'item'}->{'class'}) )
    {
        $instance = eval($attribute->{'item'}->{'class'} . '->new()');
    }
    if ( defined($instance) && !defined($instance->get_object_class()) ) {
        $instance->set_object_class($class);
    }
    return $instance;
}

#
# Convert $value from xml
#
# @param $value
# @param $attribute_name
#
sub from_hash_value
{
    my ($self, $value, $attribute_name) = @_;

    if ( ref($value) eq 'HASH' ) {
        my $attribute = undef;
        if ( defined($attribute_name) && defined($self->{'__attributes'}->{$attribute_name}) ) {
            $attribute = $self->get_attribute($attribute_name);
        }
        my $class = undef;
        if ( exists $value->{'class'} ) {
           $class = $value->{'class'};
        }
        elsif ( defined($attribute) && exists $attribute->{'item'} && exists $attribute->{'item'}->{'class'} ) {
            $class = $attribute->{'item'}->{'class'};
        }
        if ( defined($class) ) {
            my $object = create_instance($class, $attribute);
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
            push(@{$array}, $self->from_hash_value($item, $attribute_name));
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
# @param $hash    to fill from
#
sub from_hash()
{
    my ($self, $hash) = @_;

    # Get hash from xml
    if ( ref($hash) eq "RPC::XML::struct" ) {
        $hash = $hash->value();
    }

    if ( !ref($self) ) {
        if ( exists $hash->{'class'} ) {
            my $instance = create_instance($hash->{'class'});
            if ( defined($instance) ) {
                $self = $instance;
            }
        }
        if ( !ref($self) ) {
            $self = $self->new();
        }
    }
    if ( !defined($self) ) {
        var_dump($hash);
        die("Cannot convert printed hash to Object.");
    }

    # initialize object
    if ( exists $hash->{'class'} ) {
        $self->set_object_class($hash->{'class'});
    }
    $self->init();

    # Convert hash to object
    foreach my $attribute_name (keys %{$hash}) {
        my $attribute_value = $hash->{$attribute_name};
        $attribute_value = $self->from_hash_value($attribute_value, $attribute_name);
        if ( $self->has_attribute($attribute_name) ) {
            $self->set($attribute_name, $attribute_value);
        }
        elsif ( defined($self->{'__attributes_preserve'}) ) {
            if ( defined($attribute_value) ) {
                $self->{$attribute_name} = $attribute_value;
            }
        }
    }
    return $self;
}





sub test
{
    my $object = Shongo::ClientCli::API::Object->new();

    # Init class
    $object->set_object_class('TestClass');
    $object->set_object_name('Test');
    $object->add_attribute(
        'id', {
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
            'item' => {
                'class' => 'Shongo::ClientCli::API::Alias',
                'short' => 1
            },
            'required' => 1
        }
    );

    # Init instance
    $object->create({
        'id' => '1',
        'name' => 'Test 1',
        #'items' => ['Item 1', 'Item 2', 'Item 3']
    }, {'child' => 1});
}

1;
