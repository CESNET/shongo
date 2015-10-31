package cz.cesnet.shongo.controller.api.domains.response;


import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a reservation for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonIgnoreProperties({"allocated", "slot"})
public class Reservation extends AbstractResponse implements Comparable<Reservation>
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

    @JsonProperty("type")
    private DomainCapabilityListRequest.Type type;

    @JsonProperty("reservationRequestDescription")
    private String reservationRequestDescription;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("price")
    private int price;

    @JsonProperty("specification")
    private ForeignSpecification specification;

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

    public int getPrice()
    {
        if (!success() || price < 0) {
            return -1;
        }
        return price;
    }

    public void setPrice(int price)
    {
        this.price = price;
    }

    public boolean isAllocated()
    {
        if (foreignReservationId != null) {
            return true;
        }
        return false;
    }

    public ForeignSpecification getSpecification()
    {
        return specification;
    }

    public void setSpecification(ForeignSpecification specification)
    {
        this.specification = specification;
    }

    public ReservationSummary toReservationSummary()
    {
        ReservationSummary reservationSummary = new ReservationSummary();
        reservationSummary.setId(getForeignReservationId());
        reservationSummary.setSlot(getSlot());
        reservationSummary.setUserId(getUserId());
        reservationSummary.setReservationRequestId(getForeignReservationRequestId());
        reservationSummary.setReservationRequestDescription(getReservationRequestDescription());
        switch (getType()) {
            case VIRTUAL_ROOM:
                reservationSummary.setType(ReservationSummary.Type.ROOM);
                RoomSpecification roomSpecification = (RoomSpecification) getSpecification();
                reservationSummary.setRoomLicenseCount(roomSpecification.getLicenseCount());
//                reservationSummary.setRoomName();
            case RESOURCE:
                ResourceSpecification resourceSpecification = (ResourceSpecification) getSpecification();
                reservationSummary.setType(ReservationSummary.Type.RESOURCE);
                reservationSummary.setResourceId(resourceSpecification.getForeignResourceId());
                break;
        }
        return reservationSummary;
    }

    @Override
    public int compareTo(Reservation o)
    {
        if (this.getPrice() == -1 && o.getPrice() == -1) {
            return 0;
        }
        if (this.getPrice() == -1) {
            return Integer.MAX_VALUE;
        }
        if (o.getPrice() == -1) {
            return Integer.MIN_VALUE;
        }
        return this.getPrice() - o.getPrice();
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
