package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.api.ReservationRequestState;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.report.SpecificationNotReadyReport;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a request created by an user to get allocated some resources for video conference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends NormalReservationRequest
{
    /**
     * @see {@link CreatedBy}.
     */
    private CreatedBy createdBy;

    /**
     * Start date/time from which the reservation is requested.
     */
    private DateTime slotStart;

    /**
     * End date/time to which the reservation is requested.
     */
    private DateTime slotEnd;

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * Allocated {@link Reservation} for the {@link ReservationRequest}.
     */
    private Reservation reservation;

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param userId sets the {@link #setUserId(String)}
     */
    public ReservationRequest(String userId)
    {
        this.setUserId(userId);
    }

    /**
     * @return {@link #createdBy}
     */
    @Column(nullable = false)
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
     * @return requested slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the requested slot
     */
    @Transient
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * @param dateTime sets date/ime of the requested slot
     * @param duration sets duration of the requested slot
     */
    @Transient
    public void setSlot(DateTime dateTime, Period duration)
    {
        setSlot(new Interval(dateTime, duration));
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
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
     * Clear {@link #state}, useful for removing {@link State#ALLOCATED} state.
     */
    public void clearState()
    {
        this.state = null;
    }

    /**
     * @return {@link #reservation}
     */
    @Transient
    public Reservation getReservation()
    {
        if (reservations.size() == 0) {
            return null;
        }
        else if (reservations.size() == 1) {
            return reservations.get(0);
        }
        else {
            throw new IllegalStateException("Only one reservation is allowed.");
        }
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    @Transient
    public void setReservation(Reservation reservation)
    {
        if (reservations.size() > 1) {
            throw new IllegalStateException("Only one reservation is allowed.");
        }
        if (reservation == null) {
            reservations.clear();
        }
        else {
            super.addReservation(reservation);
        }
    }

    @Override
    public void addReservation(Reservation reservation)
    {
        if (reservations.size() > 1) {
            throw new IllegalStateException("Only one reservation is allowed.");
        }
        reservations.clear();
        super.addReservation(reservation);
    }

    /**
     * Update state of the {@link ReservationRequest} based on {@link #specification}.
     * <p/>
     * If {@link #specification} is instance of {@link StatefulSpecification} and it's
     * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}
     * the state of {@link ReservationRequest} is set to {@link State#NOT_COMPLETE}.
     * Otherwise the state is not changed or forced to {@link State#COMPLETE} in incorrect cases.
     *
     * @see State
     */
    public void updateStateBySpecification()
    {
        State newState = getState();
        if (newState == null || newState == State.NOT_COMPLETE) {
            newState = State.COMPLETE;
        }
        List<Report> reports = new ArrayList<Report>();
        Specification specification = getSpecification();
        if (specification instanceof StatefulSpecification) {
            StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
            if (statefulSpecification.getCurrentState().equals(StatefulSpecification.State.NOT_READY)) {
                newState = State.NOT_COMPLETE;
                reports.add(new SpecificationNotReadyReport(specification));
            }
        }

        if (newState != getState()) {
            setState(newState);
            setReports(reports);
        }
    }

    /**
     * @return {@link #state} converted to API
     */
    @Transient
    public cz.cesnet.shongo.controller.api.ReservationRequestState getStateAsApi()
    {
        switch (getState()) {
            case NOT_COMPLETE:
                return cz.cesnet.shongo.controller.api.ReservationRequestState.NOT_COMPLETE;
            case COMPLETE:
                return ReservationRequestState.NOT_ALLOCATED;
            case ALLOCATED:
                Reservation reservation = getReservation();
                if (reservation == null) {
                    throw new IllegalStateException("Allocated reservation request should have a reservation.");
                }
                Executable executable = reservation.getExecutable();
                if (executable != null) {
                    switch (executable.getState()) {
                        case STARTED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTED;
                        case STARTING_FAILED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTING_FAILED;
                        case STOPPED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.FINISHED;
                        case STOPPING_FAILED:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.STARTED;
                        default:
                            return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATED;
                    }
                }
                return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATED;
            case ALLOCATION_FAILED:
                return cz.cesnet.shongo.controller.api.ReservationRequestState.ALLOCATION_FAILED;
            default:
                throw new TodoImplementException();
        }
    }

    /**
     * @return true if {@link ReservationRequest} is in {@link State#ALLOCATED} and the {@link #reservation} is filled
     *         false otherwise
     */
    @Transient
    private boolean isReservationAllocated()
    {
        return getState().equals(State.ALLOCATED) && getReservation() != null;
    }

    @PrePersist
    protected void onCreate()
    {
        super.onCreate();

        // Reservation requests are by default created by user
        if (createdBy == null) {
            createdBy = CreatedBy.USER;
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("state", getState());
        map.put("slot", getSlot());
        map.put("specification", getSpecification());
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.ReservationRequest();
    }

    @Override
    public final cz.cesnet.shongo.controller.api.ReservationRequest toApi() throws FaultException
    {
        return (cz.cesnet.shongo.controller.api.ReservationRequest) super.toApi();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        reservationRequestApi.setSlot(getSlot());
        reservationRequestApi.setState(getStateAsApi());
        reservationRequestApi.setStateReport(getReportText());
        if (isReservationAllocated()) {
            reservationRequestApi.setReservationId(Domain.getLocalDomain().formatId(getReservation()));
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        if (reservationRequestApi.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SLOT)) {
            setSlot(reservationRequestApi.getSlot());
        }
        super.fromApi(api, entityManager);
    }

    /**
     * Enumeration defining who created the {@link ReservationRequest}.
     */
    public static enum CreatedBy
    {
        /**
         * {@link ReservationRequest} was created by a user.
         */
        USER,

        /**
         * {@link ReservationRequest} was created by the {@link cz.cesnet.shongo.controller.Controller}.
         */
        CONTROLLER
    }

    /**
     * Enumeration of {@link ReservationRequest} state.
     */
    public static enum State
    {
        /**
         * Specification is instance of {@link StatefulSpecification} and it's
         * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}.
         * <p/>
         * A {@link ReservationRequest} in {@link #NOT_COMPLETE} state become {@link #COMPLETE} when
         * the {@link Specification} become {@link StatefulSpecification.State#READY}
         * or {@link StatefulSpecification.State#SKIP}.
         */
        NOT_COMPLETE,

        /**
         * Specification is not instance of {@link StatefulSpecification} or it has
         * {@link StatefulSpecification#getCurrentState()} {@link StatefulSpecification.State#READY} or
         * {@link StatefulSpecification.State#SKIP}.
         * <p/>
         * The {@link ReservationRequest} hasn't been allocated by the {@link Scheduler} yet or
         * the {@link ReservationRequest} has been modified so the {@link Scheduler} must reallocate it.
         * <p/>
         * The {@link Scheduler} processes only {@link #COMPLETE} {@link ReservationRequest}s.
         */
        COMPLETE,

        /**
         * {@link ReservationRequest} has been successfully allocated. If the {@link ReservationRequest} becomes
         * modified, it's state changes back to {@link #COMPLETE}.
         */
        ALLOCATED,

        /**
         * Allocation of the {@link ReservationRequest} failed. The reason can be found from
         * the {@link ReservationRequest#getReports()}
         */
        ALLOCATION_FAILED;
    }
}
