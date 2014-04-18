<?php

$serverUrl = "https://shongo-auth-dev.cesnet.cz/rc/";
$serverUrl = "https://shongo-auth.cesnet.cz/";

$clientId = "meetings-migration.cesnet.cz";
$clientSecret = "03431c7bd21595c078163cff59fa0901";
$redirectUri = "https://meetings.cesnet.cz:8080/";

if (array_key_exists("code", $_GET)) {
    header('Content-Type: text/html; charset=utf-8');

    $code = $_GET["code"];

    // Get access token
    $url = $serverUrl . "authn/oic/token";
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
    $url = $serverUrl . "authn/oic/userinfo";
    $request = new HttpRequest($url, HttpRequest::METH_GET);
    $request->setHeaders(array(
        "Authorization" => "Bearer " . $accessToken
    ));
    $data = json_decode($request->send()->getBody());
    echo "<html>";
    echo "<head>";
    echo "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>";
    echo "<title>Rezervační systém</title>";
    echo "</head>";
    echo "<body>";
    echo "<h1 style='color: green;'>Vaše identita je nyní dostupná pro novou verzi rezervačního systému, děkujeme.</h1>";
    echo "<table>";
    echo "<tr><td style='text-align: right;'><strong>Jméno:</strong></td><td>" . $data->first_name . " " . $data->last_name . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Email:</strong></td><td>" . $data->mail . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Identita:</strong></td><td>" . $data->original_id . "</td></tr>";
    echo "<tr><td style='text-align: right;'><strong>Identifikátor:</strong></td><td>" . $data->id . " (přidělený identifikátor v systému <a href='https://einfra.cesnet.cz/perun-gui/' target='_blank'>Perun</a>)</td></tr>";
    echo "</table>";
    echo "<h2>Tuto stránku můžete opustit.</h2>";
    echo "</body>";
    echo "</html>";
}
else if (array_key_exists("error", $_GET)) {
    echo "<html>";
    echo "<head>";
    echo "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>";
    echo "<title>Rezervační systém</title>";
    echo "</head>";
    echo "<body>";
    echo "<h1>Nastala chyba</h1>";
    echo @$_GET["error"];
    echo "<br/>";
    echo @$_GET["error_description"];
    echo "</body>";
    echo "</html>";
}
else {
    $url = $serverUrl . "authn/oic/authorize?" . http_build_query(array(
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