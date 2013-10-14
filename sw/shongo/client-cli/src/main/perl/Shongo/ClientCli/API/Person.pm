#
# Person
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::Person;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

our $Type = ordered_hash(
    'UserPerson' => 'Person with user account',
    'AnonymousPerson' => 'Person without user account'
);

#
# Create a new instance of person
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return specification class
#
sub select_type($)
{
    my ($type) = @_;

    return console_edit_enum('Select type of person', $Type, $type);
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    my $person = $attributes->{'class'};
    if ( !defined($person) || $person eq 'Person' ) {
        $person = $self->select_type();
    }
    if ( defined($person) ) {
        $self->set_object_class($person);
    }
}

# @Override
sub on_init()
{
    my ($self) = @_;

    my $class = $self->get_object_class();

    if ( !defined($class) ) {
        return;
    }

    if ( exists $Type->{$class} ) {
        $self->set_object_name($Type->{$class});
    }

    switch ($class) {
        case 'UserPerson' {
            $self->add_attribute('userId', {
                'title' => 'User',
                'format' => sub { return Shongo::ClientCli->instance()->format_user(@_, 1); },
                'modify' => sub {
                    my ($attribute_value) = @_;
                    while ( 1 ) {
                        $attribute_value = console_edit_value('User identifier', 1, $Shongo::Common::UserIdPattern, $attribute_value);
                        if ( Shongo::ClientCli->instance()->user_exists($attribute_value) ) {
                            return $attribute_value;
                        }
                    }
                }
            });
        }
        case 'AnonymousPerson' {
            $self->add_attribute('name', {'title' => 'Full Name'});
            $self->add_attribute('email');
        }
    }
}

1;