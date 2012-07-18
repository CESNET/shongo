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