#
# Reservation class - Management of reservations.
#
package Shongo::Common;

use strict;
use warnings;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    ordered_hash ordered_hash_ref
    var_dump
);

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

    return %hash;
}

#
# Create hash reference from given values which has item "__keys" as array with keys in insertion order.
#
# @param values array of pair of items (even count)
# @return created has
#
sub ordered_hash_ref
{
    my %hash = ordered_hash(@_);
    return \%hash;
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