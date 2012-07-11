#
# Reservation class - Management of reservations.
#
package Shongo::Client::Dialog;

use strict;
use warnings;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    dialog_error
    dialog_get dialog_select
    ordered_hash ordered_hash_ref
);

use Term::ReadLine::Zoid;
use Term::ANSIColor;

sub dialog_error
{
    my ($message, @parameters) = @_;
    print STDERR colored("[ERROR] " . sprintf($message, @parameters), "red"), "\n";
}

sub dialog_get
{
    my ($message, $regex, $required) = @_;

    my $term = Term::ReadLine::Zoid->new();

    # Show prompt and run loop for getting proper value
    while ( 1 ) {
        my $value = $term->readline(colored(sprintf("%s: ", $message), "bold blue"));
        if ( $required && $value eq "" ) {
            dialog_error("Value must not be empty.");
        }
        elsif ( !defined($regex) || $value =~ m/$regex/ ) {
            return $value;
        }
        elsif ( $value eq "" ) {
            return;
        }
        else {
            dialog_error("Value must match '%s'.", $regex);
        }
    }
}

sub dialog_select
{
    my ($message, $values, $value) = @_;
    my %map = %{$values};

    # Get keys for map
    my @map_keys;
    if ( defined($map{'__keys'}) ) {
        @map_keys = @{$map{'__keys'}};
    }
    else {
        @map_keys = keys %map;
    }

    # Swap keys and values
    my %map_swapped;
    $map_swapped{$map{$_}} = $_ for @map_keys;

    # Check already passed value
    if ( defined($value) ) {
        if ( defined($map{$value}) ) {
            return $value;
        }
        else {
            my $error = "Illegal value '$value'. Allowed values are:";
            my $first = 1;
            while ( my ($key, $value) = each %map_swapped ) {
                if ( $first == 0) {
                    $error .= ",";
                }
                $error .= " '$value'";
                $first = 0;
            }
            dialog_error($error);
        }
    }

    # Show prompt and run loop for getting proper value
    printf("%s:\n", colored($message, "bold blue"));
    my @result = ();
    my $index;
    for ( $index = 0; $index < @map_keys; $index++ ) {
        my $key = $map_keys[$index];
        my $value = $map{$key};
     	printf("%s %s\n", colored(sprintf("%d)", $index + 1), "bold blue"), $value);
        push(@result, $key);
     }
    while ( 1 ) {
        printf("Enter number of choice: ");
        my $choice = <>;
        if ( ($choice=~/\d/) && $choice >= 1 && $choice <= $index ) {
            return $result[$choice - 1];
        }
        else {
            dialog_error("You must choose value from %d to %d.", 1, $index);
        }
    }
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

sub ordered_hash_ref
{
    my %hash = ordered_hash(@_);
    return \%hash;
}

1;