package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import lombok.Getter;
import lombok.Setter;

/**
 * Reservation device authorized to create/list reservations for a particular resource.
 */
@Getter
@Setter
public class ReservationDevice extends IdentifiedComplexType {
    private String accessToken;
    private String resourceId;

    private static final String ID = "id";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String RESOURCE_ID = "resource_id";


    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ID, id);
        dataMap.set(ACCESS_TOKEN, accessToken);
        dataMap.set(RESOURCE_ID, resourceId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        id = dataMap.getString(ID);
        accessToken = dataMap.getString(ACCESS_TOKEN);
        resourceId = dataMap.getString(RESOURCE_ID);
    }

    @Override
    public String toString()
    {
        return String.format("ReservationDevice (%s, %s)", id, resourceId);
    }
}
