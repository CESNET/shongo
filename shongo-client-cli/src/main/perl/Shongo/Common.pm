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
    get_enum_value
    array_value_exists array_remove_value
    get_collection_size get_map_size get_map_item_key get_map_item_value
    iso8601_datetime_parse iso8601_datetime_format iso8601_period_parse iso8601_period_format
    datetime_add_duration datetime_get_timezone datetime_fill_timezone datetime_format datetime_format_date
    datetime_partial_format
    period_format
    interval_get_duration interval_format interval_format_date
    format_report
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
our $IdPattern                 = '(^\\d+|shongo:.+:\\d+$)';
our $UserIdPattern             = '(^\\d+$)';
our $DateTimePattern           = '(^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d(:\\d\\d(.\\d+)?(Z|[\\+-]\\d\\d(:\\d\\d)?)?)?$)';
our $DateTimeOrInfinitePattern = '(' . $DateTimePattern . '|(^\\*$))';
our $PeriodPattern             = '(^P((\\d+)Y)?((\\d+)M)?((\\d+)W)?((\\d+)D)?(T((\\d+)+H)?((\\d+)M)?((\\d+)(\\.\\d+)?S)?)?$)';
our $DateTimePartialPattern    = '(^\\d\\d\\d\\d(-\\d\\d)?(-\\d\\d)?(T\\d\\d(:\\d\\d)?)?$)';

# Represents a "null" constant
use constant NULL => '__null__';

# Enumeration of technologies
our $Technology = ordered_hash(
    'H323' => 'H.323',
    'SIP' => 'SIP',
    'ADOBE_CONNECT' => 'Adobe Connect',
    'FREEPBX' => 'FreePBX',
    'SKYPE_FOR_BUSINESS' => 'Skype for Business',
    'WEBRTC' => 'WebRTC',
    'RTMP' => 'RTMP'
);

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
# Get number of items in collection.
#
# @param $collection  collection reference
# @return collection size
#
sub get_collection_size
{
    my ($collection) = @_;
    my $items = $collection;
    if ( defined($items) ) {
        return scalar(@{$items});
    }
    else {
        return 0;
    }
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
    return scalar(keys %{$map});
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
    my @item_keys = keys %{$map};
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
    return $map->{$item_key};
}

#
# Parse ISO8601 date/time
#
# @param $datetime ISO8601 date/time
# @return DateTime object
#
sub iso8601_datetime_parse
{
    my ($datetime) = @_;
    my $result = DateTime::Format::ISO8601->parse_datetime($datetime);
    if ( !($datetime =~ /(Z|[\+-]\d\d(:\d\d)?)$/) ) {
        $result->{'__omit_timezone'} = 1;
    }
    return $result;
}

#
# Format ISO8601 date/time
#
# @param $datetime DateTime object
# @return ISO8601 date/time
#
sub iso8601_datetime_format
{
    my ($dateTime) = @_;
    my $result = $dateTime->strftime('%FT%T.%3N');
    if ( !defined($dateTime->{'__omit_timezone'}) ) {
        if ( $dateTime->time_zone()->is_utc() ) {
            $result .= 'Z';
        }
        else {
            $result .= $dateTime->strftime('%z');
            $result =~ s/(\d\d)$/:$1/;
        }
    }
    return $result;
}

#
# Parse ISO8601 period
#
# @param $period ISO8601 period
# @return DateTime::Duration object
#
sub iso8601_period_parse
{
    my ($period) = @_;
    if ( $period =~ /$PeriodPattern/ ) {
        return DateTime::Duration->new(
            years   => defined($3) ? $3 : 0,
            months  => defined($5) ? $5 : 0,
            days    => ((defined($7) ? (7 * $7) : 0)) + (defined($9) ? $9 : 0),
            hours   => defined($12) ? $12 : 0,
            minutes => defined($14) ? $14 : 0,
            seconds => defined($16) ? $16 : 0
        );
    }
    return undef;
}

#
# Format ISO8601 period
#
# @param $period DateTime::Duration object
# @return ISO8601 period
#
sub iso8601_period_format
{
    my ($period) = @_;

    # Skip infinite period
    if ( !ref($period) && $period eq '*' ) {
        return '*';
    }

    # Build date part
    my $result = '';
    if ( $period->years > 0 ) {
        $result .= $period->years . 'Y';
    }
    if ( $period->months > 0 ) {
        $result .= $period->months . 'M';
    }
    if ( $period->weeks > 0 ) {
        $result .= $period->weeks . 'W';
    }
    if ( $period->days > 0 ) {
        $result .= $period->days . 'D';
    }
    # Build time part
    my $resultTime = '';
    if ( $period->hours > 0 ) {
        $resultTime .= $period->hours . 'H';
    }
    if ( $period->minutes > 0 ) {
        $resultTime .= $period->minutes . 'M';
    }
    if ( $period->seconds > 0 ) {
        $resultTime .= $period->seconds . 'S';
    }
    # Append time part
    if ( length($resultTime) > 0 ) {
        $result .= 'T' . $resultTime;
    }
    # Empty duration
    if ( length($result) == 0 ) {
        $result = 'T0S';
    }
    return 'P' . $result;
}

