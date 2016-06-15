package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.datetime.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTime;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTimeSlot;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
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
     * List of {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot}s for which the reservation is requested.
     */
    private List<DateTimeSlot> slots = new ArrayList<>();

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
     * @param slots sets the {@link #slots}
     */
    public void setSlots(List<DateTimeSlot> slots)
    {
        this.slots.clear();
        for (DateTimeSlot slot : slots) {
            this.slots.add(slot);
        }
    }

    /**
     * @param slot to be added to the {@link #slots}
     */
    public void addSlot(DateTimeSlot slot)
    {
        slots.add(slot);
    }

    /**
     * Add new {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot} constructed from {@code dateTime} and {@code duration} to
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
     * Add new {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot} constructed from {@code dateTime} and {@code duration} to
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
     * @param interval which must all returned date/time slots intersect
     * @return collection of all requested absolute date/time slots which intersect given {@code interval}
     */
    public Collection<Interval> enumerateSlots(Interval interval)
    {
        Set<Interval> enumeratedSlots = new TreeSet<Interval>(new Comparator<Interval>()
        {
            @Override
            public int compare(Interval o1, Interval o2)
            {
                return o1.getStart().compareTo(o2.getStart());
            }
        });
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

    @Override
    public ReservationRequestSet clone(EntityManager entityManager)
    {
        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.synchronizeFrom(this, entityManager);
        return reservationRequest;
    }

    @Override
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest, EntityManager entityManager)
    {
        boolean modified = super.synchronizeFrom(abstractReservationRequest, entityManager);
        if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;

            modified |= !ObjectHelper.isSameIgnoreOrder(getSlots(), reservationRequestSet.getSlots());

            slots.clear();
            for (DateTimeSlot slot : reservationRequestSet.getSlots()) {
                try {
                    addSlot(slot.clone());
                }
                catch (CloneNotSupportedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
        return modified;
    }

    @Override
    public void validate() throws CommonReportSet.ObjectInvalidException
    {
        for (DateTimeSlot slot : slots) {
            validateSlotDuration(slot.getDuration());
        }
        super.validate();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.ReservationRequestSet();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Report.UserType userType)
    {
        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSetApi =
                (cz.cesnet.shongo.controller.api.ReservationRequestSet) api;
        for (DateTimeSlot slot : getSlots()) {
            reservationRequestSetApi.addSlot(slot.toApi());
        }
        super.toApi(api, userType);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
    {
        super.fromApi(api, entityManager);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSetApi =
                (cz.cesnet.shongo.controller.api.ReservationRequestSet) api;

        Synchronization.synchronizeCollectionPartial(slots, reservationRequestSetApi.getSlots(),
                new Synchronization.Handler<DateTimeSlot, Object>(DateTimeSlot.class)
                {
                    @Override
                    public DateTimeSlot createFromApi(Object objectApi)
                    {
                        return DateTimeSlot.createFromApi(objectApi);
                    }

                    @Override
                    public void updateFromApi(DateTimeSlot object, Object objectApi)
                    {
                        object.fromApi(objectApi);
                    }
                });
    }
}
