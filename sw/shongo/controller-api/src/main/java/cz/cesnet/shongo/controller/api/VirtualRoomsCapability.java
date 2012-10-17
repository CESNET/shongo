package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * Capability tells that the device is able to host multiple virtual rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomsCapability extends Capability
{
    /**
     * Number of available ports.
     */
    public static final String PORT_COUNT = "portCount";

    /**
     * Constructor.
     */
    public VirtualRoomsCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param portCount sets the {@link #PORT_COUNT}
     */
    public VirtualRoomsCapability(Integer portCount)
    {
        setPortCount(portCount);
    }

    /**
     * @return {@link #PORT_COUNT}
     */
    @Required
    public Integer getPortCount()
    {
        return getPropertyStorage().getValue(PORT_COUNT);
    }

    /**
     * @param portCount sets the {@link #PORT_COUNT}
     */
    public void setPortCount(Integer portCount)
    {
        getPropertyStorage().setValue(PORT_COUNT, portCount);
    }
}
