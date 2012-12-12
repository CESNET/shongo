#
# Common functions.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Common;

use strict;
use warnings;
use warnings::register;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    ordered_hash ordered_hash_keys ordered_hash_merge
    array_value_exists
    get_enum_value
    get_collection_size get_collection_items get_collection_item set_collection_item add_collection_item remove_collection_item
    get_map_size get_map_items get_map_item_key get_map_item_value set_map_item add_map_item remove_map_item
    format_datetime format_date format_period format_datetime_partial format_interval format_report
    var_dump
    get_home_directory get_term_width
    text_indent_lines
    colored trim
    history_load history_save history_get_group_from history_set_group_to
    NULL
);

use DateTime::Format::ISO8601;
use File::HomeDir;
use Term::ANSIColor;
use JSON -support_by_pp;

# Regular Expression Patterns
our $IdPattern = '(^\\d|shongo:.+:\\d$)';
our $DateTimePattern = '(^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d(:\\d\\d(.\\d+)?([\\+-]\\d\\d:\\d\\d)?)?$)';
our $PeriodPattern = '(^P(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+S)?)?$)';
our $DateTimePartialPattern = '(^\\d\\d\\d\\d(-\\d\\d)?(-\\d\\d)?(T\\d\\d(:\\d\\d)?)?$)';

# Represents a "null" constant
use constant NULL => '__null__';

#
# Checks whether $value exists in the @array
#
# @param $value
# @param @array
#
sub array_value_exists
{
    my ($value, @array) = @_;
    return grep(/^$value$/, @array);
}

#
# Removes $value from the $array
#
# @param $value
# @param $array
#
sub array_remove_value
{
    my ($value, $array) = @_;
    @{$array} = grep { $_ ne $value } @{$array};
}

#
# Create hash from given values which has item "__keys" as array with keys in insertion order.
#
# @param values array of pair of items (even count)
# @return created has
#
sub ordered_hash
{
    my (@values) = @_;
    if ( ref($_[0]) ) {
        @values = @{$_[0]};
    }
    my %hash = ();
    my @order = ();

    # replace ordered hash items as theirs arrays
    for ( my $index = 0; $index < @values; $index++ ) {
        my $value= $values[$index];
        if ( ref($value) eq 'HASH' && exists $value->{'__keys'} ) {
            splice(@values, $index, 1);
            foreach my $key (@{$value->{'__keys'}}) {
                splice(@values, $index, 0, ($key, $value->{$key}));
                $index += 2;
            }
        }
    }

    for ( my $index = 0; $index < (@values - 1); $index += 2 ) {
        my $key = $values[$index];
        my $value = $values[$index + 1];
        $hash{$key} = $value;
        push(@order, $key);
    }

    ${hash{'__keys'}} = [@order];

    return \%hash;
}

#
# Get array of keys from hash or ordered hash
#
# @return array of keys
#
sub ordered_hash_keys
{
    my ($hash) = @_;

    my @hash_keys;
    if ( defined($hash->{'__keys'}) ) {
        @hash_keys = @{$hash->{'__keys'}};
    }
    else {
        @hash_keys = keys %{$hash};
    }
    return @hash_keys;
}

#
# Merge two ordered hash
#
# @param hash1
# @param hash2
# @return merged hash
#
sub ordered_hash_merge
{
    my ($hash1, $hash2) = @_;
    my @data = ();
    foreach my $key (ordered_hash_keys($hash1)) {
        push(@data, $key, $hash1->{$key});
    }
    foreach my $key (ordered_hash_keys($hash2)) {
        push(@data, $key, $hash2->{$key});
    }
    return ordered_hash(@data);
}

#
# @param $enum
# @param $value
# @return enum value
#
sub get_enum_value
{
    my ($enum, $value) = @_;
    if ( !defined($value) ) {
        $value = NULL;
    }
    return $enum->{$value};
}

#
# Get number of items in collection. Collection can be stored as hash('new' => [], 'modified' => [], 'deleted' => []).
#
# @param $collection  collection reference
# @return collection size
#
sub get_collection_size
{
    my ($collection) = @_;
    my $items = get_collection_items($collection);
    return scalar(@{$items});
}

