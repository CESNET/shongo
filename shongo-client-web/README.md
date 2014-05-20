#SHONGO WEB CLIENT

##TECHNOLOGY

* client is written in Java programming language
* Spring MVC web framework is used
* Jetty embedded http server is used for running the client

##RUN

Configure IntelliJ IDEA to run "cz.cesnet.shongo.client.web.ClientWeb.main". If you put "client-web/src/main/resources"
in "Program arguments" then the Jetty will be configured to use all resources (JSP, .properties, etc.)
from the source folder ("client-web/src/main/resources") and you can apply, e.g., runtime modifications to JSPs.
Otherwise it uses the resources from "client-web/target/classes" and you won't be able to apply runtime modifications
and you have to restart application every time you make a change.

##CONVERT KEY/CERT PAIR TO JAVE KEY STORE

      openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12
      keytool -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 -destkeystore server.keystore

##APACHE SSL

Apache configuration:

    SSLEngine on
    SSLCertificateFile      /etc/ssl/certs/<domain>.cert.pem
    SSLCertificateKeyFile   /etc/ssl/private/<domain>.keynopass.pem
    SSLCertificateChainFile /etc/ssl/certs/<domain>.cert.withcabundle.pem

##APACHE PROXY

Apache configuration:

    LoadModule proxy_module modules/mod_proxy.so
    LoadModule proxy_http_module modules/mod_proxy_http.so
    ProxyRequests Off
    <Proxy *>
        Order deny,allow
        Allow from all
    </Proxy>
    ProxyPass /storage !
    ProxyPass / http://localhost:8182/ retry=0
    ProxyStatus On

    Alias /storage /var/www/shongo_storage

Client-web configuration:

    <server>
        <port>8182</port>
        <forwarded>true</forwarded>
    </server>

##RECAPTCHA

* Admin URL: https://www.google.com/recaptcha/admin/site?siteid=317126999
* Domain Name: shongo.cz                                (This is a global key. It will work across all domains.)
* Public Key: 6LdX-eYSAAAAAMRJEuXs5zODFzMhKCd1mRvnasej  (Use this in the JavaScript code that is served to your users.)
* Private Key: 6LdX-eYSAAAAAPbnOJ4wHOJqp1YHwZX_WRnb5HIr (Use this when communicating between your server and our server. Be sure to keep it a secret.)

##CSS

For building css it is necessary to have less compiler.

Install instructions for node.js and less compiler:

    sudo apt-get update && apt-get install git-core curl build-essential openssl libssl-dev

    git clone https://github.com/joyent/node.git
    cd node
    git checkout v0.10.28
    ./configure --openssl-libpath=/usr/lib/ssl
    make
    make test
    sudo make install

    npm install -g less
