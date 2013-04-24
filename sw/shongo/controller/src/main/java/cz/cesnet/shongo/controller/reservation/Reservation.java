package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.Reportable;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an allocation for any target. Each {@link Reservation} can contains multiple {@link #childReservations}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Reservation extends PersistentObject implements Reportable
{
    /**
     * @see {@link CreatedBy}.
     */
    private CreatedBy createdBy;

    /**
     * {@link ReservationRequest} for which the {@link Reservation} is allocated.
     */
    private ReservationRequest reservationRequest;

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
     * @return {@link #reservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public ReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @return top {@link AbstractReservationRequest} which was created by a user
     */
    @Transient
    public AbstractReservationRequest getTopReservationRequest()
    {
        if (reservationRequest != null) {
            ReservationRequestSet reservationRequestSet = reservationRequest.getReservationRequestSet();
            if (reservationRequestSet != null) {
                return reservationRequestSet;
            }
        }
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(ReservationRequest reservationRequest)
    {
        // Manage bidirectional association
        if (reservationRequest != this.reservationRequest) {
            if (this.reservationRequest != null) {
                ReservationRequest oldReservationRequest = this.reservationRequest;
                this.reservationRequest = null;
                oldReservationRequest.setReservation(null);
            }
            if (reservationRequest != null) {
                this.reservationRequest = reservationRequest;
                this.reservationRequest.setReservation(this);
            }
        }
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

    @Override
    @Transient
    public String getReportDescription(Report.MessageType messageType)
    {
        return String.format("reservation '%s'", EntityIdentifier.formatId(this));
    }

    /**
     * @return converted {@link Reservation} to {@link cz.cesnet.shongo.controller.api.Reservation}
     */
    public cz.cesnet.shongo.controller.api.Reservation toApi()
    {
        cz.cesnet.shongo.controller.api.Reservation api = createApi();
        toApi(api);
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
     * @param api {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api)
    {
        api.setId(EntityIdentifier.formatId(this));
        if (getReservationRequest() != null) {
            api.setReservationRequestId(EntityIdentifier.formatId(getReservationRequest()));
        }
        api.setSlot(getSlot());
        if (getExecutable() != null) {
            api.setExecutable(getExecutable().toApi());
        }
        if (getParentReservation() != null) {
            api.setParentReservationId(EntityIdentifier.formatId(getParentReservation()));
        }
        for (Reservation childReservation : getChildReservations()) {
            api.addChildReservationId(EntityIdentifier.formatId(childReservation));
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
         * {@link cz.cesnet.shongo.controller.reservation.ReservationManager#getReservationsForDeletion()}).
         */
        USER,

        /**
         * {@link Reservation} was created by the {@link Controller}'s {@link Scheduler} (default value in
         * the most situations).
         */
        CONTROLLER
    }
}
