package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.settings.UserSessionSettings;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.*;

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
    public static final String SMTP_SUBJECT_PREFIX = "smtp.subject-prefix";

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
     * Specifies access token which won't be verified and can be used for testing purposes.
     */
    public static final String SECURITY_ROOT_ACCESS_TOKEN = "security.root-access-token";

    /**
     * Specifies expression which decides whether user is a system administrator
     * (they can use the {@link UserSessionSettings#administratorMode}).
     */
    public static final String SECURITY_AUTHORIZATION_ADMINISTRATION = "security.authorization.administration";

    /**
     * Specifies expression which decides whether user can create a reservation request.
     */
    public static final String SECURITY_AUTHORIZATION_RESERVATION = "security.authorization.reservation";

    /**
     * SSL host verification mappings.
     */
    public static final String SSL_HOST_VERIFICATION_MAPPINGS = "ssl.host-verification-mapping";

    /**
     * Administrator emails to which error are reported.
     */
    public static final String ADMINISTRATOR_EMAIL = "administrator.email";

    /**
     * Primary url of a reservation requests with "${reservationRequestId}" variable which can be used in notifications.
     */
    public static final String NOTIFICATION_RESERVATION_REQUEST_URL = "notification.reservation-request-url";

    /**
     * Url where user can change his settings.
     */
    public static final String NOTIFICATION_USER_SETTINGS_URL = "notification.user-settings-url";

    /**
     * Constructor.
     */
    public ControllerConfiguration()
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("host-verification-mapping");
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
     * @param reservationRequestId
     * @return {@link #NOTIFICATION_RESERVATION_REQUEST_URL} for given {@code reservationRequestId}
     */
    public String getNotificationReservationRequestUrl(String reservationRequestId)
    {
        String reservationRequestUrl = getString(NOTIFICATION_RESERVATION_REQUEST_URL);
        if (reservationRequestUrl == null || reservationRequestUrl.isEmpty()) {
            return null;
        }
        return reservationRequestUrl.replace("${reservationRequestId}", reservationRequestId);
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
     * @return set of {@link #ADMINISTRATOR_EMAIL}
     */
    public synchronized List<PersonInformation> getAdministrators()
    {
        if (administrators == null) {
            administrators = new LinkedList<PersonInformation>();
            for (Object item : getList(ControllerConfiguration.ADMINISTRATOR_EMAIL)) {
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
                        return (String) administratorEmail;
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
     * @param administrators sets the {@link #administrators}
     */
    public synchronized void setAdministrators(List<PersonInformation> administrators)
    {
        this.administrators = administrators;
    }
}
