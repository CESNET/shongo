package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a specification of one or multiple {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequestSet extends AbstractReservationRequest
{
    /**
     * State of {@link ReservationRequestSet}.
     */
    public static enum State
    {
        /**
         * State tells that {@link ReservationRequestSet} hasn't corresponding {@link ReservationRequest}s created
         * or the {@link ReservationRequestSet} has changed and the {@link ReservationRequest}s are out-of-sync.
         */
        NOT_PREPROCESSED,

        /**
         * State tells that reservation request has corresponding compartment requests synced.
         */
        PREPROCESSED
    }

    /**
     * List of {@link DateTimeSlotSpecification}s for which the reservation is requested.
     */
    private List<DateTimeSlotSpecification> requestedSlots = new ArrayList<DateTimeSlotSpecification>();

    /**
     * List of {@link Specification}s for targets which are requested for a reservation.
     */
    private List<Specification> requestedSpecifications = new ArrayList<Specification>();

    /**
     * List of created {@link ReservationRequest}s.
     */
    private List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();

    /**
     * @return {@link #requestedSlots}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<DateTimeSlotSpecification> getRequestedSlots()
    {
        return Collections.unmodifiableList(requestedSlots);
    }

    /**
     * @param id of the requested {@link DateTimeSlotSpecification}
     * @return {@link DateTimeSlotSpecification} with given {@code id}
     * @throws EntityNotFoundException when the {@link DateTimeSlotSpecification} doesn't exist
     */
    public DateTimeSlotSpecification getRequestedSlotById(Long id) throws EntityNotFoundException
    {
        for (DateTimeSlotSpecification dateTimeSlot : requestedSlots) {
            if (dateTimeSlot.getId().equals(id)) {
                return dateTimeSlot;
            }
        }
        throw new EntityNotFoundException(DateTimeSlotSpecification.class, id);
    }

    /**
     * @param requestedSlot to be added to the {@link #requestedSlots}
     */
    public void addRequestedSlot(DateTimeSlotSpecification requestedSlot)
    {
        requestedSlots.add(requestedSlot);
    }

    /**
     * Add new {@link DateTimeSlotSpecification} constructed from {@code dateTime} and {@code duration} to
     * the {@link #requestedSlots}.
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addRequestedSlot(DateTimeSpecification dateTime, Period duration)
    {
        requestedSlots.add(new DateTimeSlotSpecification(dateTime, duration));
    }

    /**
     * @param requestedSlot slot to be removed from the {@link #requestedSlots}
     */
    public void removeRequestedSlot(DateTimeSlotSpecification requestedSlot)
    {
        requestedSlots.remove(requestedSlot);
    }

    /**
     * @return {@link #requestedSpecifications}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Specification> getRequestedSpecifications()
    {
        return Collections.unmodifiableList(requestedSpecifications);
    }

    /**
     * @param id of the requested {@link Specification}
     * @return {@link Specification} with given {@code id}
     * @throws EntityNotFoundException when the {@link Specification} doesn't exist
     */
    public Specification getRequestedSpecificationById(Long id) throws EntityNotFoundException
    {
        for (Specification specification : requestedSpecifications) {
            if (specification.getId().equals(id)) {
                return specification;
            }
        }
        throw new EntityNotFoundException(Specification.class, id);
    }

    /**
     * @param specification to be added to the {@link #requestedSpecifications}
     */
    public void addRequestedSpecification(Specification specification)
    {
        requestedSpecifications.add(specification);
    }

    /**
     * @param specification to be removed from the {@link #requestedSpecifications}
     */
    public void removeRequestedSpecification(Specification specification)
    {
        requestedSpecifications.remove(specification);
    }

    /**
     * Enumerate requested date/time slots in a specific interval.
     *
     * @param interval
     * @return list of all requested absolute date/time slots for given interval
     */
    public List<Interval> enumerateRequestedSlots(Interval interval)
    {
        List<Interval> enumeratedSlots = new ArrayList<Interval>();
        for (DateTimeSlotSpecification slot : requestedSlots) {
            enumeratedSlots.addAll(slot.enumerate(interval));
        }
        return enumeratedSlots;
    }

    /**
     * @param referenceDateTime
     * @return true whether reservation request has any requested slot after given reference date/time,
     *         false otherwise
     */
    public boolean hasRequestedSlotAfter(DateTime referenceDateTime)
    {
        for (DateTimeSlotSpecification slot : requestedSlots) {
            if (slot.getStart().willOccur(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@link #reservationRequests}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<ReservationRequest> getReservationRequests()
    {
        return Collections.unmodifiableList(reservationRequests);
    }

    /**
     * @param id of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given {@code id}
     * @throws EntityNotFoundException when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequestById(Long id) throws EntityNotFoundException
    {
        for (ReservationRequest reservationRequest : reservationRequests) {
            if (reservationRequest.getId().equals(id)) {
                return reservationRequest;
            }
        }
        throw new EntityNotFoundException(ReservationRequest.class, id);
    }

    /**
     * @param reservationRequest to be added to the {@link #reservationRequests}
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequests.add(reservationRequest);
    }

    /**
     * @param reservationRequest slot to be removed from the {@link #reservationRequests}
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequests.remove(reservationRequest);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "slots", requestedSlots);
        addCollectionToMap(map, "specifications", requestedSpecifications);
    }
}