#
# @return timezone
#
sub datetime_get_timezone
{
    my $timezone = DateTime->now(time_zone => DateTime::TimeZone->new(name => 'local'))->strftime("%z");
    $timezone =~ s/(\d\d)$/:$1/;
    return $timezone;
}

#
# If given $datetime doesn't have timezone fill the default one
#
# @param $datetime
# @param $timeZoneOffset
# @return $datetime
#
sub datetime_fill_timezone
{
    my ($datetime, $timeZoneOffset) = @_;
    $datetime = iso8601_datetime_parse($datetime);
    if ( defined($datetime->{'__omit_timezone'}) ) {
       delete $datetime->{'__omit_timezone'};
        if ( defined($timeZoneOffset) ) {
            $datetime->set_time_zone(DateTime::TimeZone->new(name => DateTime::TimeZone->offset_as_string($timeZoneOffset)));
        }
        else {
            $datetime->set_time_zone(DateTime::TimeZone->new(name => 'local'));
        }
    }
    return iso8601_datetime_format($datetime);
}

#
# Add duration to date time
#
# @param $datetime
# @param $duration
#
sub datetime_add_duration
{
    my ($datetime, $duration) = @_;
    my $tmp = iso8601_datetime_parse($datetime);
    if ( !ref($duration) ) {
        $duration = iso8601_period_parse($duration);
    }
    my $omit_timezone = $tmp->{'__omit_timezone'};
    $tmp->add_duration($duration);
    $tmp->{'__omit_timezone'} = $omit_timezone;
    return iso8601_datetime_format($tmp);
}

#
# Format date/time
#
# @param $dateTime
# @param $timeZoneOffset
#
sub datetime_format
{
    my ($dateTime, $timeZoneOffset) = @_;
    if ( defined($dateTime) ) {
        if ( !ref($dateTime) ) {
            if ( $dateTime eq '*' ) {
                return '*';
            }
            $dateTime = iso8601_datetime_parse($dateTime);
            if ( defined($timeZoneOffset) ) {
                $dateTime->set_time_zone(DateTime::TimeZone->new(name => DateTime::TimeZone->offset_as_string($timeZoneOffset)));
            }
            else {
                $dateTime->set_time_zone(DateTime::TimeZone->new(name => 'local'));
            }
        }
        return sprintf("%s %02d:%02d", $dateTime->ymd, $dateTime->hour, $dateTime->minute);
    }
    return $dateTime;
}

#
# Format date/time
#
# @param $dateTime
# @param $timeZoneOffset
#
sub datetime_format_date
{
    my ($dateTime, $timeZoneOffset) = @_;
    if ( defined($dateTime) ) {
        if ( !ref($dateTime) ) {
            if ( $dateTime eq '*' ) {
                return '*';
            }
            $dateTime = iso8601_datetime_parse($dateTime);
            if ( defined($timeZoneOffset) ) {
                $dateTime->set_time_zone(DateTime::TimeZone->new(name => DateTime::TimeZone->offset_as_string($timeZoneOffset)));
            }
            else {
                $dateTime->set_time_zone(DateTime::TimeZone->new(name => 'local'));
            }
        }
        return sprintf("%s", $dateTime->ymd);
    }
    return $dateTime;
}

#
# Format date/time partial
#
# @param $dateTime
#
sub datetime_partial_format
{
    my ($dateTime) = @_;
    if ( defined($dateTime) ) {
        return sprintf("%s", $dateTime);
    } else {
        return "";
    }
}

#
# Format period
#
# @param $period
#
sub period_format
{
    my ($period) = @_;

    if ( !ref($period) ) {
        if ( $period eq '*' ) {
            return '*';
        }
        $period = iso8601_period_parse($period);
    }
    my $format = '';
    foreach my $component ('years', 'months', 'days', 'hours', 'minutes', 'seconds') {
        my $value = undef;
        if ( defined($period->{$component}) ) {
            $value = int($period->{$component});
        }
        if ( defined($value) && $value > 0 ) {
            if ( length($format) > 0 ) {
                $format .= ' ';
            }
            $format .= $value . ' ' . $component;
            if ( $value == 1 ) {
                # delete last s
                $format =~ s/s$//g;
            }
        }
    }
    if ( $format eq '' ) {
        $format = '0 seconds';
    }
    return $format;
}

#
# Format interval duration
#
# @param $interval
#
sub interval_get_duration
{
    my ($start, $end) = @_;
    if ( $start eq '*' || $end eq '*' ) {
        return '*';
    }
    else {
        $start = iso8601_datetime_parse($start);
        $end = iso8601_datetime_parse($end);
        return $end - $start;
    }
}

