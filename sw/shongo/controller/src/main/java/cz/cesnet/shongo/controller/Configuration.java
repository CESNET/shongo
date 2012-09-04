package cz.cesnet.shongo.controller;

import org.apache.commons.configuration.CompositeConfiguration;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Configuration for the controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Configuration extends CompositeConfiguration
{
    /**
     * Controller configuration parameters names.
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
    public static final String ALLOCATION_RESOURCE_MAX_DURATION = "allocation.resource-max-duration";
    public static final String ALLOCATION_ALIAS_MAX_DURATION = "allocation.alias-max-duration";

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
