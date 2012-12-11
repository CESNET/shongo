package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
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
     * {@link Specification} of target which is requested for a reservation.
     */
    private Specification specification;

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * Allocated {@link Reservation} for the {@link ReservationRequest}.
     */
    private Reservation reservation;

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
     * @return {@link #specification}
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public void setSpecification(Specification specification)
    {
        this.specification = specification;
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
        // And if allocated reservation exists, we remove reference to it and it will be deleted
        // at the start of the Scheduler
        setReservation(null);
    }

    /**
     * @return {@link #reservation}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
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
    public void updateStateBySpecifications()
    {
        State newState = getState();
        if (newState == null || newState == State.NOT_COMPLETE) {
            newState = State.COMPLETE;
        }
        List<Report> reports = new ArrayList<Report>();
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
    public cz.cesnet.shongo.controller.api.ReservationRequest.State getStateAsApi()
    {
        switch (getState()) {
            case NOT_COMPLETE:
                return cz.cesnet.shongo.controller.api.ReservationRequest.State.NOT_COMPLETE;
            case COMPLETE:
                return cz.cesnet.shongo.controller.api.ReservationRequest.State.COMPLETE;
            case ALLOCATED:
                Executable executable = getReservation().getExecutable();
                if ( executable != null) {
                    switch (executable.getState()) {
                        case STARTED:
                            return cz.cesnet.shongo.controller.api.ReservationRequest.State.STARTED;
                        case STARTING_FAILED:
                            return cz.cesnet.shongo.controller.api.ReservationRequest.State.STARTING_FAILED;
                        case STOPPED:
                            return cz.cesnet.shongo.controller.api.ReservationRequest.State.FINISHED;
                        default:
                            return cz.cesnet.shongo.controller.api.ReservationRequest.State.ALLOCATED;
                    }
                }
                return cz.cesnet.shongo.controller.api.ReservationRequest.State.ALLOCATED;
            case ALLOCATION_FAILED:
                return cz.cesnet.shongo.controller.api.ReservationRequest.State.ALLOCATION_FAILED;
            default:
                throw new TodoImplementException();
        }
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
    public final cz.cesnet.shongo.controller.api.ReservationRequest toApi(Domain domain) throws FaultException
    {
        return (cz.cesnet.shongo.controller.api.ReservationRequest) super.toApi(domain);
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        reservationRequestApi.setSlot(getSlot());
        reservationRequestApi.setSpecification(getSpecification().toApi(domain));
        reservationRequestApi.setState(getStateAsApi());
        reservationRequestApi.setStateReport(getReportText());
        if (getReservation() != null) {
            reservationRequestApi.setReservationId(domain.formatId(getReservation().getId()));
        }
        super.toApi(api, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager,
            Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        if (reservationRequestApi.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SLOT)) {
            setSlot(reservationRequestApi.getSlot());
        }
        if (reservationRequestApi.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SPECIFICATION)) {
            cz.cesnet.shongo.controller.api.Specification specificationApi = reservationRequestApi.getSpecification();
            if (specificationApi == null) {
                setSpecification(null);
            }
            else if (getSpecification() != null && getSpecification().equalsId(specificationApi.getId())) {
                getSpecification().fromApi(specificationApi, entityManager, domain);
            }
            else {
                setSpecification(Specification.createFromApi(specificationApi, entityManager, domain));
            }
        }
        super.fromApi(api, entityManager, domain);
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
