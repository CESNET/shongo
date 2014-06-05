# Shongo Common

Contains common classes to be used in all other modules.

## JAVA KEYSTORE

Listing certificates in jks:

    keytool -keystore /usr/lib/jvm/java-6-oracle/jre/lib/security/cacerts -storepass "changeit" -list  -v

Add DER certificate to jks:

    wget http://www.terena.org/activities/tcs/repository/TERENA_SSL_CA.der \
        && keytool -keystore /usr/lib/jvm/java-6-oracle/jre/lib/security/cacerts -storepass "changeit" -noprompt -import -alias "TERENA_SSL_CA" -file TERENA_SSL_CA.der \
        ; rm TERENA_SSL_CA.der

Add CRT certificate to keystore:

    echo -n | openssl s_client -connect rec1.cesnet.cz:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/host.crt \
        && keytool -keystore /usr/lib/jvm/java-6-oracle/jre/lib/security/cacerts -storepass "changeit" -noprompt -importcert -alias "rec1.cesnet.cz" -file /tmp/host.crt \
        ; rm /tmp/host.crt

Remove certificate from jks:

    keytool -keystore /usr/lib/jvm/java-6-oracle/jre/lib/security/cacerts -storepass "changeit" -noprompt -delete -alias "TERENA_SSL_CA"

Debugging:

    -Djavax.net.debug=all

Use different keystore:

    -Djavax.net.ssl.trustStore=<keystore>