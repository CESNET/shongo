#
# Reservation request
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

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
            my $specification = undef;
            if ( defined($self->{'specification'}) ) {
                $specification = $self->{'specification'}->{'class'};
            }
            $specification = Shongo::Controller::API::Specification::select_type($specification);
            if ( !defined($self->{'specification'}) || !($specification eq $self->{'specification'}->{'class'}) ) {
                $self->{'specification'} = Shongo::Controller::API::Specification->create($specification);
            } else {
                $self->{'specification'}->modify();
            }
            return undef;
        }
    ));
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'specification' ) {
        return Shongo::Controller::API::Specification->new($class);
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

#
# @return state
#
sub get_state
{
    my ($self) = @_;
    if ( !defined($self->{'state'}) ) {
        return undef;
    }
    my $state = $State->{$self->{'state'}};
    if ( $self->{'state'} eq 'NOT_COMPLETE' ) {
        $state = colored($state, 'yellow')
    }
    elsif ( $self->{'state'} eq 'ALLOCATED' ) {
        $state = colored($state, 'green')
    }
    elsif ( $self->{'state'} eq 'ALLOCATION_FAILED' ) {
        $state = colored($state, 'red')
    }
    else {
        $state = colored($state, 'blue');
    }
    return '[' . $state . ']';
}

#
# @return report
#
sub get_state_report
{
    my ($self) = @_;
    my $color = 'blue';
    if ( defined($self->{'state'}) && $self->{'state'} eq 'ALLOCATION_FAILED' ) {
        $color = 'red';
    }
    my $stateReport = $self->{'stateReport'};
    my $max_line_width = get_term_width() - 23;
    my @lines = ();
    if ( defined($stateReport) ) {
        @lines = split("\n", $stateReport);
    }
    $stateReport = '';
    # Process each line from report
    for ( my $index = 0; $index < scalar(@lines); $index++ ) {
        my $line = $lines[$index];
        my $nextLine = $lines[$index + 1];
        if ( length($stateReport) > 0 ) {
            $stateReport .= "\n";
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
            $stateReport .= substr($line, 0, $index);
            $stateReport .= "\n". $indent;
            $line = substr($line, $index + 1);
        }

        # Append the rest
        $stateReport .= $line;
    }
    if ( !defined($stateReport) || $stateReport eq '' ) {
        $stateReport = "-- No report --";
    }
    return colored($stateReport, $color);
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Reservation Request";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add'}('Requested Slot', format_interval($self->{'slot'}));
    $attributes->{'add'}('Specification', $self->{'specification'});

    my $state = $self->get_state();
    if ( $self->{'state'} eq 'ALLOCATED' ) {
        $state .= sprintf(" (" . colored("reservation", $Shongo::Controller::API::Object::COLOR) . ": %s)", $self->{'reservationIdentifier'});
    }
    $attributes->{'add'}('Current State', $state, $self->get_state_report());
}

1;