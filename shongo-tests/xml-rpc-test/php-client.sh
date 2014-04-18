#!/usr/bin/php
<?
    $curl = curl_init();

    curl_setopt_array($curl, array(
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_VERBOSE => true,
        CURLOPT_HEADER => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_FORBID_REUSE => false,
        CURLOPT_HTTPHEADER => array("Connection: keep-alive"),
    ));

    curl_setopt_array($curl, array(
        //CURLOPT_URL => "http://127.0.0.1:9090",
        //CURLOPT_URL => "https://mcuc.cesnet.cz/RPC2",
        CURLOPT_URL => "http://mcu2.arnes.si/RPC2",
        CURLOPT_POSTFIELDS => "<?xml version=\"1.0\"?>
            <methodCall>
                <methodName>conference.status</methodName>
                <params>
                    <param><value><struct>
                        <member>
                            <name>authenticationUser</name>
                            <value><string>janr</string></value>
                        </member>
                        <member>
                            <name>authenticationPassword</name>
                            <value><string></string></value>
                        </member>
                        <member>
                            <name>conferenceName</name>
                            <value><string>test123Cesnet</string></value>
                        </member>
                    </struct></value></param>
                </params>
            </methodCall>
        "
    ));

    // Send request every 10s
    for ( $index = 1; $index <= 14; $index++ ) {
        $response = curl_exec($curl);
        if ( $response === false) {
            die('ERROR: ' . curl_error($curl) . PHP_EOL);
        }
        echo PHP_EOL . 'RESPONSE ' . $index . ':' . PHP_EOL . PHP_EOL;
        //echo $response . PHP_EOL;

        sleep(10);
    }

    // Send request every 20s
    for ( $index = 1; $index <= 8; $index++ ) {
        $response = curl_exec($curl);
        if ( $response === false) {
            die('ERROR: ' . curl_error($curl) . PHP_EOL);
        }
        echo PHP_EOL . 'RESPONSE ' . $index . ':' . PHP_EOL . PHP_EOL;
        //echo $response . PHP_EOL;

        sleep(20);
    }

    // Send request every 30s
    for ( $index = 1; $index <= 6; $index++ ) {
        $response = curl_exec($curl);
        if ( $response === false) {
            die('ERROR: ' . curl_error($curl) . PHP_EOL);
        }
        echo PHP_EOL . 'RESPONSE ' . $index . ':' . PHP_EOL . PHP_EOL;
        //echo $response . PHP_EOL;

        sleep(30);
    }

    curl_close($curl);
?>