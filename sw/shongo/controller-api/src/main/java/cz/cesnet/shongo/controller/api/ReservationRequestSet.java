package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSet extends NormalReservationRequest
{
    /**
     * Collection of {@link cz.cesnet.shongo.controller.api.DateTimeSlot} for which the reservation is requested.
     */
    public static final String SLOTS = "slots";

    /**
     * Collection of {@link Specification} which are requested for the reservation.
     */
    public static final String SPECIFICATIONS = "specifications";

    /**
     * List of {@link ReservationRequest} which have been already created for the {@link ReservationRequestSet}.
     */
    private List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();

    /**
     * Constructor.
     */
    public ReservationRequestSet()
    {
    }

    /**
     * @return {@link #SLOTS}
     */
    @Required
    public List<DateTimeSlot> getSlots()
    {
        return getPropertyStorage().getCollection(SLOTS, List.class);
    }

    /**
     * @param slots sets the {@link #SLOTS}
     */
    public void setSlots(List<DateTimeSlot> slots)
    {
        getPropertyStorage().setCollection(SLOTS, slots);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param dateTimeSlot
     */
    public void addSlot(DateTimeSlot dateTimeSlot)
    {
        getPropertyStorage().addCollectionItem(SLOTS, dateTimeSlot, List.class);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param start
     * @param duration
     */
    public void addSlot(Object start, Period duration)
    {
        DateTimeSlot dateTimeSlot = new DateTimeSlot(start, duration);
        addSlot(dateTimeSlot);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param start
     * @param duration
     */
    public void addSlot(String start, String duration)
    {
        DateTimeSlot dateTimeSlot = new DateTimeSlot(start, duration);
        addSlot(dateTimeSlot);
    }

    /**
     * @param dateTimeSlot slot to be removed from the {@link #SLOTS}
     */
    public void removeSlot(DateTimeSlot dateTimeSlot)
    {
        getPropertyStorage().removeCollectionItem(SLOTS, dateTimeSlot);
    }

    /**
     * @return {@link #SPECIFICATIONS}
     */
    @Required
    public List<Specification> getSpecifications()
    {
        return getPropertyStorage().getCollection(SPECIFICATIONS, List.class);
    }

    /**
     * @param specifications sets the {@link #SPECIFICATIONS}
     */
    public void setSpecifications(List<Specification> specifications)
    {
        getPropertyStorage().setCollection(SPECIFICATIONS, specifications);
    }

    /**
     * @param specification to be added to the {@link #SPECIFICATIONS}
     */
    public <T extends Specification> T addSpecification(T specification)
    {
        getPropertyStorage().addCollectionItem(SPECIFICATIONS, specification, List.class);
        return specification;
    }

    /**
     * @param specification to be removed from the {@link #SPECIFICATIONS}
     */
    public void removeSpecification(Specification specification)
    {
        getPropertyStorage().removeCollectionItem(SPECIFICATIONS, specification);
    }

    /**
     * @return {@link #reservationRequests}
     */
    @ReadOnly
    public List<ReservationRequest> getReservationRequests()
    {
        return reservationRequests;
    }

    /**
     * @param request slot to be added to the {@link #reservationRequests}
     */
    public void addReservationRequest(ReservationRequest request)
    {
        reservationRequests.add(request);
    }
}
