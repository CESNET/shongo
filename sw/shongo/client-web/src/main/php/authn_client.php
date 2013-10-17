<?php

$clientId = "meetings.cesnet.cz";
$clientSecret = "c9izhj0e8ypsxe978r0x2eynvdcz06ro";
$redirectUri = "https://meetings.cesnet.cz/";

if (array_key_exists("code", $_GET)) {
    header('Content-Type: text/html; charset=utf-8');

    $code = $_GET["code"];

    // Get access token
    $url = "https://shongo-auth-dev.cesnet.cz/rc/authn/oic/token";
    $request = new HttpRequest($url, HttpRequest::METH_POST);
    $request->setHeaders(array(
        "Authorization" => "Basic " . base64_encode($clientId . ":" . $clientSecret)
    ));
    $request->addPostFields(array(
        "client_id" => $clientId,
        "redirect_uri" => $redirectUri,
        "grant_type" => "authorization_code",
        "code" => $code,
    ));
    $data = json_decode($request->send()->getBody());
    $accessToken = $data->access_token;

    // Get user info
    $url = "https://shongo-auth-dev.cesnet.cz/rc/authn/oic/userinfo";
    $request = new HttpRequest($url, HttpRequest::METH_GET);
    $request->setHeaders(array(
        "Authorization" => "Bearer " . $accessToken
    ));
    $data = json_decode($request->send()->getBody());
    echo "<h1 style='color: green;'>Vaše identita je nyní dostupná pro rezervační systém, děkujeme.</h1>";
    echo "<table>";
    echo "<tr><td style='text-align: right;'><strong>Identita:</strong></td><td>" . $data->original_id . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Jméno:</strong></td><td>" . $data->given_name . " " . $data->family_name . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Email:</strong></td><td>" . $data->email . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Přidělený identifikátor:</strong></td><td>" . $data->id . "</td></tr>";
    echo "</table>";
    echo "<h2>Tuto stránku můžete zavřít.</h2>";
}
else {
    $url = "https://shongo-auth-dev.cesnet.cz/rc/authn/oic/authorize?" . http_build_query(array(
            "client_id" => $clientId,
            "redirect_uri" => $redirectUri,
            "state" => "324e32ab4c4",
            "scope" => "openid",
            "response_type" => "code",
            "prompt" => "login"
        ));
    header('Location: ' . $url);
}

?>