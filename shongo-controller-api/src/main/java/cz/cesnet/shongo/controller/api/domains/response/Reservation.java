package cz.cesnet.shongo.controller.api.domains.response;


import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

/**
 * Represents a reservation for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonIgnoreProperties({"allocated", "slot"})
public class Reservation extends AbstractResponse
{
    /**
     * Reservation request for which is {@link Reservation} allocated.
     */
    @JsonProperty("reservationRequestId")
    private String reservationRequestId;

    @JsonProperty("foreignReservationId")
    private String foreignReservationId;

    /**
     * Slot start for which the {@link Reservation} is allocated.
     */
    @JsonProperty("slotStart")
    private DateTime slotStart;

    /**
     * Slot end for which the {@link Reservation} is allocated.
     */
    @JsonProperty("slotEnd")
    private DateTime slotEnd;

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
        return new Interval(slotStart, slotEnd);
    }

    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    public DateTime getSlotStart()
    {
        return slotStart;
    }

    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
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
        if (foreignReservationId != null) {
            return true;
        }
        return false;
    }
}
