package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends IdentifiedChangeableObject
{
    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * @see ReservationRequestType
     */
    public static final String TYPE = "type";

    /**
     * Name of the reservation request.
     */
    public static final String NAME = "name";

    /**
     * @see ReservationRequestPurpose
     */
    public static final String PURPOSE = "purpose";

    /**
     * Description of the reservation request.
     */
    public static final String DESCRIPTION = "description";

    /**
     * Collection of {@link DateTimeSlot} for which the reservation is requested.
     */
    public static final String SLOTS = "slots";

    /**
     * Collection of {@link Compartment} which are requested for the reservation.
     */
    public static final String COMPARTMENTS = "compartments";

    /**
     * List of {@link Request} which are already processed for the reservation.
     */
    private List<Request> requests = new ArrayList<Request>();

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #TYPE}
     */
    @Required
    public ReservationRequestType getType()
    {
        return getPropertyStorage().getValue(TYPE);
    }

    /**
     * @param type sets the {@link #TYPE}
     */
    public void setType(ReservationRequestType type)
    {
        getPropertyStorage().setValue(TYPE, type);
    }

    /**
     * @return {@link #NAME}
     */
    public String getName()
    {
        return getPropertyStorage().getValue(NAME);
    }

    /**
     * @param name sets the {@link #NAME}
     */
    public void setName(String name)
    {
        getPropertyStorage().setValue(NAME, name);
    }

    /**
     * @return {@link #PURPOSE}
     */
    @Required
    public ReservationRequestPurpose getPurpose()
    {
        return getPropertyStorage().getValue(PURPOSE);
    }

    /**
     * @param purpose sets the {@link #PURPOSE}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        getPropertyStorage().setValue(PURPOSE, purpose);
    }

    /**
     * @return {@link #DESCRIPTION}
     */
    public String getDescription()
    {
        return getPropertyStorage().getValue(DESCRIPTION);
    }

    /**
     * @param description sets the {@link #DESCRIPTION}
     */
    public void setDescription(String description)
    {
        getPropertyStorage().setValue(DESCRIPTION, description);
    }

    /**
     * @return {@link #SLOTS}
     */
    @Required
    public List<DateTimeSlot> getSlots()
    {
        return getPropertyStorage().getCollection(SLOTS);
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
        getPropertyStorage().addCollectionItem(SLOTS, dateTimeSlot);
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
     * @return {@link #COMPARTMENTS}
     */
    @Required
    public List<Compartment> getCompartments()
    {
        return getPropertyStorage().getCollection(COMPARTMENTS);
    }

    /**
     * @param compartments sets the {@link #COMPARTMENTS}
     */
    public void setCompartments(List<Compartment> compartments)
    {
        getPropertyStorage().setCollection(COMPARTMENTS, compartments);
    }

    /**
     * @return newly added {@link Compartment} to the {@link #COMPARTMENTS}
     */
    public Compartment addCompartment()
    {
        Compartment compartment = new Compartment();
        getPropertyStorage().addCollectionItem(COMPARTMENTS, compartment);
        return compartment;
    }

    /**
     * @param compartment compartment to be removed from the {@link #COMPARTMENTS}
     */
    public void removeCompartment(Compartment compartment)
    {
        getPropertyStorage().removeCollectionItem(COMPARTMENTS, compartment);
    }

    /**
     * @return {@link #requests}
     */
    public List<Request> getRequests()
    {
        return requests;
    }

    /**
     * @param request slot to be added to the {@link #requests}
     */
    public void addRequest(Request request)
    {
        requests.add(request);
    }

    /**
     * Represents a single already processed slot that is requested by reservation request.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    public static class Request
    {
        /**
         * State of processed slot.
         */
        public static enum State
        {
            NOT_ALLOCATED,
            ALLOCATED,
            ALLOCATION_FAILED
        }

        /**
         * Starting date/time.
         */
        private DateTime start;

        /**
         * Duration of the time slot.
         */
        private Period duration;

        /**
         * Type of processed slot.
         */
        private State state;

        /**
         * @return {@link #start}
         */
        public DateTime getStart()
        {
            return start;
        }

        /**
         * @param start sets the {@link #start}
         */
        void setStart(DateTime start)
        {
            this.start = start;
        }

        /**
         * @return {@link #duration}
         */
        public Period getDuration()
        {
            return duration;
        }

        /**
         * @param duration sets the {@link #duration}
         */
        void setDuration(Period duration)
        {
            this.duration = duration;
        }

        /**
         * @return {@link #state}
         */
        public State getState()
        {
            return state;
        }

        /**
         * @param state sets the {@link #state}
         */
        public void setState(State state)
        {
            this.state = state;
        }
    }
}
