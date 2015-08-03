package cz.cesnet.shongo.controller.api.domains.response;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.Interval;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a reservation for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonIgnoreProperties({"allocated"})
public class Reservation
{
    /**
     * Reservation request for which is {@link Reservation} allocated.
     */
    @JsonProperty("reservationRequestId")
    private String reservationRequestId;

    @JsonProperty("foreignReservationId")
    private String foreignReservationId;

    /**
     * Slot fot which the {@link Reservation} is allocated.
     */
    @JsonProperty("slot")
    private Interval slot;

    @JsonProperty("resourceId")
    private String resourceId;

    @JsonProperty("type")
    private DomainCapabilityListRequest.Type type;

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    public String getForeignReservationId()
    {
        return foreignReservationId;
    }

    public void setForeignReservationId(String foreignReservationId)
    {
        this.foreignReservationId = foreignReservationId;
    }

    public Interval getSlot()
    {
        return slot;
    }

    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    public String getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    public DomainCapabilityListRequest.Type getType()
    {
        return type;
    }

    public void setType(DomainCapabilityListRequest.Type type)
    {
        this.type = type;
    }

    public boolean isAllocated()
    {
        if (foreignReservationId != null && slot != null) {
            return true;
        }
        return false;
    }
}
