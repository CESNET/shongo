#
# ValueProvider for devices providing virtual rooms
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::ClientCli::API::ValueProvider;
use base qw(Shongo::ClientCli::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;

#
# Value provider types
#
our $Type = ordered_hash(
    'ValueProvider.Pattern' => 'Pattern Value Provider',
    'ValueProvider.Filtered' => 'Filtered Value Provider',
);
our $FilterType = ordered_hash(
    'CONVERT_TO_URL' => 'Convert To Url',
);

#
# Create a new instance of value provider
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::ClientCli::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_name('Value Provider');

    return $self;
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
        case 'ValueProvider.Pattern' {
            $self->add_attribute('patterns', {
                'type' => 'collection',
                'item' => {
                    'title' => 'Pattern',
                    'add' => sub {
                        console_read_value('Pattern', 1);
                    }
                },
                'display-empty' => 1,
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('allowAnyRequestedValue', {
                'title' => 'Allow Any Requested Value',
                'type' => 'bool'
            });
        }
        case 'ValueProvider.Filtered' {
            $self->add_attribute('filterType', {
                'required' => 1,
                'type' => 'enum',
                'enum' =>  $FilterType
            });
            $self->add_attribute('valueProvider', {
                'title' => 'Value Provider',
                'complex' => 1,
                'format' => sub {
                    my ($valueProvider) = @_;
                    if ( ref($valueProvider) ) {
                        return $valueProvider->to_string();
                    }
                    else {
                        return $valueProvider;
                    }
                },
                'modify' => sub {
                    my ($attribute_value) = @_;
                    my $valueProvider = undef;
                    if ( ref($attribute_value) ) {
                        $valueProvider = $attribute_value->{'class'};
                    }
                    elsif ( defined($attribute_value) ) {
                        $valueProvider = NULL();
                    }

                    $valueProvider = console_edit_enum('Select type of value provider',
                        ordered_hash_merge({ NULL() => 'Other Resource' }, $Type),
                        $valueProvider
                    );
                    if ( $valueProvider eq NULL() ) {
                        $attribute_value = console_edit_value('Resource identifier', 1, $Shongo::Common::IdPattern, $attribute_value);
                    } else {
                        if ( !defined($attribute_value) || !ref($attribute_value) ) {
                            $attribute_value = Shongo::ClientCli::API::ValueProvider->create({'class' => $valueProvider});
                        }
                        else {
                            $attribute_value->modify();
                        }
                    }
                    return $attribute_value;
                },
                'required' => 1
            });
        }
    }
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    return console_read_enum('Select type of value provider', $Type, $attributes->{'class'});
}

1;