#
# Reservation class - Management of reservations.
#
package Shongo::Console;

use strict;
use warnings;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    console_print_info console_print_error
    console_read console_read_choice console_select
);

use Term::ReadLine::Zoid;
use Term::ANSIColor;

sub console_print_info
{
    my ($message, @parameters) = @_;
    print STDERR colored("" . sprintf($message, @parameters), "bold blue"), "\n";
}

sub console_print_error
{
    my ($message, @parameters) = @_;
    print STDERR colored("[ERROR] " . sprintf($message, @parameters), "red"), "\n";
}

sub console_read
{
    my ($message, $required, $regex, $value) = @_;

    # Show prompt and run loop for getting proper value
    my $term = Term::ReadLine::Zoid->new();
    while ( 1 ) {
        if ( defined($value) ) {
            if ( $required && $value eq "" ) {
                console_print_error("Value must not be empty.");
            }
            elsif ( !defined($regex) || $value =~ m/$regex/ ) {
                return $value;
            }
            elsif ( $value eq "" ) {
                return;
            }
            else {
                console_print_error("Value must match '%s'.", $regex);
            }
        }
        $value = $term->readline(colored(sprintf("%s: ", $message), "bold blue"));
    }
}

sub console_read_choice
{
    my ($message, $count) = @_;

    my $term = Term::ReadLine::Zoid->new();

    # Show prompt and run loop for getting proper value
    while ( 1 ) {
        my $choice = $term->readline(colored(sprintf("%s: ", $message), "bold blue"));
        if ( ($choice=~/\d/) && $choice >= 1 && $choice <= $count ) {
            return $choice;
        }
        elsif ($choice eq 'exit' ) {
            return;
        }
        else {
            console_print_error("You must choose value from %d to %d.", 1, $count);
        }
    }
}

sub console_select
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
            console_print_error($error);
        }
    }

    # Show prompt and run loop for getting proper value
    printf("%s\n", colored($message . ':', "bold blue"));
    my @result = ();
    my $index;
    for ( $index = 0; $index < @map_keys; $index++ ) {
        my $key = $map_keys[$index];
        my $value = $map{$key};
     	printf("%s %s\n", colored(sprintf("%d)", $index + 1), "bold blue"), $value);
        push(@result, $key);
     }
    while ( 1 ) {
        my $choice = console_read_choice("Enter number of choice", $index);
        if ( defined($choice) ) {
            return $result[$choice - 1];
        }
        return;
    }
}

1;