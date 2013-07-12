package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a connection between {@link cz.cesnet.shongo.controller.api.EndpointExecutable}s in the {@link cz.cesnet.shongo.controller.api.CompartmentExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectionExecutable extends Executable
{
    /**
     * Id of endpoint which initiates the {@link cz.cesnet.shongo.controller.api.ConnectionExecutable}.
     */
    private String endpointFromId;

    /**
     * Id of target endpoint for the {@link cz.cesnet.shongo.controller.api.ConnectionExecutable}.
     */
    private String endpointToId;

    /**
     * {@link cz.cesnet.shongo.api.Alias} which is used for the {@link cz.cesnet.shongo.controller.api.ConnectionExecutable}
     */
    private Alias alias;

    /**
     * @return {@link #endpointFromId}
     */
    public String getEndpointFromId()
    {
        return endpointFromId;
    }

    /**
     * @param endpointFromId sets the {@link #endpointFromId}
     */
    public void setEndpointFromId(String endpointFromId)
    {
        this.endpointFromId = endpointFromId;
    }

    /**
     * @return {@link #endpointToId}
     */
    public String getEndpointToId()
    {
        return endpointToId;
    }

    /**
     * @param endpointToId sets the {@link #endpointToId}
     */
    public void setEndpointToId(String endpointToId)
    {
        this.endpointToId = endpointToId;
    }

    /**
     * @return {@link #alias}
     */
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    private static final String ENDPOINT_FROM_ID = "endpointFromId";
    private static final String ENDPOINT_TO_ID = "endpointToId";
    private static final String ALIAS = "alias";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ENDPOINT_FROM_ID, endpointFromId);
        dataMap.set(ENDPOINT_TO_ID, endpointToId);
        dataMap.set(ALIAS, alias);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        endpointFromId = dataMap.getString(ENDPOINT_FROM_ID);
        endpointToId = dataMap.getString(ENDPOINT_TO_ID);
        alias = dataMap.getComplexType(ALIAS, Alias.class);
    }
}
