#
# Functions for dialog in console.
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Console;

use strict;
use warnings;
use Switch;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(
    console_print_text
    console_print_debug console_print_info console_print_error console_print_table
    console_read console_read_choice console_select
    console_action console_action_loop
    console_auto_value console_read_value console_edit_value
    console_auto_enum console_read_enum console_edit_enum
    console_edit_bool
    console_read_password
);

BEGIN {
    if ($^O eq "MSWin32")
    {
        require Win32::Console::ANSI;
        Win32::Console::ANSI->import();
    }
}
use Term::ReadKey;
use Shongo::Common;

#
# Print text
#
sub console_print_text
{
    my ($text) = @_;
    if ( ref($text) eq 'HASH' ) {
        if ( exists $text->{'to_string'} ) {
            $text = $text->{'to_string'}();
        }
        else {
            $text = JSON->new()->pretty->encode($text);
        }
    }
    elsif ( ref($text) eq 'ARRAY' ) {
        if ( Shongo::ClientCli::is_scripting() ) {
            $text = JSON->new()->pretty->encode($text);
        }
        else {
            my $index = 0;
            foreach my $item (@{$text}) {
                $index++;
                if ( ref($item) ) {
                    $item = $item->to_string();
                }
                printf(" %d) %s\n", $index, text_indent_lines($item, 4, 0));
            }
            return;
        }
    }
    elsif( ref($text) ) {
        if ( Shongo::ClientCli::is_scripting() ) {
            $text = JSON->new()->pretty->encode($text->to_hash());
        }
        else {
            $text = $text->to_string();
        }
    }
    if ( Shongo::ClientCli::is_scripting() ) {
        $text =~ s/\n$//g;
        Shongo::ClientCli->set_scripting_result($text);
        printf("%s\n", $text);
    }
    else {
        $text =~ s/\n *$//g;
        $text = text_indent_lines($text, 1, 1);
        printf("\n%s\n\n", $text);
    }
}

#
# Print DEBUG message to console
#
# @param $message     message to be printed
# @param $parameters  format parameters for message
#
sub console_print_debug
{
    my ($message, @parameters) = @_;
    $message =~ s/\s*$//;
    if ( !Shongo::ClientCli::is_scripting() ) {
        if ( scalar(@parameters) > 0 ) {
            print sprintf($message, @parameters), "\n";
        }
        else {
            print $message, "\n";
        }
    }
}

#
# Print INFO message to console
#
# @param $message     message to be printed
# @param $parameters  format parameters for message
#
sub console_print_info
{
    my ($message, @parameters) = @_;
    $message =~ s/\s*$//;
    if ( Shongo::ClientCli::is_scripting() ) {
        foreach my $parameter (@parameters) {
            Shongo::ClientCli->set_scripting_result($parameter);
            print $parameter . "\n";
        }
    }
    else {
        print colored(sprintf($message, @parameters), "bold blue"), "\n";
    }
}

#
# Print ERROR message to console
#
# @param $message     message to be printed
# @param $parameters  format parameters for message
#
sub console_print_error
{
    my ($message, @parameters) = @_;
    $message =~ s/\s*$//;
    print STDERR colored("[ERROR] " . sprintf($message, @parameters), "red"), "\n";
}

#
# Print table to console
#
# @param $table
#
sub console_print_table
{
    my ($table, $indent) = @_;
    if ( Shongo::ClientCli::is_scripting() ) {
        my $data = [];
        foreach my $data_item (@{$table->{'data'}}) {
            my $new_data_item = {};
            foreach my $key (keys %{$data_item}) {
                my $value = $data_item->{$key};
                if ( ref($value) eq 'ARRAY' && scalar(@{$value}) >= 1 ) {
                    $value = $value->[0];
                }
                $new_data_item->{$key} = $value;
            }
            push(@{$data}, $new_data_item);
        }
        print JSON->new()->pretty->encode($data);
    }
    else {
        my $string = '';

        # Create text table with columns
        my @text_table_columns = (\'| ');
        foreach my $column (@{$table->{'columns'}}) {
            if ( scalar(@text_table_columns) > 1 ) {
                push(@text_table_columns, \' | ');
            }
            push(@text_table_columns, $column->{'title'});
        }
        push(@text_table_columns, \' |');
        my $text_table = Text::Table->new(@text_table_columns);

        # Fill text table with data
        foreach my $data_item (@{$table->{'data'}}) {
            my @text_table_row = ();
            foreach my $column (@{$table->{'columns'}}) {
                my $value = $data_item->{$column->{'field'}};
                if ( ref($value) eq 'ARRAY' && scalar(@{$value}) >= 2 ) {
                    $value = $value->[1];
                }
                push(@text_table_row, $value);
            }
            $text_table->add(@text_table_row);
        }

        # Print table
        $string .= $text_table->rule( '-', '+');
        $string .= $text_table->title;
        $string .= $text_table->rule( '-', '+');
        if ( !defined($text_table->body) || $text_table->body eq '' ) {
            my $empty_text = ' -- None -- ';
            my $width = $text_table->width() - 2 - length($empty_text);
            my $left = $width / 2;
            my $right = $width / 2 + ($width % 2);
            $string .= sprintf("|%" . $left . "s%s%" . $right . "s|\n", '', $empty_text, '');
            $string .= $text_table->rule( '-', '+');
        } else {
            $string .= $text_table->body;
            $string .= $text_table->rule( '-', '+');
        }
        if ( defined($indent) ) {
            $string = text_indent_lines($string, $indent);
        }
        print $string;
    }
}

