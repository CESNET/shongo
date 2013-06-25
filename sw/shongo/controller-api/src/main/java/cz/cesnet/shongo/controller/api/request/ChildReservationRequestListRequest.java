package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link ListRequest} for child reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ChildReservationRequestListRequest extends ListRequest
{
    private String reservationRequestId;

    public ChildReservationRequestListRequest()
    {
    }

    public ChildReservationRequestListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    public ChildReservationRequestListRequest(SecurityToken securityToken, String reservationRequestId)
    {
        super(securityToken);
        this.reservationRequestId = reservationRequestId;
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    private static final String RESERVATION_REQUEST_ID = "reservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_ID, reservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestId = dataMap.getStringRequired(RESERVATION_REQUEST_ID);
    }
}
