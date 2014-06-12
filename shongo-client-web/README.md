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

## DESIGNS

Each design should be stored in own folder with name which represents the design name.

### Requirements

Each folder must contain:

* <code>&lt;desing-name&gt;/design.properties</code> - [properties file](http://en.wikipedia.org/wiki/.properties) with english translation messages

* <code>&lt;desing-name&gt;/design_cs.properties</code> - [properties file](http://en.wikipedia.org/wiki/.properties) with czech translation messages

* <code>&lt;desing-name&gt;/layout.ftl</code> - design HTML layout file with [FreeMarker](http://freemarker.org/docs/dgui_template_exp.html) syntax to retrieve variables

* <code>&lt;desing-name&gt;/main.ftl</code> - design HTML main page content

* <code>&lt;desing-name&gt;/css/design.css</code> - design CSS file

Each folder should contain:

* <code>/img/icon.ico</code> - icon for web browsers (@see favicon)
* <code>/img/apple-touch-icon.png</code> - icon for apple touch (@see apple-touch-icon)

In design HTML files <code>/*.ftl</code> you may use:

Functions:

* <code>${message("&lt;message-code&gt;")}</code> - for retrieving translated messages from design properties files
* <code>${escapeJavaScript("&lt;code&gt;")}</code> - for escaping strings for usage in javascript string

Page construction variables:

* <code>${title}</code> - for rendering current page title
* <code>${head}</code> - for rendering current page part of &lt;head&gt; section (CSS/JS imports and common javascript code, the &lt;head&gt; tag is not included)
* <code>${content}</code> - for rendering current page content

Support page variables:

* <code>${app.version}</code> - application version (e.g., *1.2.3*)
* <code>${url.resources}</code> - URL base path for resources (e.g., *${url.resources}/img/logo.png* to access *logo.png* in *&lt;desing-name&gt;/img/* folder)
* <code>${url.changelog}</code> - URL to show changelog
* <code>${url.home}</code> - URL to show main page
* <code>${url.languageCs}</code> - URL to change user language to czech
* <code>${url.languageEn}</code> - URL to change user language to english
* <code>${url.user.login}</code> - URL to redirect user to authentication server
* <code>${url.user.logout}</code> - URL to clear user authentication information
* <code>${url.report}</code> - URL to show report problem page
* <code>${url.userSettings}</code> - URL to show
* <code>${url.userSettingsAdvancedMode(true|false)}</code> - URL to switch on/off the advance user interface
* <code>${url.userSettingsAdministrationMode(true|false)}</code> - URL to switch on/off the administrator mode
* <code>${user}</code> - Object containing user session information
* <code>${user.id}</code> - User id
* <code>${user.name}</code> - Full user name
* <code>${user.advancedMode}</code> - Specifies whether user is in advance user interface
* <code>${user.administrationMode}</code> - Specifies whether user is in administrator mode
* <code>${user.administrationModeAvailable}</code> - Specifies whether user can switch to administrator mode
* <code>${session.locale.title}</code> - Locale title
* <code>${session.locale.language}</code> - Locale language (e.g., *en* or *cs*)
* <code>${session.timezone.title}</code> - Timezone title (e.g., *+01:00*)
* <code>${session.timezone.help}</code> - Timezone description
* <code>${links}</code> - Sequence of <code>Link</code> objects
* <code>${breadcrumbs}</code> - Sequence of <code>Breadcrumb</code> objects

<code>Link</code> object:

* <code>${&lt;link-object&gt;.title}</code> - Link title
* <code>${&lt;link-object&gt;.url}</code> - Link url

<code>Breadcrumb</code> object:

* <code>${&lt;breadcrumb-object&gt;.title}</code> - Breadcrumb title
* <code>${&lt;breadcrumb-object&gt;.url}</code> - Breadcrumb url

### Example

<pre>
cesnet/
cesnet/design.properties
cesnet/design_cs.properties
cesnet/layout.ftl
cesnet/main.ftl
cesnet/css/design.css
cesnet/img/icon.ico
cesnet/img/apple-touch-icon.png
</pre>