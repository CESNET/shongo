#!/usr/bin/php
<?
    $curl = curl_init();

    curl_setopt_array($curl, array(
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_FORBID_REUSE => false,
    ));

    curl_setopt_array($curl, array(
        //CURLOPT_URL => "https://mcuc.cesnet.cz/RPC2",
        CURLOPT_URL => "http://127.0.0.1:9090",
        CURLOPT_POSTFIELDS => "<?xml version=\"1.0\"?>
            <methodCall>
                <methodName>conference.status</methodName>
                <params>
                    <param><value><struct>
                        <member>
                            <name>authenticationUser</name>
                            <value><string>shongo</string></value>
                        </member>
                        <member>
                            <name>authenticationPassword</name>
                            <value><string>uracq674q</string></value>
                        </member>
                        <member>
                            <name>conferenceName</name>
                            <value><string>shongo-test</string></value>
                        </member>
                    </struct></value></param>
                </params>
            </methodCall>
        "
    ));

    for ( $index = 1; $index <= 2; $index++ ) {
        sleep(1);
        $response = curl_exec($curl);
        if ( $response === false) {
            die('ERROR: ' . curl_error($curl) . PHP_EOL);
        }
        echo PHP_EOL . 'RESPONSE ' . $index . ':' . PHP_EOL . PHP_EOL;
        echo $response;
        echo PHP_EOL;
    }

    curl_close($curl);
?>