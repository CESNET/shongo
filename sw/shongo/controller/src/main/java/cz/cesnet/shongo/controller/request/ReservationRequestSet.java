package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.controller.common.DateTimeSlot;
import cz.cesnet.shongo.controller.common.PeriodicDateTime;
import cz.cesnet.shongo.controller.common.PeriodicDateTimeSlot;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
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
public class ReservationRequestSet extends AbstractReservationRequest
{
    /**
     * List of {@link cz.cesnet.shongo.controller.common.DateTimeSlot}s for which the reservation is requested.
     */
    private List<DateTimeSlot> slots = new ArrayList<DateTimeSlot>();

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
    public List<DateTimeSlot> getSlots()
    {
        return Collections.unmodifiableList(slots);
    }

    /**
     * @param id of the requested {@link cz.cesnet.shongo.controller.common.DateTimeSlot}
     * @return {@link cz.cesnet.shongo.controller.common.DateTimeSlot} with given {@code id}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException
     *          when the {@link cz.cesnet.shongo.controller.common.DateTimeSlot} doesn't exist
     */
    @Transient
    private DateTimeSlot getSlotById(Long id) throws PersistentEntityNotFoundException
    {
        for (DateTimeSlot dateTimeSlot : slots) {
            if (dateTimeSlot.getId().equals(id)) {
                return dateTimeSlot;
            }
        }
        throw new PersistentEntityNotFoundException(DateTimeSlot.class, id);
    }

    /**
     * @param slot to be added to the {@link #slots}
     */
    public void addSlot(DateTimeSlot slot)
    {
        slots.add(slot);
    }

    /**
     * Add new {@link cz.cesnet.shongo.controller.common.DateTimeSlot} constructed from {@code dateTime} and {@code duration} to
     * the {@link #slots}.
     *
     * @param periodicDateTime slot date/time
     * @param duration         slot duration
     */
    public void addSlot(PeriodicDateTime periodicDateTime, String duration)
    {
        addSlot(new PeriodicDateTimeSlot(periodicDateTime, Period.parse(duration)));
    }

    /**
     * Add new {@link cz.cesnet.shongo.controller.common.DateTimeSlot} constructed from {@code dateTime} and {@code duration} to
     * the {@link #slots}.
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addSlot(String dateTime, String duration)
    {
        addSlot(new AbsoluteDateTimeSlot(DateTime.parse(dateTime), Period.parse(duration)));
    }

    /**
     * @param slot slot to be removed from the {@link #slots}
     */
    public void removeSlot(DateTimeSlot slot)
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
            for (Specification childSpecification : ((CompositeSpecification) specification).getChildSpecifications()) {
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
        for (DateTimeSlot slot : slots) {
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
        for (DateTimeSlot slot : slots) {
            if (slot.willOccur(referenceDateTime)) {
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
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException
     *          when the {@link ReservationRequest} doesn't exist
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
        for (DateTimeSlot slot : getSlots()) {
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
        for (Object slotApi : reservationRequestSetApi.getSlots()) {
            if (api.isPropertyItemMarkedAsNew(reservationRequestSetApi.SLOTS, slotApi)) {
                addSlot(DateTimeSlot.createFromApi(slotApi));
            }
            else {
                if (slotApi instanceof cz.cesnet.shongo.api.util.IdentifiedObject) {
                    cz.cesnet.shongo.api.util.IdentifiedObject identifiedApi =
                            (cz.cesnet.shongo.api.util.IdentifiedObject) slotApi;
                    DateTimeSlot slot = getSlotById(identifiedApi.notNullIdAsLong());
                    slot.fromApi(identifiedApi);
                }
                else {
                    throw new TodoImplementException("Modifying " + slotApi.getClass().getName());
                }
            }
        }
        // Delete slots
        Set<Object> apiDeletedSlots =
                api.getPropertyItemsMarkedAsDeleted(reservationRequestSetApi.SLOTS);
        for (Object slotApi : apiDeletedSlots) {
            for (DateTimeSlot dateTimeSlot : slots) {
                if (dateTimeSlot.equalsApi(slotApi)) {
                    removeSlot(dateTimeSlot);
                    break;
                }
            }
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
