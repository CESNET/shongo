#
# Reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

use Term::ANSIColor;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Specification;

# Enumeration of state
our $State = ordered_hash(
    'NOT_COMPLETE' => 'Not Complete',
    'NOT_ALLOCATED' => 'Not Allocated',
    'ALLOCATED' => 'Allocated',
    'ALLOCATION_FAILED' => 'Allocation Failed'
);

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::ReservationRequestAbstract->new(@_);
    bless $self, $class;

    $self->{'class'} = 'ReservationRequest';

    return $self;
}

# @Override
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    push(@{$actions}, (
        'Modify requested slot' => sub {
            my $start = undef;
            my $duration = undef;
            if ( defined($self->{'slot'}) && $self->{'slot'} =~ m/(.*)\/(.*)/ ) {
                $start = $1;
                $duration = $2;
            }
            $start = console_edit_value("Type a date/time", 1, $Shongo::Common::DateTimePattern, $start);
            $duration = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $duration);
            $self->{'slot'} = $start . '/' . $duration;
            return undef;
        },
        'Modify specification' => sub {
            if ( !defined($self->{'specification'}) ) {
                $self->{'specification'} = Shongo::Controller::API::Specification::create();
            } else {
                $self->{'specification'}->modify();
            }
            return undef;
        }
    ));
}

#
# Format report
#
sub format_report($)
{
    my ($report) = @_;
    my $max_line_width = get_term_width() - 6;
    my @lines = split("\n", $report);
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

    return $report;
}

# @Override
sub to_string_name
{
    return "Reservation Request";
}

# @Override
sub to_string_attributes
{
    my ($self) = @_;
    var_dump($self);

    my $string = Shongo::Controller::API::ReservationRequestAbstract::to_string_attributes(@_);
    $string .= sprintf(" Requested Slot: %s\n", format_interval($self->{'slot'}));
    $string .= " Specification:\n";
    if ( defined($self->{'specification'}) ) {
        $string .= $self->{'specification'}->to_string();
    } else {
        $string .= " -- None -- \n";
    }

    return $string;
}

1;