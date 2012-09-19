#
# Reservation request
#
package Shongo::Controller::API::CompartmentSpecification;
use base qw(Shongo::Controller::API::Specification);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Resource;

#
# Create a new instance of compartment
#
# @static
#
sub new
{
    my $class = shift;
    my $self = Shongo::Controller::API::Specification->new(@_);
    bless $self, $class;

    $self->{'class'} = 'CompartmentSpecification';
    $self->{'specifications'} = [];

    return $self;
}

#
# Get count of requested resources in compartment
#
sub get_specifications_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'specifications'});
}

# @Override
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
            push(@actions, (
                'Modify attributes' => sub {
                    $self->modify_attributes(1);
                    return undef;
                },
                'Add new specification' => sub {
                    my $specification = Shongo::Controller::API::Specification->create();
                    if ( defined($specification) ) {
                        add_collection_item(\$self->{'specifications'}, $specification);
                    }
                    return undef;
                }
            ));
            if ( $self->get_specifications_count() > 0 ) {
                push(@actions, 'Modify existing specification' => sub {
                    my $index = console_read_choice("Type a number of specification", 0, $self->get_specifications_count());
                    if ( defined($index) ) {
                        get_collection_item($self->{'specifications'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing specification' => sub {
                    my $index = console_read_choice("Type a number of specification", 0, $self->get_specifications_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'specifications'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying specification' => sub {
                return 1;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# Modify attributes
#
sub modify_attributes()
{
    my ($self, $edit) = @_;

    my $callInitiation = $self->{'callInitiation'};
    if ( !defined($callInitiation) ) {
        $callInitiation = 'DEFAULT';
    }
    $callInitiation = console_auto_enum($edit, 'Select call initiation', $Shongo::Controller::API::Specification::CallInitiation, $self->{'callInitiation'});
    if ( $callInitiation eq 'DEFAULT' ) {
        $callInitiation = undef;
    }
    $self->{'callInitiation'} = $callInitiation;
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'specifications' && !($class eq 'CompartmentSpecification')) {
        return Shongo::Controller::API::Specification->new();
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

# @Override
sub to_string_attributes
{
    my ($self) = @_;
    my $string = '';
    my $callInitiation = $self->{'callInitiation'};
    if ( !defined($callInitiation) ) {
        $callInitiation = 'DEFAULT';
    }
    $string .= sprintf(" Call initiation: %s\n", $Shongo::Controller::API::Specification::CallInitiation->{$callInitiation});
    return $string;
}

# @Override
sub to_string_collections
{
    my ($self) = @_;
    my $string = "";
    $string .= " Specifications:\n";
    if ( $self->get_specifications_count() > 0) {
        for ( my $index = 0; $index < $self->get_specifications_count(); $index++ ) {
            my $specification = get_collection_item($self->{'specifications'}, $index);
            $string .= sprintf("   %d) %s", $index + 1, indent_block($specification->to_string_short(), 0, 6));
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;