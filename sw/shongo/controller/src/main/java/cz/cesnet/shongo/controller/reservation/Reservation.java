package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation for any target. Each {@link Reservation} can contains multiple {@link #childReservations}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Reservation extends PersistentObject
{
    /**
     * User-id of an user who is owner of the {@link Reservation}.
     */
    private String userId;

    /**
     * @see {@link CreatedBy}.
     */
    private CreatedBy createdBy;

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

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
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
    }

    /**
     * @return {@link #createdBy}
     */
    @Column(nullable = false, columnDefinition = "varchar(255) default 'CONTROLLER'")
    @Enumerated(EnumType.STRING)
    public CreatedBy getCreatedBy()
    {
        return createdBy;
    }

    /**
     * @return {@link #createdBy}
     */
    public void setCreatedBy(CreatedBy createdBy)
    {
        this.createdBy = createdBy;
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
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
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
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
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    @Transient
    public void setSlot(DateTime start, DateTime end)
    {
        setSlotStart(start);
        setSlotEnd(end);
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
    @ManyToOne
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
     * @return {@link #childReservations}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentReservation")
    @Access(AccessType.FIELD)
    public List<Reservation> getChildReservations()
    {
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
     * @return {@link #executable}
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }

    /**
     * @return child {@link Reservation}s, theirs child {@link Reservation}, etc. (recursive)
     */
    @Transient
    public List<Reservation> getNestedReservations()
    {
        List<Reservation> reservations = new ArrayList<Reservation>();
        getNestedReservations(reservations);
        return reservations;
    }

    /**
     * @param reservations to which will be added child {@link Reservation}s, theirs child {@link Reservation}, etc.
     */
    @Transient
    private void getNestedReservations(List<Reservation> reservations)
    {
        for (Reservation childReservation : childReservations) {
            reservations.add(childReservation);
            childReservation.getNestedReservations(reservations);
        }
    }

    /**
     * Validate reservation.
     *
     * @param cache
     * @throws ReportException
     */
    public void validate(Cache cache) throws ReportException
    {
    }

    /**
     * @return this {@link Reservation} which allocates any resources (it can be overridden, e.g., by
     *         {@link ExistingReservation} to return proper reused {@link Reservation})
     */
    @Transient
    public Reservation getTargetReservation()
    {
        return this;
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

    @PrePersist
    protected void onCreate()
    {
        // Reservations are by default created by the controller
        if (createdBy == null) {
            createdBy = CreatedBy.CONTROLLER;
        }
    }

    /**
     * @return converted {@link Reservation} to {@link cz.cesnet.shongo.controller.api.Reservation}
     */
    public cz.cesnet.shongo.controller.api.Reservation toApi(Domain domain)
    {
        cz.cesnet.shongo.controller.api.Reservation api = createApi();
        toApi(api, domain);
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
     * @param api    {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param domain
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        api.setId(domain.formatId(getId()));
        api.setUserId(getUserId());
        api.setSlot(getSlot());
        if (getExecutable() != null) {
            api.setExecutable(getExecutable().toApi(domain));
        }
        if (getParentReservation() != null) {
            api.setParentReservationId(domain.formatId(getParentReservation().getId()));
        }
        for (Reservation childReservation : getChildReservations()) {
            api.addChildReservationId(domain.formatId(childReservation.getId()));
        }
    }

    /**
     * Enumeration defining who created the {@link Reservation}.
     */
    public static enum CreatedBy
    {
        /**
         * {@link Reservation} was created by a user. In fact user should never create the {@link Reservation} itself,
         * but it is useful, e.g., for testing purposes, to create a {@link Reservation} and to ensure that it will
         * not be deleted by the {@link Scheduler} when it delete all not-referenced {@link Reservation}s through
         * {@link ReservationManager#deleteAllNotReferenced(cz.cesnet.shongo.controller.Cache)}).
         */
        USER,

        /**
         * {@link Reservation} was created by the {@link Controller}'s {@link Scheduler} (default value in
         * the most situations).
         */
        CONTROLLER
    }
}
