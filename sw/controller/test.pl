#!/usr/bin/perl

require RPC::XML;
require RPC::XML::Client;

$client = RPC::XML::Client->new('http://localhost:8008');

$response = $client->send_request(
    'Reservations.modifyReservation',
    RPC::XML::struct->new(
        'class' => RPC::XML::string->new('SecurityToken')
    ),
    RPC::XML::string->new('15082783-5b6f-4287-9015-3dbc0ab2f0d9'),
    RPC::XML::struct->new(
        'class' => RPC::XML::string->new('AttributeMap:Reservation'),
        'description' => RPC::XML::struct->new(),
    )
);


#'id' => RPC::XML::string->new(''),

if ( ref($response) ) {
    use XML::Twig;
    $xml = XML::Twig->new(pretty_print => 'indented');
    $xml->parse($response->as_string());
    $xml->print();
} else {
    print($response . "\n");
}

