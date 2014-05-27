package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.util.DeviceAddress;

/**
 * Configuration for device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface DeviceConfiguration
{
    /**
     * @return {@link cz.cesnet.shongo.api.util.DeviceAddress} for managed device
     */
    public DeviceAddress getAddress();

    /**
     * @return username for managed device
     */
    public String getUserName();

    /**
     * @return password for {@link #getUserName()}
     */
    public String getPassword();
}
