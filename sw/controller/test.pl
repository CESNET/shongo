#!/usr/bin/perl

require RPC::XML;
require RPC::XML::Client;

$client = RPC::XML::Client->new('http://localhost:8008');

$response = $client->send_request(
    'Reservations.createReservation',
    RPC::XML::struct->new(
        'class' => RPC::XML::string->new('SecurityToken')
    ),
    RPC::XML::struct->new(
        'class' => RPC::XML::string->new('Reservation'),
        'type' => RPC::XML::string->new('OneTime'),
        'date' => RPC::XML::struct->new(
            'class' => RPC::XML::string->new('Date'),
            'date' => RPC::XML::string->new('20120101'),
            'end' => RPC::XML::string->new('20121231')
        )
    )
);


if ( $response->is_fault() ) {
    print "Fault: " . $response->string . " (code " . $response->code . ")\n";
} else {
    use XML::Twig;
    use XML::Parser;
    $xml = XML::Twig->new(pretty_print => 'indented');
    $xml->parse($response->as_string());
    $xml->print();
}

