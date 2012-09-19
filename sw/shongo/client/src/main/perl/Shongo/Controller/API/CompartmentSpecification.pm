#
# Reservation request
#
package Shongo::Controller::API::Compartment;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Resource;
use Shongo::Controller::API::ResourceSpecification;

#
# Create a new instance of compartment
#
# @static
#
sub new
{
    my $class = shift;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->{'resources'} = [];
    $self->{'persons'} = [];

    return $self;
}

#
# Get count of requested resources in compartment
#
sub get_resources_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'resources'});
}

#
# Get count of requested persons in compartment
#
sub get_persons_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'persons'});
}

#
# Create a new instance of compartment
#
# @static
#
sub create
{
    my $self = new(@_);

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

    console_action_loop(
        sub {
            printf("\n%s\n", $self->to_string());
        },
        sub {
            my @actions = ();
            push(@actions, 'Add new requested resource' => sub {
                my $resource = Shongo::Controller::API::ResourceSpecification->new();
                $resource = $resource->create();
                if ( defined($resource) ) {
                    add_collection_item(\$self->{'resources'}, $resource);
                }
                return undef;
            });
            if ( $self->get_resources_count() > 0 ) {
                push(@actions, 'Modify existing requested resource' => sub {
                    my $index = console_read_choice("Type a number of requested resource", 0, $self->get_resources_count());
                    if ( defined($index) ) {
                        get_collection_item($self->{'resources'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing requested resource' => sub {
                    my $index = console_read_choice("Type a number of requested resource", 0, $self->get_resources_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'resources'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Add new requested person' => sub {
                my $name = console_read_value("Name");
                my $email = console_read_value("Email");
                if ( defined($name) && defined($email) ) {
                    add_collection_item(\$self->{'persons'}, {'name' => $name, 'email' => $email});
                }
                return undef;
            });
            if ( $self->get_persons_count() > 0 ) {
                push(@actions, 'Remove existing requested person' => sub {
                    my $index = console_read_choice("Type a number of requested person", 0, $self->get_persons_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'persons'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying compartment' => sub {
                return 1;
            });
            return ordered_hash(@actions);
        }
    );
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'resources' ) {
        return Shongo::Controller::API::ResourceSpecification->new();
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

# @Override
sub to_string()
{
    my ($self) = @_;

    my $string = " COMPARTMENT\n";
    $string .= " Requested resources:\n";
    if ( $self->get_resources_count() > 0) {
        for ( my $index = 0; $index < $self->get_resources_count(); $index++ ) {
            my $resource = get_collection_item($self->{'resources'}, $index);
            $string .= sprintf("   %d) %s", $index + 1, $resource->to_string());
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    $string .= " Requested persons:\n";
    if ( $self->get_persons_count() > 0) {
        for ( my $index = 0; $index < $self->get_persons_count(); $index++ ) {
            my $person = get_collection_item($self->{'persons'}, $index);
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