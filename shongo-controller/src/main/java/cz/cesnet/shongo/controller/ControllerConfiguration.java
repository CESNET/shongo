package cz.cesnet.shongo.controller;

import com.google.common.base.Strings;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.settings.UserSessionSettings;
import cz.cesnet.shongo.ssl.SSLCommunication;
import cz.cesnet.shongo.util.PatternParser;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.postgresql.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Configuration for the {@link Controller}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerConfiguration extends CombinedConfiguration
{
    /**
     * Time-zone in which the controller works and which is considered as default for date/times without specific zone.
     */
    public static final String TIMEZONE = "timezone";

    /**
     * Domain configuration.
     */
    public static final String DOMAIN_NAME = "domain.name";
    public static final String DOMAIN_SHORT_NAME = "domain.shortName";
    public static final String DOMAIN_ORGANIZATION = "domain.organization";

    /**
     * Database configuration.
     */
    public static final String DATABASE_DRIVER = "database.driver";
    public static final String DATABASE_URL = "database.url";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_PASSWORD = "database.password";

    /**
     * XML-RPC configuration
     */
    public static final String RPC_HOST = "rpc.host";
    public static final String RPC_PORT = "rpc.port";

    /**
     * Jade configuration.
     */
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";
    public static final String JADE_AGENT_NAME = "jade.agent-name";
    public static final String JADE_PLATFORM_ID = "jade.platform-id";

    /**
     * REST api configuration
     */
    public static final String REST_API = "rest-api";
    public static final String REST_API_HOST = "rest-api.host";
    public static final String REST_API_PORT = "rest-api.port";
    public static final String REST_API_SSL_KEY_STORE = REST_API + ".ssl-key-store";
    public static final String REST_API_SSL_KEY_STORE_TYPE = REST_API + ".ssl-key-store-type";
    public static final String REST_API_SSL_KEY_STORE_PASSWORD = REST_API + ".ssl-key-store-password";

    /**
     * Interdomains configuration
     */
    public static final String INTERDOMAIN = "domain.inter-domain-connection";
    public static final String INTERDOMAIN_PKI_CLIENT_AUTH = INTERDOMAIN + ".pki-client-auth";
    public static final String INTERDOMAIN_TRUSTED_CA_CERT_FILES = INTERDOMAIN + ".ssl-trust-store.ca-certificate";
    public static final String INTERDOMAIN_COMMAND_TIMEOUT = INTERDOMAIN + ".command-timeout";
    public static final String INTERDOMAIN_CACHE_REFRESH_RATE = INTERDOMAIN + ".cache-refresh-rate";
    public static final String INTERDOMAIN_BASIC_AUTH_PASSWORD = INTERDOMAIN + ".basic-auth.password";

    /**
     * Worker configuration (it runs scheduler and executor).
     */
    public static final String WORKER_PERIOD = "worker.period";
    public static final String WORKER_LOOKAHEAD = "worker.lookahead";

    /**
     * Maximum duration of reservations.
     */
    public static final String RESERVATION_ROOM_MAX_DURATION = "reservation.room.max-duration";

    /**
     * SMTP configuration.
     */
    public static final String SMTP_SENDER = "smtp.sender";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";

    /**
     * CalDAV connector configuration.
     */
    public static final String CALDAV_URL = "caldav-connector.url";
    public static final String CALDAV_BASIC_AUTH_USERNAME = "caldav-connector.basic-auth.username";
    public static final String CALDAV_BASIC_AUTH_PASSWORD = "caldav-connector.basic-auth.password";



    /**
     * Period in which the executor works.
     */
    public static final String EXECUTOR_PERIOD = "executor.period";

    /**
     * Duration to modify {@link Executable} starting date/time.
     */
    public static final String EXECUTOR_EXECUTABLE_START = "executor.executable.start";

    /**
     * Duration to modify {@link Executable} ending date/time.
     */
    public static final String EXECUTOR_EXECUTABLE_END = "executor.executable.end";

    /**
     * Period in which {@link cz.cesnet.shongo.controller.executor.Executor} try to perform failed action again.
     */
    public static final String EXECUTOR_EXECUTABLE_NEXT_ATTEMPT = "executor.executable.next-attempt";

    /**
     * Maximum count of attempts for {@link cz.cesnet.shongo.controller.executor.Executor} to try to perform action.
     */
    public static final String EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT = "executor.executable.max-attempt-count";

    /**
     * Duration which {@link cz.cesnet.shongo.controller.executor.Executor} waits for virtual rooms to be created.
     */
    public static final String EXECUTOR_STARTING_DURATION_ROOM = "executor.starting-duration.room";

    /**
     * URL to AA server.
     */
    public static final String SECURITY_SERVER = "security.server";

    /**
     * Client id for AA server.
     */
    public static final String SECURITY_CLIENT_ID = "security.client-id";

    /**
     * Client secret for AA server.
     */
    public static final String SECURITY_CLIENT_SECRET = "security.client-secret";

    /**
     * URL to LDAP AA server.
     */
    public static final String SECURITY_LDAP_SERVER = "security.ldap.server";

    /**
     * Client secret for LDAP AA server.
     */
    public static final String SECURITY_LDAP_CLIENT_SECRET = "security.ldap.client-secret";

    /**
     * Client secret for LDAP AA server.
     */
    public static final String SECURITY_LDAP_BINDDN = "security.ldap.binddn";

    /**
     * Specifies expiration of cache for user-id by access-token.
     */
    public static final String SECURITY_EXPIRATION_USER_ID = "security.expiration.user-id";

    /**
     * Specifies expiration of cache for user information by user-id.
     */
    public static final String SECURITY_EXPIRATION_USER_INFORMATION = "security.expiration.user-information";

    /**
     * Specifies expiration of cache for user ACL by user-id.
     */
    public static final String SECURITY_EXPIRATION_ACL = "security.expiration.acl";

    /**
     * Specifies expiration of cache for user groups.
     */
    public static final String SECURITY_EXPIRATION_GROUP = "security.expiration.group";

    /**
     * Specifies filename where the root access token will be written when controller starts.
     */
    public static final String SECURITY_ROOT_ACCESS_TOKEN_FILE = "security.root-access-token";

    /**
     * Specifies expression which decides whether user is a system administrator
     * (they can use the {@link UserSessionSettings#administrationMode}).
     */
    public static final String SECURITY_AUTHORIZATION_ADMINISTRATOR = "security.authorization.administrator";

    /**
     * Specifies expression which decides whether user is a system operator
     * (they can use the {@link UserSessionSettings#administrationMode}).
     */
    public static final String SECURITY_AUTHORIZATION_OPERATOR = "security.authorization.operator";

    /**
     * Specifies expression which decides whether user can create a reservation request.
     */
    public static final String SECURITY_AUTHORIZATION_RESERVATION = "security.authorization.reservation";

    /**
     * Url where user can change his settings.
     */
    public static final String NOTIFICATION_USER_SETTINGS_URL = "notification.user-settings-url";

    /**
     * Primary url of a reservation requests with "${reservationRequestId}" variable which can be used in notifications.
     */
    public static final String NOTIFICATION_RESERVATION_REQUEST_URL = "notification.reservation-request-url";


    /**
     * Primary url of a reservation requests with "${reservationRequestId}" variable which can be used in notifications.
     */
    public static final String NOTIFICATION_RESERVATION_REQUEST_CONFIRMATION_URL = "notification.reservation-request-confirmation-url";

    /**
     * Filepath for FreePBX PDF guide.
     */
    public static final String FREEPBX_PDF_GUIDE_FILEPATH = "notification.freepbx-guide-filepath";

    /**
     * Constructor.
     */
    public ControllerConfiguration()
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("email");
        setNodeCombiner(nodeCombiner);
    }

    /**
     * @see {@link #getString(String)}
     */
    public Duration getDuration(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value).toStandardDuration();
    }

    /**
     * @see {@link #getString(String)}
     */
    public Period getPeriod(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value);
    }

    /**

     * @return timeout to receive response when performing commands from agent
     */
    public Duration getJadeCommandTimeout()
    {
        return getDuration("jade.command-timeout");
    }

    /**
     * @return XML-RPC host
     */
    public String getRpcHost(boolean nullAsDefault)
    {
        String rpcHost = getString(RPC_HOST);
        if (rpcHost.isEmpty()) {
            if (nullAsDefault) {
                rpcHost = null;
            }
            else {
                rpcHost = "localhost";
            }
        }
        return rpcHost;
    }

    /**
     * @return XML-RPC port
     */
    public int getRpcPort()
    {
        return getInt(RPC_PORT);
    }

    /**
     * @return XML-RPC url
     */
    public URL getRpcUrl() throws MalformedURLException {
        int rpcPort;

        try {
            rpcPort = getRpcPort();
        } catch (NoSuchElementException e) {
            rpcPort = 8181;
        }
        String urlString = String.format("http://%s:%d", getRpcHost(false), rpcPort);
        return new URL(urlString);
    }

    /**
     * @return XML-RPC ssl key store
     */
    public String getRpcSslKeyStore()
    {
        String sslKeyStore = getString("rpc.ssl-key-store");
        if (sslKeyStore == null || sslKeyStore.trim().isEmpty()) {
            return null;
        }
        return sslKeyStore;
    }

    /**
     * @return password for XML-RPC ssl key store
     */
    public String getRpcSslKeyStorePassword()
    {
        return getString("rpc.ssl-key-store-password");
    }

    /**
     * @return subject prefix for emails sent by SMTP
     */
    public String getSmtpSubjectPrefix()
    {
        return evaluate(getString("smtp.subject-prefix"));
    }

    /**
     * @return {@link #NOTIFICATION_RESERVATION_REQUEST_URL}
     */
    public String getNotificationReservationRequestUrl()
    {
        String reservationRequestUrl = getString(NOTIFICATION_RESERVATION_REQUEST_URL);
        if (reservationRequestUrl == null || reservationRequestUrl.isEmpty()) {
            return null;
        }
        return reservationRequestUrl;
    }

    /**
     * @return {@link #NOTIFICATION_RESERVATION_REQUEST_CONFIRMATION_URL}
     */
    public String getNotificationReservationRequestConfirmationUrl()
    {
        String reservationRequestConfirmationUrl = getString(NOTIFICATION_RESERVATION_REQUEST_CONFIRMATION_URL);
        if (reservationRequestConfirmationUrl == null || reservationRequestConfirmationUrl.isEmpty()) {
            return null;
        }
        return reservationRequestConfirmationUrl;
    }

    /**
     * @return {@link #FREEPBX_PDF_GUIDE_FILEPATH}
     */
    public String getFreePBXPDFGuidePath()
    {
        String FreePBXGuidePath = getString(FREEPBX_PDF_GUIDE_FILEPATH);
        if (FreePBXGuidePath == null || FreePBXGuidePath.isEmpty()) {
            return null;
        }
        return FreePBXGuidePath;
    }

    /**
     * @return {@link #NOTIFICATION_USER_SETTINGS_URL}
     */
    public String getNotificationUserSettingsUrl()
    {
        String reservationRequestUrl = getString(NOTIFICATION_USER_SETTINGS_URL);
        if (reservationRequestUrl == null || reservationRequestUrl.isEmpty()) {
            return null;
        }
        return reservationRequestUrl;
    }

    /**
     * List of administrators.
     */
    private List<PersonInformation> administrators;

    /**
     * @return set of administrators to which errors are reported.
     */
    public synchronized List<PersonInformation> getAdministrators()
    {
        if (administrators == null) {
            administrators = new LinkedList<PersonInformation>();
            for (Object item : getList("administrator")) {
                final String administratorEmail = (String) item;
                administrators.add(new PersonInformation()
                {
                    @Override
                    public String getFullName()
                    {
                        return "administrator";
                    }

                    @Override
                    public String getRootOrganization()
                    {
                        return null;
                    }

                    @Override
                    public String getPrimaryEmail()
                    {
                        return administratorEmail;
                    }

                    @Override
                    public String toString()
                    {
                        return getFullName();
                    }
                });
            }
        }
        return administrators;
    }

    /**
     * @return set of administrator emails to which errors are reported.
     */
    public synchronized List<String> getAdministratorEmails()
    {
        List<String> administratorEmails = new LinkedList<String>();
        for (PersonInformation administrator : getAdministrators()) {
            administratorEmails.add(administrator.getPrimaryEmail());
        }
        return administratorEmails;
    }

    /**
     * @param administrators sets the {@link #administrators}
     */
    public synchronized void setAdministrators(List<PersonInformation> administrators)
    {
        this.administrators = administrators;
    }

    /**
     * Pattern for parameters.
     */
    private static final Pattern EXPRESSION_PARAM_PATTERN = Pattern.compile("\\$\\{([^\\$]+)\\}");

    /**
     * Parser for parameters.
     */
    private static final PatternParser EXPRESSION_PATTERN_PARSER = new PatternParser(EXPRESSION_PARAM_PATTERN);

    /**
     * @param text
     * @return evaluated string
     */
    public String evaluate(String text)
    {
        if (text == null) {
            return null;
        }
        return EXPRESSION_PATTERN_PARSER.parseAndJoin(text, new PatternParser.Callback()
        {
            @Override
            public String processString(String string)
            {
                return string;
            }

            @Override
            public String processMatch(MatchResult match)
            {
                String name = match.group(1);
                if (name.equals("domain.name")) {
                    return LocalDomain.getLocalDomain().getName();
                }
                else if (name.equals("domain.shortName")) {
                    return LocalDomain.getLocalDomain().getShortName();
                }
                else {
                    throw new IllegalArgumentException("Parameter " + name + " not defined.");
                }
            }
        });
    }

    public boolean isInterDomainConfigured()
    {
        if (requiresClientPKIAuth() && hasRESTApiPKI()) {
            return true;
        }
        return hasInterDomainBasicAuth();
    }

    public boolean hasInterDomainBasicAuth()
    {
        return !Strings.isNullOrEmpty(getInterDomainBasicAuthPasswordHash());
    }

    public String getRESTApiHost()
    {
        String host = getString(ControllerConfiguration.REST_API_HOST);
        return host != null ? host : "localhost";
    }

    public Integer getRESTApiPort()
    {
        Integer port = getInteger(ControllerConfiguration.REST_API_PORT, null);
        return port != null ? port : 9999;
    }

    public String getRESTApiSslKeyStore()
    {
        String sslKeyStore = getString(ControllerConfiguration.REST_API_SSL_KEY_STORE);
        if (sslKeyStore == null || sslKeyStore.trim().isEmpty()) {
            return null;
        }
        return sslKeyStore;
    }

    public String getRESTApiSslKeyStoreType() {
        return getString(ControllerConfiguration.REST_API_SSL_KEY_STORE_TYPE);
    }

    public String getRESTApiSslKeyStorePassword() {
        return getString(ControllerConfiguration.REST_API_SSL_KEY_STORE_PASSWORD);
    }

    public boolean hasRESTApiPKI()
    {
        if (Strings.isNullOrEmpty(getRESTApiSslKeyStore())
                || Strings.isNullOrEmpty(getRESTApiSslKeyStoreType())
                || Strings.isNullOrEmpty(getRESTApiSslKeyStorePassword())) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if PKI auth is selected to be used
     * @return
     */
    public boolean requiresClientPKIAuth()
    {
        return getBoolean(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH, false);
    }

    public String getInterDomainBasicAuthPasswordHash()
    {
        String password = getString(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD);
        if (Strings.isNullOrEmpty(password)) {
            return null;
        }
        return SSLCommunication.hashPassword(password.getBytes());
    }

    public List<String> getForeignDomainsCaCertFiles() {
        List<String> caCertFiles = new ArrayList<String>();
        for (Object o : getList(ControllerConfiguration.INTERDOMAIN_TRUSTED_CA_CERT_FILES)) {
            caCertFiles.add((String) o);
        }
        return caCertFiles;
    }

    public int getInterDomainCommandTimeout() {
        return (int) getDuration(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT).getMillis();
    }

    public Integer getInterDomainCacheRefreshRate() {
        return getDuration(ControllerConfiguration.INTERDOMAIN_CACHE_REFRESH_RATE).toStandardSeconds().getSeconds();
    }

    public boolean hasCalDAVBasicAuth()
    {
        if (Strings.isNullOrEmpty(getCalDAVEncodedBasicAuth())) {
            return false;
        }
        return true;
    }

    public String getCalDAVEncodedBasicAuth()
    {
        String username = getString(ControllerConfiguration.CALDAV_BASIC_AUTH_USERNAME);
        String password = getString(ControllerConfiguration.CALDAV_BASIC_AUTH_PASSWORD);
        if (Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(username)) {
            return null;
        }
        String authString = username + ":" + password;
        String authStringEnc = Base64.encodeBytes(authString.getBytes(StandardCharsets.UTF_8));
        return authStringEnc;
    }
}
