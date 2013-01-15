package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a specification for date/time slots when the {@link Resource} is not available for allocation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PermanentReservationRequest extends AbstractReservationRequest
{
    /**
     * List of {@link DateTimeSlotSpecification}s for which the reservation is requested.
     */
    private List<DateTimeSlotSpecification> slots = new ArrayList<DateTimeSlotSpecification>();

    /**
     * {@link Resource}
     */
    private Resource resource;

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
     * @throws cz.cesnet.shongo.fault.EntityNotFoundException
     *          when the {@link DateTimeSlotSpecification} doesn't exist
     */
    @Transient
    private DateTimeSlotSpecification getSlotById(Long id) throws EntityNotFoundException
    {
        for (DateTimeSlotSpecification dateTimeSlot : slots) {
            if (dateTimeSlot.getId().equals(id)) {
                return dateTimeSlot;
            }
        }
        throw new EntityNotFoundException(DateTimeSlotSpecification.class, id);
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
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sest the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #reservations}
     */
    @Transient
    public List<ResourceReservation> getResourceReservations()
    {
        ArrayList<ResourceReservation> resourceReservations = new ArrayList<ResourceReservation>();
        for (Reservation reservation : super.getReservations()) {
            resourceReservations.add((ResourceReservation) reservation);
        }
        return resourceReservations;
    }

    @Override
    protected ResourceReservation getReservationById(Long id) throws EntityNotFoundException
    {
        return (ResourceReservation) super.getReservationById(id);
    }

    @Override
    public void addReservation(Reservation reservation)
    {
        if (!(reservation instanceof ResourceReservation)) {
            throw new IllegalArgumentException(ResourceReservation.class.getSimpleName() + " is required.");
        }
        super.addReservation(reservation);
    }

    @Override
    public void removeReservation(Reservation reservation)
    {
        if (!(reservation instanceof ResourceReservation)) {
            throw new IllegalArgumentException(ResourceReservation.class.getSimpleName() + " is required.");
        }
        super.removeReservation(reservation);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.PermanentReservationRequest();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.PermanentReservationRequest permanentReservationRequestApi =
                (cz.cesnet.shongo.controller.api.PermanentReservationRequest) api;
        for (DateTimeSlotSpecification slot : getSlots()) {
            permanentReservationRequestApi.addSlot(slot.toApi());
        }
        permanentReservationRequestApi.setResourceId(Domain.getLocalDomain().formatId(getResource()));
        permanentReservationRequestApi.setReport(getReportText());
        for (ResourceReservation resourceReservation : getResourceReservations()) {
            permanentReservationRequestApi.addResourceReservation(resourceReservation.toApi());
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.PermanentReservationRequest permanentReservationRequestApi =
                (cz.cesnet.shongo.controller.api.PermanentReservationRequest) api;

        // Create/modify slots
        for (cz.cesnet.shongo.controller.api.DateTimeSlot slotApi : permanentReservationRequestApi.getSlots()) {
            if (api.isPropertyItemMarkedAsNew(permanentReservationRequestApi.SLOTS, slotApi)) {
                addSlot(DateTimeSlotSpecification.createFromApi(slotApi));
            }
            else {
                DateTimeSlotSpecification slot = getSlotById(slotApi.notNullIdAsLong());
                slot.fromApi(slotApi);
            }
        }
        // Delete slots
        Set<cz.cesnet.shongo.controller.api.DateTimeSlot> apiDeletedSlots =
                api.getPropertyItemsMarkedAsDeleted(permanentReservationRequestApi.SLOTS);
        for (cz.cesnet.shongo.controller.api.DateTimeSlot slotApi : apiDeletedSlots) {
            removeSlot(getSlotById(slotApi.notNullIdAsLong()));
        }

        // Set resource
        if (permanentReservationRequestApi.isPropertyFilled(permanentReservationRequestApi.RESOURCE_ID)) {
            if (permanentReservationRequestApi.getResourceId() == null) {
                setResource(null);
            }
            else {
                Long resourceId = Domain.getLocalDomain().parseId(permanentReservationRequestApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }

        super.fromApi(api, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("slots", slots);
        map.put("resource", resource);
        map.put("resourceReservations", getReservations());
    }
}
