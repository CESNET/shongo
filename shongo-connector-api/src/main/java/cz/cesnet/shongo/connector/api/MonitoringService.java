package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface MonitoringService
{
    /**
     * Gets the multipoint usage stats.
     *
     * @return usage stats
     */
    DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException;

    /**
     * Gets the multipoint usage stats.
     *
     * @return usage stats
     */
    UsageStats getUsageStats() throws CommandException, CommandUnsupportedException;
}