#
# Format interval
#
# @param $interval
# @param $timeZoneOffset
#
sub interval_format
{
    my ($interval, $timeZoneOffset) = @_;
    if ( defined($interval) && $interval =~ m/(.*)\/(.*)/ ) {
        my $start = $1;
        my $end = $2;
        my $duration = iso8601_period_format(interval_get_duration($start, $end));
        # Format as "<start>/<end>"
        if ( $duration eq '*' || length($duration) > 6 ) {
            return sprintf("%s/%s", datetime_format($start, $timeZoneOffset), datetime_format($end, $timeZoneOffset));
        }
        # Format as "<start>, <duration>"
        else {
            return sprintf("%s, %s", datetime_format($start, $timeZoneOffset), $duration);
        }
    } else {
        return "";
    }
}

#
# Format interval only as dates
#
# @param $interval
# @param $timeZoneOffset
#
sub interval_format_date
{
    my ($interval, $timeZoneOffset) = @_;
    if ( defined($interval) && $interval =~ m/(.*)\/(.*)/ ) {
        my $start = $1;
        my $end = $2;
        return sprintf("%s/%s", datetime_format_date($start, $timeZoneOffset), datetime_format_date($end, $timeZoneOffset));
    } else {
        return "";
    }
}

#
# Format report item value
#
# @param $value
#
sub format_report_item_value
{
    my ($value) = @_;
    my $output = '';
    if ( ref($value) eq 'ARRAY' ) {
        $output .= '[';
        my $valueData = 0;
        foreach my $valueItem (@{$value}) {
            if ( $valueData ) {
                $output .= ', ';
            }
            $output .= format_report_item_value($valueItem);
            $valueData = 1;
        }
        $output .= ']';
    }
    elsif ( ref($value) eq 'HASH' ) {
        $output .= '{';
        my $valueData = 0;
        foreach my $valueKey (sort keys %{$value}) {
            if ( $valueData ) {
                $output .= ', ';
            }
            $output .= $valueKey;
            $output .= ': ';
            $output .= format_report_item_value($value->{$valueKey});
            $valueData = 1;
        }
        $output .= '}';
    }
    else {
        $output .= $value;
    }
    return $output;
}

#
# Format report item
#
# @param $report
# @param $max_line_width
#
sub format_report_item
{
    my ($report, $max_line_width) = @_;

    my $has_children = defined($report->{'children'});
    my $reportOutput = '';
    $reportOutput .= $report->{'id'};

    my $data = 0;
    foreach my $key (keys %{$report}) {
        if ( $key ne 'id' && $key ne 'children' && $key ne 'type' ) {
            if ( $data ) {
                $reportOutput .= ', ';
            }
            else {
                $reportOutput .= ' (';
            }
            $data = 1;
            my $value = $report->{$key};
            $reportOutput .= $key;
            $reportOutput .= ': ';
            $reportOutput .= format_report_item_value($value);
        }
    }
    if ( $data ) {
        $reportOutput .= ')';
    }
    my $output = '-';
    while ( length($reportOutput) > $max_line_width ) {
        my $index = rindex($reportOutput, " ", $max_line_width);
        if ($index == -1) {
            $index = $max_line_width;
        }
        $output .= substr($reportOutput, 0, $index);
        if ( $has_children ) {
            $output .= "\n | ";
        }
        else {
            $output .= "\n ";
        }
        $reportOutput = substr($reportOutput, $index + 1);
    }
    $output .= $reportOutput;

    if ( $has_children ) {
        my $reportCount = scalar(@{$report->{'children'}});
        for ( my $reportIndex = 0; $reportIndex < $reportCount; $reportIndex++ ) {
            my $childReport = $report->{'children'}->[$reportIndex];
            $output .= "\n |";
            $childReport = format_report_item($childReport, $max_line_width - 3);
            my @childReportLines = split("\n", $childReport);
            for ( my $lineIndex = 0; $lineIndex < scalar(@childReportLines); $lineIndex++ ) {
                my $line = $childReportLines[$lineIndex];
                my $indent = '';
                if ( $lineIndex == 0 ) {
                    $output .= "\n +-";
                }
                elsif ( $reportIndex < ($reportCount - 1) ) {
                    $output .= "\n | ";
                }
                else {
                    $output .= "\n   ";
                }
                while ( length($line) > $max_line_width ) {
                    my $index = rindex($line, " ", $max_line_width);
                    if ($index == -1) {
                        $index = $max_line_width;
                    }
                    $output .= substr($line, 0, $index);
                    $output .= "\n   ";
                    $line = substr($line, $index + 1);
                }
                $output .= $line;
            }
        }
    }

    return $output;
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

    my $reportText = '';
    foreach my $reportItem (@{$report->{'reports'}}) {
        if ( length($reportText) > 0 ) {
            $reportText .= "\n";
        }
        $reportText .= format_report_item($reportItem, $max_line_width) . "\n";
    }
    $report = $reportText;

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