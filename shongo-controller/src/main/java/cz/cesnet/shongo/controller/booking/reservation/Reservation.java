package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.report.ReportableSimple;
import org.hibernate.annotations.Index;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an allocation for any target. Each {@link Reservation} can contains multiple {@link #childReservations}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Reservation extends PersistentObject implements ReportableSimple
{
    /**
     * User-id of an user who created the {@link AbstractReservationRequest}
     * based on which this {@link Reservation} was allocated.
     */
    @Index(name = "user_id_idx")
    private String userId;

    /**
     * {@link Allocation} for which the {@link Reservation} is allocated.
     */
    private Allocation allocation;

    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * Parent {@link Reservation}.
     */
    private Reservation parentReservation;

    /**
     * Child {@link Reservation}s that are allocated for the {@link Reservation}.
     */
    private List<Reservation> childReservations = new ArrayList<Reservation>();

    /**
     * {@link Executable} which is allocated for execution by the {@link Reservation}.
     */
    private Executable executable;

    @Id
    @SequenceGenerator(name = "reservation_id", sequenceName = "reservation_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "reservation_id")
    @Override
    public Long getId()
    {
        return id;
    }

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false, length = Controller.USER_ID_COLUMN_LENGTH)
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
        for (Reservation childReservation : childReservations) {
            childReservation.setUserId(userId);
        }
    }

    /**
     * @return {@link #allocation#getReservationRequest()}
     */
    @Transient
    public AbstractReservationRequest getReservationRequest()
    {
        return (allocation != null ? allocation.getReservationRequest() : null);
    }

    /**
     * @return top {@link AbstractReservationRequest} which was created by a user
     */
    @Transient
    public AbstractReservationRequest getTopReservationRequest()
    {
        Reservation topReservation = getTopReservation();
        AbstractReservationRequest abstractReservationRequest = topReservation.getReservationRequest();
        if (abstractReservationRequest != null && abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            Allocation parentAllocation = reservationRequest.getParentAllocation();
            if (parentAllocation != null) {
                abstractReservationRequest = parentAllocation.getReservationRequest();
            }
        }
        return abstractReservationRequest;
    }

    /**
     * @return {@link #allocation}
     */
    @OneToOne(fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public Allocation getAllocation()
    {
        return allocation;
    }

    /**
     * @param allocation sets the {@link #allocation}
     */
    public void setAllocation(Allocation allocation)
    {
        // Manage bidirectional association
        if (allocation != this.allocation) {
            if (this.allocation != null) {
                Allocation oldAllocation = this.allocation;
                this.allocation = null;
                oldAllocation.removeReservation(this);
            }
            if (allocation != null) {
                this.allocation = allocation;
                this.allocation.addReservation(this);
            }
        }
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    @Access(AccessType.FIELD)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
        if (executable != null) {
            executable.setSlotStart(slotStart);
        }
        for (Reservation childReservation : childReservations) {
            childReservation.setSlotStart(slotStart);
        }
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    @Access(AccessType.FIELD)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
        if (executable != null) {
            executable.setSlotEnd(slotEnd);
        }
        for (Reservation childReservation : childReservations) {
            childReservation.setSlotEnd(slotEnd);
        }
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    @Transient
    public void setSlot(Interval slot)
    {
        setSlot(slot.getStart(), slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param slotStart
     * @param slotEnd
     */
    @Transient
    public void setSlot(DateTime slotStart, DateTime slotEnd)
    {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        if (executable != null) {
            executable.setSlot(slotStart, slotEnd);
        }
        for (Reservation childReservation : childReservations) {
            childReservation.setSlot(slotStart, slotEnd);
        }
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    @Transient
    public void setSlot(String start, String end)
    {
        setSlot(DateTime.parse(start), DateTime.parse(end));
    }

    /**
     * @return {@link #parentReservation}
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @Access(AccessType.FIELD)
    public Reservation getParentReservation()
    {
        return parentReservation;
    }

    /**
     * @param parentReservation sets the {@link #parentReservation}
     */
    public void setParentReservation(Reservation parentReservation)
    {
        // Manage bidirectional association
        if (parentReservation != this.parentReservation) {
            if (this.parentReservation != null) {
                Reservation oldParentReservation = this.parentReservation;
                this.parentReservation = null;
                oldParentReservation.removeChildReservation(this);
            }
            if (parentReservation != null) {
                this.parentReservation = parentReservation;
                this.parentReservation.addChildReservation(this);
            }
        }
    }

    /**
     * @return top parent {@link Reservation}
     */
    @Transient
    public Reservation getTopReservation()
    {
        Reservation reservation = this;
        while (reservation.getParentReservation() != null) {
            reservation = reservation.getParentReservation();
        }
        return reservation;
    }

    /**
     * @return {@link #childReservations}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentReservation")
    @Access(AccessType.FIELD)
    public List<Reservation> getChildReservations()
    {
        return childReservations;
    }

    private void setChildReservations(List<Reservation> childReservations)
    {
        this.childReservations = childReservations;
    }

    /**
     * @param reservationClass
     * @return {@link #childReservations} of given {@code reservationClass}
     */
    @Transient
    public <T extends Reservation> Collection<T> getChildReservations(Class<T> reservationClass)
    {
        List<T> childReservations = new LinkedList<T>();
        for (Reservation childReservation : this.childReservations) {
            if (reservationClass.isInstance(childReservation)) {
                childReservations.add(reservationClass.cast(childReservation));
            }
        }
        return childReservations;
    }

    /**
     * @param reservation to be added to the {@link #childReservations}
     */
    public void addChildReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (childReservations.contains(reservation) == false) {
            childReservations.add(reservation);
            reservation.setParentReservation(this);
        }
    }

    /**
     * @param reservation to be removed from the {@link #childReservations}
     */
    public void removeChildReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (childReservations.contains(reservation)) {
            childReservations.remove(reservation);
            reservation.setParentReservation(null);
        }
    }

    /**
     * Remove all {@link Reservation}s from {@link #childReservations}.
     */
    public void clearChildReservations()
    {
        while (!childReservations.isEmpty()) {
            removeChildReservation(childReservations.get(0));
        }
    }

    /**
     * @return {@link #executable}
     */
    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public Executable getExecutable()
    {
        return getLazyImplementation(executable);
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate()
    {
        if (slotStart != null && slotEnd!= null && slotStart.isAfter(slotEnd)) {
            throw new RuntimeException("Slot start can't be after slot end.");
        }
    }

    /**
     * @return set of this and all child {@link Reservation}s (recursive)
     */
    @Transient
    public Set<Reservation> getSetOfAllReservations()
    {
        Set<Reservation> reservations = new HashSet<Reservation>();
        reservations.add(this);
        getNestedReservations(reservations);
        return reservations;
    }

    /**
     * @return list of all child {@link Reservation}s (recursive, this {@link Reservation} is not included)
     */
    @Transient
    public List<Reservation> getNestedReservations()
    {
        List<Reservation> reservations = new LinkedList<Reservation>();
        getNestedReservations(reservations);
        return reservations;
    }

    /**
     * @param reservations to which will be added all child {@link Reservation}s (recursive)
     */
    @Transient
    private void getNestedReservations(Collection<Reservation> reservations)
    {
        for (Reservation childReservation : childReservations) {
            reservations.add(childReservation);
            childReservation.getNestedReservations(reservations);
        }
    }

    /**
     * @return {@link Reservation} which allocates any resources (recursively calls {@link #getAllocationReservation()})
     */
    @Transient
    public final Reservation getTargetReservation()
    {
        Reservation allocationReservation = this;
        while (true) {
            Reservation newReservation = allocationReservation.getAllocationReservation();
            if (newReservation == allocationReservation) {
                break;
            }
            else {
                allocationReservation = newReservation;
            }
        }
        return allocationReservation;
    }

    /**
     * @return {@link Reservation} which allocates any resources (it can be overridden, e.g., by
     * {@link ExistingReservation} to return proper reused {@link Reservation})
     */
    @Transient
    public Reservation getAllocationReservation()
    {
        return this;
    }

    /**
     * @return {@link Resource} which is allocated by this {@link Reservation}
     */
    @Transient
    public Resource getAllocatedResource()
    {
        return null;
    }

    /**
     * @param reservationType to cast result from {@link #getTargetReservation()}
     * @see {@link #getTargetReservation()}
     */
    @Transient
    public final <T extends Reservation> T getTargetReservation(Class<T> reservationType)
    {
        return reservationType.cast(getTargetReservation());
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return ObjectIdentifier.formatId(this);
    }

    /**
     * @param entityManager
     * @param administrator
     * @return converted {@link Reservation} to {@link cz.cesnet.shongo.controller.api.Reservation}
     */
    public cz.cesnet.shongo.controller.api.Reservation toApi(EntityManager entityManager, boolean administrator)
    {
        cz.cesnet.shongo.controller.api.Reservation api = createApi();
        toApi(api, entityManager, administrator);
        return api;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.Reservation}
     */
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.Reservation();
    }

    /**
     * @param api           {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param entityManager
     * @param admin
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, EntityManager entityManager, boolean admin)
    {
        api.setId(ObjectIdentifier.formatId(this));
        if (getReservationRequest() != null) {
            api.setReservationRequestId(ObjectIdentifier.formatId(getReservationRequest()));
        }
        api.setSlot(getSlot());
        if (getExecutable() != null) {
            cz.cesnet.shongo.controller.api.Executable executable = getExecutable().toApi(entityManager, admin);
            executable.setReservationId(api.getId());
            api.setExecutable(executable);
        }
        if (getParentReservation() != null) {
            api.setParentReservationId(ObjectIdentifier.formatId(getParentReservation()));
        }
        for (Reservation childReservation : getChildReservations()) {
            api.addChildReservationId(ObjectIdentifier.formatId(childReservation));
        }
        api.sortChildReservationIds();
    }

    /**
     * {@link Reservation} class by {@link cz.cesnet.shongo.controller.api.Reservation} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.Reservation>,
            Class<? extends Reservation>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.Reservation>,
            Class<? extends Reservation>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.Reservation.class,
                Reservation.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.AliasReservation.class,
                AliasReservation.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ExistingReservation.class,
                ExistingReservation.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ResourceReservation.class,
                ResourceReservation.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.RoomReservation.class,
                RoomReservation.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ValueReservation.class,
                ValueReservation.class);
    }

    /**
     * @param reservationApiClass
     * @return {@link Reservation} for given {@code reservationApiClass}
     */
    public static Class<? extends Reservation> getClassFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.Reservation> reservationApiClass)
    {
        Class<? extends Reservation> reservationClass = CLASS_BY_API.get(reservationApiClass);
        if (reservationClass == null) {
            throw new TodoImplementException(reservationApiClass);
        }
        return reservationClass;
    }

    /**
     * @param dateTime
     * @return true whether this {@link Reservation} takes place before given {@code dateTime} or it contains started {@link Executable}
     * false otherwise
     */
    public boolean isHistory(DateTime dateTime)
    {
        if (slotStart.isBefore(dateTime)) {
            // Reservation starts before given dateTime
            return true;
        }
        if (executable != null && executable.getState().isStarted()) {
            // Reservation has active executable
            return true;
        }
        for (Reservation childReservation : childReservations) {
            if (childReservation.isHistory(dateTime)) {
                // Reservation has a active child reservation
                return true;
            }
        }
        return false;
    }
}