#
# Get collection items as array. Collection can be stored as hash('new' => [], 'modified' => [], 'deleted' => []).
#
# @param $collection  collection reference
# @return array of items
#
sub get_collection_items
{
    my ($collection) = @_;
    if ( ref($collection) eq 'ARRAY' ) {
        return $collection;
    }
    elsif ( ref($collection) eq 'HASH' ) {
        my $array = [];
        if ( defined($collection->{'__array'}) ) {
            push(@{$array}, @{$collection->{'__array'}});
        }
        if ( defined($collection->{'modified'}) ) {
            push(@{$array}, @{$collection->{'modified'}});
        }
        if ( defined($collection->{'new'}) ) {
            push(@{$array}, @{$collection->{'new'}});
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
# @param $collection  collection reference
# @param $item_index  item index
#
sub get_collection_item
{
    my ($collection, $item_index) = @_;
    my $items = get_collection_items($collection);
    return $items->[$item_index];
}

#
# Set item item collection by index
#
# @param $collection  reference to collection reference
# @param $item_index  item index
# @param $item  item
#
sub set_collection_item
{
    my ($collection, $item_index, $item) = @_;
    convert_collection_to_hash($collection);

    if ( $item_index < scalar(@{${$collection}->{'__array'}}) ) {
        splice(@{${$collection}->{'__array'}}, $item_index, 1);
        if ( !defined(${$collection}->{'modified'}) ) {
            ${$collection}->{'modified'} = [];
        }
        push(@{${$collection}->{'modified'}}, $item);
    }
    else {
        $item_index -= scalar(@{${$collection}->{'__array'}});
        if ( $item_index < scalar(@{${$collection}->{'modified'}}) ) {
            @{${$collection}->{'modified'}}[$item_index] = $item;
        }
        else {
            $item_index -= scalar(@{${$collection}->{'modified'}});
            @{${$collection}->{'new'}}[$item_index] = $item;
        }
    }
}

#
# Convert collection to hash
#
# @param \$collection  reference to collection reference
# @param $is_new       specifies whether collection items should be marked as new
#
sub convert_collection_to_hash
{
    my ($collection, $is_new) = @_;
    if ( !defined($is_new) ) {
        $is_new = 0;
    }
    if ( ref(${$collection}) eq 'HASH' ) {
        # Do nothing
        if ( $is_new ) {
            die("Collection already contains changes so the items can't be marked as new.");
        }
    }
    elsif ( ref(${$collection}) eq 'ARRAY' ) {
        if ( $is_new ) {
            ${$collection} = {'new' => ${$collection}};
        }
        else {
            ${$collection} = {'__array' => ${$collection}};
        }
    }
    else {
        ${$collection} = {};
    };
}

#
# Add item to collection
#
# @param \$collection  reference to collection reference
# @param $item         new item
#
sub add_collection_item
{
    my ($collection, $item) = @_;
    convert_collection_to_hash($collection);
    if ( !defined(${$collection}->{'new'}) ) {
        ${$collection}->{'new'} = [];
    }
    push(@{${$collection}->{'new'}}, $item);
}

#
# Remove item from collection
#
# @param \$collection  reference to collection reference
# @param $item_index   existing item index
#
sub remove_collection_item
{
    my ($collection, $item_index) = @_;
    convert_collection_to_hash($collection);
    my $item = undef;
    # Remove item from original array
    if ( defined(${$collection}->{'__array'}) ) {
        my $array_count = scalar(@{${$collection}->{'__array'}});
        if ( $item_index < $array_count ) {
            $item = ${$collection}->{'__array'}[$item_index];
            splice(@{${$collection}->{'__array'}}, $item_index, 1);
            $item_index = undef;
        } else {
            $item_index -= $array_count;
        }
    }
    # Remove item from modified array
    if ( defined($item_index) && defined(${$collection}->{'modified'}) ) {
        my $modified_count = scalar(@{${$collection}->{'modified'}});
        if ( $item_index < $modified_count ) {
            $item = ${$collection}->{'modified'}[$item_index];
            splice(@{${$collection}->{'modified'}}, $item_index, 1);
            $item_index = undef;
        } else {
            $item_index -= $modified_count;
        }
    }
    # Remove item from new array
    if ( defined($item_index) && defined(${$collection}->{'new'}) ) {
        my $new_count = scalar(@{${$collection}->{'new'}});
        if ( $item_index < $new_count ) {
            splice(@{${$collection}->{'new'}}, $item_index, 1);
            return;
        }
    }
    # If item was original or modified, add it to deleted
    if ( defined($item) ) {
        if ( !defined(${$collection}->{'deleted'}) ) {
            ${$collection}->{'deleted'} = [];
        }
        push(@{${$collection}->{'deleted'}}, $item);
        return;
    }
    console_print_error("Cannot delete item with index '%d' in collection '%s'.", $item_index, $collection);
}

#
# Get number of items in map.
#
# @param $map  map reference
# @return map size
#
sub get_map_size
{
    my ($map) = @_;
    my $items = get_map_items($map);
    return scalar(keys %{$items});
}

#
# Get map items as hash.
#
# @param $map  map reference
# @return hash of items
#
sub get_map_items
{
    my ($map) = @_;
    if ( ref($map) eq 'HASH' ) {
        if ( defined($map->{'__map'}) ) {
            return $map->{'__map'};
        }
        else {
            return $map;
        }
    }
    else {
        return {};
    }
}

#
# Get item key from map by index
#
# @param $map         map reference
# @param $item_index  item index
#
sub get_map_item_key
{
    my ($map, $item_index) = @_;
    my $items = get_map_items($map);
    my @item_keys = keys %{$items};
    return $item_keys[$item_index];
}

#
# Get item value from map by key
#
# @param $map       map reference
# @param $item_key  item key
#
sub get_map_item_value
{
    my ($map, $item_key) = @_;
    my $items = get_map_items($map);
    return $items->{$item_key};
}

#
# Convert map to hash with changes.
#
# @param \$map    reference to map reference
# @param $is_new  specifies whether map items should be marked as new)
#
sub convert_map_to_hash
{
    my ($map, $is_new) = @_;
    if ( !defined($is_new) ) {
        $is_new = 0;
    }
    if ( ref(${$map}) eq 'HASH' ) {
        if ( defined(${$map}->{'__map'}) ) {
            # Do nothing
            if ( $is_new ) {
                die("Map already contains changes so the items can't be marked as new.");
            }
        }
        else {
            ${$map} = {'__map' => ${$map}};
            ${$map}->{'new'} = [];
            foreach my $item_key (keys %{${$map}->{'__map'}}) {
                push(@{${$map}->{'new'}}, $item_key);
            }
        }
    }
    else {
        ${$map} = {};
    };
}

#
# Set item value to map
#
# @param $map       reference to map reference
# @param $item_key  item key
#
sub set_map_item
{
    my ($map, $item_key, $item_value) = @_;
    convert_map_to_hash($map);
    # If item is not new, mark it as "modified"
    if ( !array_value_exists($item_key, @{${$map}->{'new'}}) ) {
        if ( !defined(${$map}->{'modified'}) ) {
            ${$map}->{'modified'} = [];
        }
        array_remove_value($item_key, ${$map}->{'modified'});
        push(@{${$map}->{'modified'}}, $item_key);
    }
    ${$map}->{'__map'}->{$item_key} = $item_value;
}

#
# Add new item to map
#
# @param $map         reference to map reference
# @param $item_key    new item key
# @param $item_value  new item value
#
sub add_map_item
{
    my ($map, $item_key, $item_value) = @_;
    convert_map_to_hash($map);

    # If item is deleted, mark it as "modified"
    if ( array_value_exists($item_key, @{${$map}->{'deleted'}}) ) {
        array_remove_value($item_key, ${$map}->{'deleted'});
        if ( !defined(${$map}->{'modified'}) ) {
            ${$map}->{'modified'} = [];
        }
        array_remove_value($item_key, ${$map}->{'modified'});
        push(@{${$map}->{'modified'}}, $item_key);
    }
    # If item is already defined, mark it as "modified"
    elsif ( defined(${$map}->{'__map'}->{$item_key}) ) {
        if ( !defined(${$map}->{'modified'}) ) {
            ${$map}->{'modified'} = [];
        }
        array_remove_value($item_key, ${$map}->{'modified'});
        push(@{${$map}->{'modified'}}, $item_key);
    }
    # If item is not defined, mark it as "new"
    else {
        if ( !defined(${$map}->{'new'}) ) {
            ${$map}->{'new'} = [];
        }
        array_remove_value($item_key, ${$map}->{'new'});
        push(@{${$map}->{'new'}}, $item_key);
    }

    ${$map}->{'__map'}->{$item_key} = $item_value;
}

#
# Remove item from map by key
#
# @param $map         reference to map reference
# @param $item_key    item key
#
sub remove_map_item
{
    my ($map, $item_key) = @_;
    convert_map_to_hash($map);
    # Remove item mark as "modified"
    if ( defined(${$map}->{'modified'}) ) {
        array_remove_value($item_key, ${$map}->{'modified'});
    }
    # Remove item mark as "new"
    if ( defined(${$map}->{'new'}) && array_value_exists($item_key, @{${$map}->{'new'}})) {
        array_remove_value($item_key, ${$map}->{'new'});
    }
    # If item was not new, mark it as "deleted"
    else {
        if ( !defined(${$map}->{'deleted'}) ) {
            ${$map}->{'deleted'} = [];
        }
        push(@{${$map}->{'deleted'}}, $item_key);
    }
    delete ${$map}->{'__map'}->{$item_key};
}

#
# Format date/time
#
# @param $dateTime
#
sub format_datetime
{
    my ($dateTime) = @_;
    if ( defined($dateTime) ) {
        $dateTime = DateTime::Format::ISO8601->parse_datetime($dateTime);
        return sprintf("%s %02d:%02d", $dateTime->ymd, $dateTime->hour, $dateTime->minute);
    }
    return $dateTime;
}

#
# Format period
#
# @param $period
#
sub format_period
{
    my ($period) = @_;
    #$period = 'P2Y1M3DT2H4M3S';
    if ( $period =~ /P((\d+)Y)?((\d+)M)?((\d+)D)?(T((\d+)H)?((\d+)M)?((\d+)S)?)?/) {
        my $components = ordered_hash(
            'year' => {'value' => $2, 'separator' => ' '},
            'month' => {'value' => $4, 'separator' => ' '},
            'day' => {'value' => $6, 'separator' => ' '},
            'hour' => {'value' => $9, 'separator' => ' '},
            'minute' => {'value' => $11, 'separator' => ' '},
            'second' => {'value' => $13, 'separator' => ' '}
        );
        $period = '';
        foreach my $component (ordered_hash_keys($components)) {
            my $value = undef;
            if ( defined($components->{$component}->{'value'}) ) {
                $value = int($components->{$component}->{'value'});
            }
            my $separator = $components->{$component}->{'separator'};
            if ( defined($value) && $value > 0 ) {
                if ( length($period) > 0 ) {
                    $period .= $separator;
                }
                $period .= $value . ' ' . $component;
                if ( $value > 1 ) {
                    $period .= 's';
                }
            }
        }
    #    var_dump($period);
    }
    return $period;
}

#
# Format date/time
#
# @param $dateTime
#
sub format_date
{
    my ($dateTime) = @_;
    if ( defined($dateTime) ) {
        $dateTime = DateTime::Format::ISO8601->parse_datetime($dateTime);
        return sprintf("%s", $dateTime->ymd);
    }
    return $dateTime;
}

#
# Format date/time partial
#
# @param $dateTime
#
sub format_datetime_partial
{
    my ($dateTime) = @_;
    if ( defined($dateTime) ) {
        return sprintf("%s", $dateTime);
    } else {
        return "";
    }
}

#
# Format interval
#
# @param $interval
#
sub format_interval
{
    my ($interval) = @_;
    if ( defined($interval) && $interval =~ m/(.*)\/(.*)/ ) {
        return sprintf("%s, %s", format_datetime($1), $2);
    } else {
        return "";
    }
}

#
# Format report
#
# @param $report
# @param $max_line_width
#
sub format_report
{
    my ($report, $max_line_width) = @_;

    my @lines = ();
    if ( defined($report) ) {
        @lines = split("\n", $report);
    }
    $report = '';
    # Process each line from report
    for ( my $index = 0; $index < scalar(@lines); $index++ ) {
        my $line = $lines[$index];
        my $nextLine = $lines[$index + 1];
        if ( length($report) > 0 ) {
            $report .= "\n";
        }

        # Calculate new line indent
        my $indent = '';
        if ( $line =~ /(^[| ]*\+?)(-+)/ ) {
            my $start = $1;
            $indent = $1 . $2;
            $indent =~ s/[-]/ /g;

            $start =~ s/[\+]/|/g;
            if ( defined($nextLine) && $nextLine =~ /^\Q$start/ ) {
                $indent =~ s/\+/|/g;
            }
            else {
                $indent =~ s/\+/ /g;
            }
        }

        # Break line to multiple lines
        while ( length($line) > $max_line_width ) {
            my $index = rindex($line, " ", $max_line_width);
            if ($index == -1) {
                $index = $max_line_width;
            }
            $report .= substr($line, 0, $index);
            $report .= "\n". $indent;
            $line = substr($line, $index + 1);
        }

        # Append the rest
        $report .= $line;
    }
    if ( !defined($report) || $report eq '' ) {
        $report = "-- No report --";
    }
    return $report;
}

#
# Dump given arguments
#
sub var_dump
{
    use Data::Dumper;
    print Dumper(@_);
}

#
# Get home directory
#
sub get_home_directory
{
    return File::HomeDir->my_home;
}

#
# Get terminal width
#
sub get_term_width
{
    use Term::ReadKey;
    my ($wchar, $hchar, $wpixels, $hpixels) = GetTerminalSize();
    return $wchar
}

#
# Indent block
#
# @param $text
# @param $size
# @param $indent_first
#
sub text_indent_lines
{
    my ($text, $size, $indent_first) = @_;
    if ( !defined($size) ) {
        $size = 2;
    }
    if ( !defined($indent_first) ) {
        $indent_first = 1;
    }
    if ( !defined($text) ) {
        return $text;
    }
    my $indent = '';
    for ( my $index = 0; $index < $size; $index++ ) {
        $indent .= ' ';
    }
    $text =~ s/\n/\n$indent/g;
    $text =~ s/\n +$/\n/g;
    if ( $indent_first ) {
        $text = $indent . $text;
    }
    return $text;
}

#
# @param $text
# @param $color
# @return colorized $text
#
no warnings "all";
sub colored
{
    my ($text, $color) = @_;
    my @lines = split("\n", $text);
    $text = '';
    for ( my $index = 0; $index < scalar(@lines); $index++ ) {
        if ( length($text) > 0 ) {
            $text .= "\n";
        }
        $text .= sprintf("%s", Term::ANSIColor::colored($lines[$index], $color));
    }
    return $text;
}
use warnings "all";

# History
our $history = {};

#
# Load history from file
#
# @param $history_file
#
sub history_load
{
    my ($history_file) = @_;

    my $group = 'default';
    if ( open FILE, '<'. $history_file ) {
        my @lines = <FILE>;
        foreach my $line (@lines) {
            $line =~ s/\s+$//g;
            if ( defined($line) && !($line eq '') ) {
                if ( $line =~ /^\[(.+)\]/ ) {
                    $group = $1;
                } else {
                    if ( !defined($history->{$group}) ) {
                        $history->{$group} = [];
                    }
                    push(@{$history->{$group}}, $line);
                }
            }
        }
        close(FILE);
    }
}

#
# Trim text
#
# @text
#
sub trim
{
    my ($text) = @_;
    $text =~ s/\s+$//g;
    $text =~ s/^\s+//g;
    return $text;
}

#
# Save history to file
#
# @param $history_file
#
sub history_save
{
    my ($history_file) = @_;
    if ( open FILE, '>'. $history_file ) {
        while(my ($group, $values) = each(%{$history})) {
            print FILE sprintf("[%s]\n", $group);
            foreach my $value (@{$values}) {
                print FILE sprintf("%s\n", $value);
            }
        }
        close(FILE);
    }
}

#
# Get history group
#
# @param $group
# @param $target
#
sub history_get_group_from
{
    my ($group, $target) = @_;

    #printf("get history from '%s'\n", $group);

    my @values = $target->GetHistory();
    $history->{$group} = [];
    my $previous_value = undef;
    my $start = 0;
    if ( scalar(@values) > 100 ) {
        $start = scalar(@values) - 101;
    }
    for ( my $index = $start; $index < @values; $index++) {
        my $value = $values[$index];
        if ( !defined($previous_value) || !($previous_value eq $value) ) {
            push(@{$history->{$group}}, $value);
        }
        $previous_value = $value;
    }
}

#
# Set history group
#
# @param $group
# @param $target
#
sub history_set_group_to
{
    my ($group, $target) = @_;

    #printf("set history to '%s'\n", $group);

    $target->SetHistory();
    if ( defined($history->{$group}) ) {
        foreach my $value (@{$history->{$group}}) {
            $target->addhistory($value);
        }
    }
}

1;