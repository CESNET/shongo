package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specification for date/time slots when the {@link Resource} is not available for allocation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermanentReservationRequest extends AbstractReservationRequest
{
    /**
     * Collection of {@link DateTimeSlot} for which the reservation is requested.
     */
    public static final String SLOTS = "slots";

    /**
     * Identifier of a {@link Resource}.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

    /**
     * List of allocated {@link ResourceReservation}s.
     */
    private List<ResourceReservation> resourceReservations = new ArrayList<ResourceReservation>();

    /**
     * Constructor.
     */
    public PermanentReservationRequest()
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
     * @param dateTimeSlot slot to be removed from the {@link #SLOTS}
     */
    public void removeSlot(DateTimeSlot dateTimeSlot)
    {
        getPropertyStorage().removeCollectionItem(SLOTS, dateTimeSlot);
    }

    /**
     * @return {@link #RESOURCE_IDENTIFIER}
     */
    @Required
    public String getResourceIdentifier()
    {
        return getPropertyStorage().getValue(RESOURCE_IDENTIFIER);
    }

    /**
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        getPropertyStorage().setValue(RESOURCE_IDENTIFIER, resourceIdentifier);
    }

    /**
     * @return {@link #resourceReservations}
     */
    public List<ResourceReservation> getResourceReservations()
    {
        return resourceReservations;
    }

    /**
     * @param resourceReservation slot to be added to the {@link #resourceReservations}
     */
    public void addResourceReservation(ResourceReservation resourceReservation)
    {
        resourceReservations.add(resourceReservation);
    }
}
