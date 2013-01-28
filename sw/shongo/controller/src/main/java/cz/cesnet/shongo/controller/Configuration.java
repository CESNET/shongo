package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.executor.Executable;
import org.apache.commons.configuration.CompositeConfiguration;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Configuration for the {@link Controller}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Configuration extends CompositeConfiguration
{
    /**
     * Configuration parameters names.
     */
    public static final String LOG_RPC = "log-rpc";
    public static final String DOMAIN_NAME = "domain.name";
    public static final String DOMAIN_ORGANIZATION = "domain.organization";
    public static final String RPC_HOST = "rpc.host";
    public static final String RPC_PORT = "rpc.port";
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";
    public static final String JADE_PLATFORM_ID = "jade.platform-id";
    public static final String WORKER_PERIOD = "worker.period";
    public static final String WORKER_INTERVAL = "worker.interval";
    public static final String RESERVATION_RESOURCE_MAX_DURATION = "reservation.resource.max-duration";
    public static final String RESERVATION_VALUE_MAX_DURATION = "reservation.value.max-duration";

    /**
     * Database configuration
     */
    public static final String DATABASE_FILENAME = "database.filename";

    /**
     * SMTP configuration.
     */
    public static final String SMTP_SENDER = "smtp.sender";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";

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
     * Duration which {@link cz.cesnet.shongo.controller.Executor} waits for virtual rooms to be created.
     */
    public static final String EXECUTOR_STARTINT_DURATION_ROOM = "executor.starting-duration.room";

    /**
     * Authorization server.
     */
    public static final String SECURITY_AUTHORIZATION_SERVER = "security.authorization-server";

    /**
     * Specifies expiration of cache for user-id by access-token.
     */
    public static final String SECURITY_USER_ID_CACHE_EXPIRATION = "security.user-id-cache-expiration";

    /**
     * Specifies expiration of cache for user information by user-id.
     */
    public static final String SECURITY_USER_INFORMATION_CACHE_EXPIRATION =
            "security.user-information-cache-expiration";

    /**
     * Specifies access token which won't be verified and can be used for testing purposes.
     */
    public static final String SECURITY_TESTING_ACCESS_TOKEN = "security.testing-access-token";

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
}
