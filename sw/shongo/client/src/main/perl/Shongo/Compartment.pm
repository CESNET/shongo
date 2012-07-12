#
# Reservation request
#
package Shongo::Compartment;

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Resource;
use Switch;

#
# Get count of requested resources in compartment
#
sub get_resources_count()
{
    my ($self) = @_;
    return scalar(@{$self->{'resources'}});
}

#
# Get count of requested persons in compartment
#
sub get_persons_count()
{
    my ($self) = @_;
    return scalar(@{$self->{'persons'}});
}

#
# Create a new instance of compartment
#
# @static
#
sub create
{
    my $class = shift;
    my $self = {};
    bless $self, $class;

    $self->{'resources'} = [];
    $self->{'persons'} = [];

    if ( $self->modify_loop() ) {
        return $self;
    }
    return undef;
}

#
# Modify compartment
#
sub modify()
{
    my ($self) = @_;

    $self->modify_loop();
}

#
# Run modify loop
#
sub modify_loop()
{
    my ($self, $message) = @_;

    while ( 1 ) {
        printf("\n%s\n", $self->to_string());


        my $actions = [];
        push($actions, 'resource-new' => 'Add new requested resource');
        if ( $self->get_resources_count() > 0 ) {
            #push($actions, 'resource-modify' => 'Modify existing requested resource');
            push($actions, 'resource-remove' => 'Remove existing requested resource');
        }
        push($actions, 'person-new' => 'Add new requested person');
        if ( $self->get_persons_count() > 0 ) {
            #push($actions, 'person-modify' => 'Modify existing requested person');
            push($actions, 'person-remove' => 'Remove existing requested person');
        }
        push($actions, 'stop' => 'Finish modifying compartment');

        my $action = console_select('Select action', ordered_hash_ref($actions));
        switch ( $action ) {
            case 'resource-new' {
                my $technology = console_select("Select technology", \%Shongo::Resource::Technology);
                my $count = console_read("Count", 1, "\\d");
                if ( defined($technology) && defined($count) ) {
                    push($self->{'resources'}, {'technology' => $technology, 'count' => $count});
                }
            }
            case 'resource-remove' {
                my $index = console_read_choice("Type a number of requested resource", $self->get_resources_count());
                if ( defined($index) ) {
                    splice($self->{'resources'}, $index - 1, 1);
                }
            }
            case 'person-new' {
                my $name = console_read("Name");
                my $email = console_read("Email");
                if ( defined($name) && defined($email) ) {
                    push($self->{'persons'}, {'name' => $name, 'email' => $email});
                }
            }
            case 'person-remove' {
                my $index = console_read_choice("Type a number of requested person", $self->get_persons_count());
                if ( defined($index) ) {
                    splice($self->{'persons'}, $index - 1, 1);
                }
            }
            else {
                return 1;
            }
        }
    }
}

#
# Convert object to string
#
sub to_string()
{
    my ($self) = @_;

    my $string = " COMPARTMENT\n";
    $string .= " Requested resources:\n";
    if ( $self->get_resources_count() > 0) {
        for ( my $index = 0; $index < $self->get_resources_count(); $index++ ) {
            my $resource = $self->{'resources'}->[$index];
            $string .= sprintf("   %d) Technology: %s, Count: %d\n", $index + 1,
                $Shongo::Resource::Technology{$resource->{'technology'}}, $resource->{'count'});
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    $string .= " Requested persons:\n";
    if ( $self->get_persons_count() > 0) {
        for ( my $index = 0; $index < $self->get_persons_count(); $index++ ) {
            my $person = $self->{'persons'}->[$index];
            $string .= sprintf("   %d) Name: %s, Email: %s\n", $index + 1,
                $person->{'name'}, $person->{'email'});
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;