#
# Read a single value from input stream
#
# @param $message  message to display as prompt
# @param $value    value to be written as default after prompt
#
sub console_read
{
    my ($message, $value) = @_;
    my $term = Term::ReadLine->new('read_line');
    $term->ornaments(0);
    $term->SetHistory();
    my $line = $term->readline(colored(sprintf("%s: ", $message), "bold blue"), $value);
    utf8::decode($line);
    return $line;
}

#
# Read a choice number
#
# @param $message      message to show as prompt
# @param $required     option specifies whether user must choose
# @param $max_choice   maximum choice number (starting from 1)
# @param $default      default value written to user
#
sub console_read_choice
{
    my ($message, $required, $max_choice, $default) = @_;

    # Show prompt and run loop for getting proper value
    while ( 1 ) {
        my $choice = console_read($message, $default);
        $default = $choice;
        if ( ($choice=~/^\d+$/) && $choice >= 1 && $choice <= $max_choice ) {
            return $choice;
        }
        elsif ($choice eq 'exit' || (!$required && $choice eq '') ) {
            return;
        }
        else {
            console_print_error("You must choose value from %d to %d.", 1, $max_choice);
        }
    }
}

#
# Select key from hash of values
#
# @param $message  message to be shown as prompt
# @param $values   hash of ($key => $title) pairs
# @param $default  default value
#
sub console_select
{
    my ($message, $values, $default) = @_;
    my %map = %{$values};
    my @map_keys = ordered_hash_keys(\%map);

    # Swap keys and values
    my %map_swapped;
    $map_swapped{$map{$_}} = $_ for @map_keys;

    # Show prompt and run loop for getting proper value
    printf("%s\n", colored($message . ':', "bold blue"));
    my @result = ();
    my $index;
    my $default_index;
    for ( $index = 0; $index < @map_keys; $index++ ) {
        my $key = $map_keys[$index];
        my $value = $map{$key};
     	printf("%s %s\n", colored(sprintf("%d)", $index + 1), "bold blue"), $value);
        push(@result, $key);
        if ( defined($default) && $key eq $default ) {
            $default_index = $index + 1;
        }
    }
    while ( 1 ) {
        my $choice = console_read_choice("Enter number of choice", 1, $index, $default_index);
        if ( defined($choice) ) {
            return $result[$choice - 1];
        }
        return;
    }
}

#
# Prints all given actions and asks user to select one
#
# @param $actions  hash of action pairs ($title => $method) or sub returning it
# @param $prompt
#
sub console_action
{
    my ($actions, $prompt) = @_;
    if ( !defined($prompt) ) {
        $prompt = 'Select action';
    }

    if ( ref($actions) eq 'CODE' ) {
        $actions = &{$actions}();
    }

    my @action_array = ();
    my $index = 0;
    foreach my $action (ordered_hash_keys($actions)) {
        if ( !($action eq "__keys") ) {
            push(@action_array, $index, $action);
            $index++;
        }
    }

    my $action_hash = ordered_hash(@action_array);
    my $action = console_select($prompt, $action_hash);
    if ( defined($action) && defined($action_hash->{$action}) ) {
        $action = $action_hash->{$action};
        my $method = $actions->{$action};
        return &{$method}();
    }
    return 0;
}

