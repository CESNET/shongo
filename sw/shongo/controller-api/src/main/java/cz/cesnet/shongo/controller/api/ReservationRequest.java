package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.DateTime;
import org.joda.time.Interval;
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
     * Date/time when the reservation request was created.
     */
    private DateTime created;

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
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    public static final String INTER_DOMAIN = "interDomain";

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
     * @return {@link #created}
     */
    public DateTime getCreated()
    {
        return created;
    }
    /**
     * @param created sets the {@link #created}
     */
    public void setCreated(DateTime created)
    {
        this.created = created;
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
     * @return {@link #COMPARTMENTS}
     */
    @Required
    public List<Compartment> getCompartments()
    {
        return getPropertyStorage().getCollection(COMPARTMENTS, List.class);
    }

    /**
     * @param compartments sets the {@link #COMPARTMENTS}
     */
    public void setCompartments(List<Compartment> compartments)
    {
        getPropertyStorage().setCollection(COMPARTMENTS, compartments);
    }

    /**
     * @param compartment compartment to be added to the {@link #COMPARTMENTS}
     */
    public void addCompartment(Compartment compartment)
    {
        getPropertyStorage().addCollectionItem(COMPARTMENTS, compartment, List.class);
    }

    /**
     * @return newly added {@link Compartment} to the {@link #COMPARTMENTS}
     */
    public Compartment addCompartment()
    {
        Compartment compartment = new Compartment();
        addCompartment(compartment);
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
     * @return {@link #INTER_DOMAIN}
     */
    public Boolean getInterDomain()
    {
        return getPropertyStorage().getValue(INTER_DOMAIN);
    }

    /**
     * @param interDomain sets the {@link #INTER_DOMAIN}
     */
    public void setInterDomain(Boolean interDomain)
    {
        getPropertyStorage().setValue(INTER_DOMAIN, interDomain);
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
            NOT_COMPLETE,
            NOT_ALLOCATED,
            ALLOCATED,
            ALLOCATION_FAILED
        }

        /**
         * Slot date/time and duration.
         */
        private Interval slot;

        /**
         * State of processed slot.
         */
        private State state;

        /**
         * Description of state.
         */
        private String stateDescription;

        /**
         * @return {@link #slot}
         */
        public Interval getSlot()
        {
            return slot;
        }

        /**
         * @param slot sets the {@link #slot}
         */
        public void setSlot(Interval slot)
        {
            this.slot = slot;
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

        /**
         * @return {@link #stateDescription}
         */
        public String getStateDescription()
        {
            return stateDescription;
        }

        /**
         * @param stateDescription sets the {@link #stateDescription}
         */
        public void setStateDescription(String stateDescription)
        {
            this.stateDescription = stateDescription;
        }
    }
}
