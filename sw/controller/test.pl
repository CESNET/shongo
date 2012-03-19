#!/usr/bin/perl

require RPC::XML;
require RPC::XML::Client;

$client = RPC::XML::Client->new('http://localhost:8008');

$response = $client->send_request(
    'Reservation.createReservation', 
     RPC::XML::struct->new(
         'class' => RPC::XML::string->new('Date'),
         'date' => RPC::XML::string->new('20120101'),
         'end' => RPC::XML::string->new('20121231')
     )
);
if ( $response->is_fault() ) {
    print "Fault: " . $response->string . " (code " . $response->code . ")\n";
} else {
    print "Response: " . $response->as_string() . "\n";
}