#
# Run loop which calls $before_method and asks user for action
#
# @param $before_method  method that is called before each loop run
# @param $actions        hash of actions or sub returning it from which the user can select
# @param $prompt
# @return value that is returned by any action handler method
#
sub console_action_loop
{
    my ($before_method, $actions, $prompt) = @_;
    while ( 1 ) {
        &{$before_method}();
        my $result = console_action($actions);
        if ( defined($result) ) {
            return $result;
        }
    }
}

#
# Validate given value by pattern and required option
#
# @param $value
# @param $required
# @param $pattern
#
sub console_validate_value
{
    my ($value, $required, $pattern) = @_;
    if ( $required && $value eq "" ) {
        console_print_error("Value must not be empty.");
        return 0;
    }
    elsif ( !defined($pattern) || $value =~ m/$pattern/ ) {
        return 1;
    }
    elsif ( $value eq "" ) {
        return 1;
    }
    else {
        console_print_error("Value '%s' must match '%s'.", $value, $pattern);
        return 0;
    }
}

#
# Read/edit a single value
#
# @param $edit  if edit should be performed
# @param ...    parameters to console_(read/edit)_value
#
sub console_auto_value
{
    my ($edit, @params) = @_;
    if ( $edit ) {
        return console_edit_value(@params);
    } else {
        return console_read_value(@params);
    }
}

#
# Edit a single value
#
# @param $message   message to be shown as prompt
# @param $required  option whether not empty value should be returned
# @param $pattern   pattern which returned value must match
# @param $value     value to be edited
#
sub console_edit_value
{
    my ($message, $required, $pattern, $value) = @_;
    $value = console_read($message, $value);
    while ( 1 ) {
        if ( defined($value) && console_validate_value($value, $required, $pattern) ) {
            if ( $value eq '' ) {
                return undef;
            }
            return $value;
        }
        $value = console_read($message, $value);
    }
}

#
# Read a single value
#
# @param $message   message to be shown as prompt
# @param $required  option whether not empty value should be returned
# @param $pattern   pattern which returned value must match
# @param $default   default value to be returned
#
sub console_read_value
{
    my ($message, $required, $pattern, $default) = @_;
    if ( defined($default) && console_validate_value($default, $required, $pattern) ) {
        return $default;
    }
    return console_edit_value($message, $required, $pattern, $default);
}

#
# Read/edit an enum value
#
# @param $edit  if edit should be performed
# @param ...    parameters to console_(read/edit)_enum
#
sub console_auto_enum
{
    my ($edit, @params) = @_;
    if ( $edit ) {
        return console_edit_enum(@params);
    } else {
        return console_read_enum(@params);
    }
}

#
# Edit an enum value
#
# @param $message  message to be shown as prompt
# @param $values   hash of enum ($key => $title) pairs
# @param $value    value to be edited
#
sub console_edit_enum
{
    my ($message, $values, $value) = @_;
    if ( !defined($value) ) {
        $value = NULL;
    }
    return console_select($message, $values, $value);
}

#
# Read an enum value
#
# @param $message  message to be shown as prompt
# @param $values   hash of enum ($key => $title) pairs
# @param $default  default value to return
#
sub console_read_enum
{
    my ($message, $values, $default) = @_;
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
    if ( defined($default) ) {
        if ( defined($map{$default}) ) {
            return $default;
        }
    }
    return console_edit_enum($message, $values, $default);
}

#
# Edit an bool value
#
# @param $message   message to be shown as prompt
# @param $required  option whether not empty value should be returned
# @param $value     value to be edited
#
sub console_edit_bool
{
    my ($message, $required, $default) = @_;
    if ( defined($default) && $default == 1 ) {
        $default = 'yes';
    } elsif ( defined($default) && $default == 0 ) {
        $default = 'no';
    }
    my $result = console_edit_value($message . ' (yes/no)', $required, 'yes|no', $default);
    if ( defined($result) && $result eq 'yes' ) {
        return 1;
    } elsif ( defined($result) && $result eq 'no' ) {
        return 0;
    }
    return undef;
}

#
# Read password value
#
# @param $message   message to be shown as prompt
# @param $required  option whether not empty value should be returned
#
sub console_read_password
{
    my ($message, $required) = @_;

    print(colored(sprintf("%s: ", $message), "bold blue"));

    ReadMode('noecho');
    chomp(my $password = <STDIN>);
    ReadMode('restore');

    print("\n");

    return $password;
}

1;
