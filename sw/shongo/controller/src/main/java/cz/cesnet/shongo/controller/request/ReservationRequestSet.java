package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a specification of one or multiple {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequestSet extends NormalReservationRequest
{

    /**
     * List of {@link DateTimeSlotSpecification}s for which the reservation is requested.
     */
    private List<DateTimeSlotSpecification> slots = new ArrayList<DateTimeSlotSpecification>();

    /**
     * List of created {@link ReservationRequest}s.
     */
    private List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();

    /**
     * Map of original instances of the {@link Specification} by the cloned instances.
     * <p/>
     * When a {@link ReservationRequest} is created from the {@link ReservationRequestSet} for a {@link Specification}
     * from the {@link #specification} all instances which are {@link StatefulSpecification} are cloned and we must
     * keep the references from cloned instances to the original instances for synchronizing.
     */
    private Map<Specification, Specification> originalSpecifications = new HashMap<Specification, Specification>();

    /**
     * @return {@link #slots}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<DateTimeSlotSpecification> getSlots()
    {
        return Collections.unmodifiableList(slots);
    }

    /**
     * @param id of the requested {@link DateTimeSlotSpecification}
     * @return {@link DateTimeSlotSpecification} with given {@code id}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException when the {@link DateTimeSlotSpecification} doesn't exist
     */
    @Transient
    private DateTimeSlotSpecification getSlotById(Long id) throws PersistentEntityNotFoundException
    {
        for (DateTimeSlotSpecification dateTimeSlot : slots) {
            if (dateTimeSlot.getId().equals(id)) {
                return dateTimeSlot;
            }
        }
        throw new PersistentEntityNotFoundException(DateTimeSlotSpecification.class, id);
    }

    /**
     * @param slot to be added to the {@link #slots}
     */
    public void addSlot(DateTimeSlotSpecification slot)
    {
        slots.add(slot);
    }

    /**
     * Add new {@link DateTimeSlotSpecification} constructed from {@code dateTime} and {@code duration} to
     * the {@link #slots}.
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addSlot(DateTimeSpecification dateTime, Period duration)
    {
        addSlot(new DateTimeSlotSpecification(dateTime, duration));
    }

    /**
     * Add new {@link DateTimeSlotSpecification} constructed from {@code dateTime} and {@code duration} to
     * the {@link #slots}.
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addSlot(DateTimeSpecification dateTime, String duration)
    {
        addSlot(new DateTimeSlotSpecification(dateTime, Period.parse(duration)));
    }

    /**
     * @param slot slot to be removed from the {@link #slots}
     */
    public void removeSlot(DateTimeSlotSpecification slot)
    {
        slots.remove(slot);
    }

    /**
     * @param specification which should be removed from the {@link #originalSpecifications}
     */
    private void removeOriginalSpecification(Specification specification)
    {
        Iterator<Specification> iterator = originalSpecifications.keySet().iterator();
        while (iterator.hasNext()) {
            Specification clonedSpecification = iterator.next();
            Specification originalSpecification = originalSpecifications.get(clonedSpecification);
            if (originalSpecification == specification) {
                iterator.remove();
            }
        }
        if (specification instanceof CompositeSpecification) {
            for ( Specification childSpecification : ((CompositeSpecification) specification).getChildSpecifications()) {
                removeOriginalSpecification(childSpecification);
            }
        }
    }

    /**
     * Enumerate requested date/time slots in a specific interval.
     *
     * @param interval
     * @return collection of all requested absolute date/time slots for given interval
     */
    public Collection<Interval> enumerateSlots(Interval interval)
    {
        Set<Interval> enumeratedSlots = new HashSet<Interval>();
        for (DateTimeSlotSpecification slot : slots) {
            enumeratedSlots.addAll(slot.enumerate(interval));
        }
        return enumeratedSlots;
    }

    /**
     * @param referenceDateTime
     * @return true whether reservation request has any requested slot after given reference date/time,
     *         false otherwise
     */
    public boolean hasSlotAfter(DateTime referenceDateTime)
    {
        for (DateTimeSlotSpecification slot : slots) {
            if (slot.getStart().willOccur(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@link #reservationRequests}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    @OrderBy("slotStart")
    public List<ReservationRequest> getReservationRequests()
    {
        return Collections.unmodifiableList(reservationRequests);
    }

    /**
     * @param id of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given {@code id}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException when the {@link ReservationRequest} doesn't exist
     */
    @Transient
    private ReservationRequest getReservationRequestById(Long id) throws PersistentEntityNotFoundException
    {
        for (ReservationRequest reservationRequest : reservationRequests) {
            if (reservationRequest.getId().equals(id)) {
                return reservationRequest;
            }
        }
        throw new PersistentEntityNotFoundException(ReservationRequest.class, id);
    }

    /**
     * @param reservationRequest to be added to the {@link #reservationRequests}
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequests.add(reservationRequest);
    }

    /**
     * @param reservationRequest to be removed from the {@link #reservationRequests}
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        removedClonedSpecification(reservationRequest.getSpecification());
        reservationRequests.remove(reservationRequest);
    }

    /**
     * @return {@link #originalSpecifications}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Map<Specification, Specification> getOriginalSpecifications()
    {
        return originalSpecifications;
    }

    /**
     * @param clonedSpecification to be removed from the {@link #originalSpecifications}
     */
    public void removedClonedSpecification(Specification clonedSpecification)
    {
        originalSpecifications.remove(clonedSpecification);
        if (clonedSpecification instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) clonedSpecification;
            for (Specification specification : compositeSpecification.getChildSpecifications()) {
                removedClonedSpecification(specification);
            }
        }
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.ReservationRequestSet();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSetApi =
                (cz.cesnet.shongo.controller.api.ReservationRequestSet) api;
        for (DateTimeSlotSpecification slot : getSlots()) {
            reservationRequestSetApi.addSlot(slot.toApi());
        }
        for (ReservationRequest reservationRequest : getReservationRequests()) {
            reservationRequestSetApi.addReservationRequest(reservationRequest.toApi());
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSetApi =
                (cz.cesnet.shongo.controller.api.ReservationRequestSet) api;

        // Create/modify slots
        for (cz.cesnet.shongo.controller.api.DateTimeSlot slotApi : reservationRequestSetApi.getSlots()) {
            if (api.isPropertyItemMarkedAsNew(reservationRequestSetApi.SLOTS, slotApi)) {
                addSlot(DateTimeSlotSpecification.createFromApi(slotApi));
            }
            else {
                DateTimeSlotSpecification slot = getSlotById(slotApi.notNullIdAsLong());
                slot.fromApi(slotApi);
            }
        }
        // Delete slots
        Set<cz.cesnet.shongo.controller.api.DateTimeSlot> apiDeletedSlots =
                api.getPropertyItemsMarkedAsDeleted(reservationRequestSetApi.SLOTS);
        for (cz.cesnet.shongo.controller.api.DateTimeSlot slotApi : apiDeletedSlots) {
            removeSlot(getSlotById(slotApi.notNullIdAsLong()));
        }

        super.fromApi(api, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("slots", slots);
    }
}
