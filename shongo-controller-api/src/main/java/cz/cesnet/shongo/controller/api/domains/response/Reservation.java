package cz.cesnet.shongo.controller.api.domains.response;


import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
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
    @JsonProperty("foreignReservationRequestId")
    private String foreignReservationRequestId;

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

    @JsonProperty("foreignResourceId")
    private String foreignResourceId;

    @JsonProperty("type")
    private DomainCapabilityListRequest.Type type;

    @JsonProperty("reservationRequestDescription")
    private String reservationRequestDescription;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("resourceName")
    private String resourceName;

    @JsonProperty("resourceDescription")
    private String resourceDescription;

    public String getForeignReservationRequestId()
    {
        return foreignReservationRequestId;
    }

    public void setForeignReservationRequestId(String foreignReservationRequestId)
    {
        this.foreignReservationRequestId = foreignReservationRequestId;
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

    public String getForeignResourceId()
    {
        return foreignResourceId;
    }

    public void setForeignResourceId(String foreignResourceId)
    {
        this.foreignResourceId = foreignResourceId;
    }

    public DomainCapabilityListRequest.Type getType()
    {
        return type;
    }

    public void setType(DomainCapabilityListRequest.Type type)
    {
        this.type = type;
    }

    public String getReservationRequestDescription()
    {
        return reservationRequestDescription;
    }

    public void setReservationRequestDescription(String reservationRequestDescription)
    {
        this.reservationRequestDescription = reservationRequestDescription;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }

    public String getResourceDescription()
    {
        return resourceDescription;
    }

    public void setResourceDescription(String resourceDescription)
    {
        this.resourceDescription = resourceDescription;
    }

    public boolean isAllocated()
    {
        if (foreignReservationId != null) {
            return true;
        }
        return false;
    }

    public ReservationSummary toReservationSummary()
    {
        ReservationSummary reservationSummary = new ReservationSummary();
        reservationSummary.setId(getForeignReservationId());
        reservationSummary.setUserId(getUserId());
        reservationSummary.setReservationRequestId(getForeignReservationRequestId());
        switch (getType()) {
            case VIRTUAL_ROOM:
//                reservationSummary.setRoomLicenseCount();
//                reservationSummary.setRoomName();
                throw new TodoImplementException();
            case RESOURCE:
                reservationSummary.setType(ReservationSummary.Type.RESOURCE);
                break;
        }
        reservationSummary.setSlot(getSlot());
        reservationSummary.setResourceId(getForeignResourceId());
        reservationSummary.setReservationRequestDescription(getReservationRequestDescription());
        return reservationSummary;
    }

//    public Reservation fromReservationSummary(ReservationSummary reservationSummary)
//    {
//        Reservation reservation = new Reservation();
//        reservation.setForeignReservationId(reservationSummary.getId());
//        reservation.setUserId(reservationSummary.getUserId());
//        reservation.setForeignReservationRequestId(reservationSummary.getReservationRequestId());
//        switch (reservationSummary.getType()) {
//            case RESOURCE:
//                reservation.setType(DomainCapabilityListRequest.Type.RESOURCE);
//                reservation.setForeignResourceId(reservationSummary.getResourceId());
//                break;
//            case ROOM:
//            default:
//                throw new TodoImplementException();
//        }
//        reservation.setSlot(reservationSummary.getSlot());
//        reservation.setReservationRequestDescription(reservationSummary.getReservationRequestDescription());
//        return reservation;
//    }
}
