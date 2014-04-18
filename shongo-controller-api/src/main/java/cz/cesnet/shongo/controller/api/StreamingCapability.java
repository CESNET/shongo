package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Capability tells that the device is able to stream a call between endpoints.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StreamingCapability extends Capability
{
    /**
     * Constructor.
     */
    public StreamingCapability()
    {
    }

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
    }
}
