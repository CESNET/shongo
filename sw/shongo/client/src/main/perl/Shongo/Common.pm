#
# Reservation class - Management of reservations.
#
package Shongo::Common;

use strict;
use warnings;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    ordered_hash ordered_hash_keys
    get_collection_size get_collection_items get_collection_item add_collection_item remove_collection_item
    format_datetime
    var_dump
);

use DateTime::Format::ISO8601;

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
# Get number of items in collection. Collection can be stored as hash('new' => [], 'modified' => [], 'deleted' => []).
#
# @param $collection  collection reference
# @return collection size
#
sub get_collection_size
{
    my ($collection) = @_;
    if ( ref($collection) eq 'ARRAY' ) {
        return scalar(@{$collection});
    }
    elsif ( ref($collection) eq 'HASH' ) {
        my $count = 0;
        if ( defined($collection->{'modified'}) ) {
            $count += scalar(@{$collection->{'modified'}});
        }
        if ( defined($collection->{'new'}) ) {
            $count += scalar(@{$collection->{'new'}});
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
        if ( defined($collection->{'modified'}) ) {
            push($array, @{$collection->{'modified'}});
        }
        if ( defined($collection->{'new'}) ) {
            push($array, @{$collection->{'new'}});
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
    if ( ref($collection) eq 'ARRAY' ) {
        return $collection->[$item_index];
    }
    elsif ( ref($collection) eq 'HASH' ) {
        my $items = get_collection_items($collection);
        return $items->[$item_index];
    }
    else {
        return undef;
    }
}

#
# Convert collection to hash
#
# @param \$collection  reference to collection reference
#
sub convert_collection_to_hash
{
    my ($collection) = @_;
    if ( ref(${$collection}) eq 'HASH' ) {
        # Do nothing
    }
    elsif ( ref(${$collection}) eq 'ARRAY' ) {
        ${$collection} = {'modified' => ${$collection}};
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
    push(${$collection}->{'new'}, $item);
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
    if ( defined(${$collection}->{'modified'}) ) {
        my $modified_count = scalar(@{${$collection}->{'modified'}});
        if ( $item_index < $modified_count ) {
            my $item = ${$collection}->{'modified'}[$item_index];
            splice(${$collection}->{'modified'}, $item_index, 1);
            if ( !defined(${$collection}->{'deleted'}) ) {
                ${$collection}->{'deleted'} = [];
            }
            push(${$collection}->{'deleted'}, $item);
            return;
        } else {
            $item_index -= $modified_count;
        }
    }
    if ( defined(${$collection}->{'new'}) ) {
        my $new_count = scalar(@{${$collection}->{'new'}});
        if ( $item_index < $new_count ) {
            splice(${$collection}->{'new'}, $item_index, 1);
            return;
        }
    }
    console_print_error("Cannot delete item with index '%d' in collection '%s'.", $item_index, $collection);
}

#
# Format date/time
#
# @param $dateTime
#
sub format_datetime
{
    my ($dateTime) = @_;
    $dateTime = DateTime::Format::ISO8601->parse_datetime($dateTime);
    return sprintf("%s %02d:%02d", $dateTime->ymd, $dateTime->hour, $dateTime->minute);
}

#
# Dump given arguments
#
sub var_dump
{
    use Data::Dumper;
    print Dumper(@_);
}

1;