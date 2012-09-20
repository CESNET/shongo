package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.report.SpecificationNotReadyReport;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

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
public class ReservationRequest extends AbstractReservationRequest
{
    /**
     * @see {@link CreatedBy}.
     */
    private CreatedBy createdBy;

    /**
     * Start date/time from which the reservation is requested.
     */
    private DateTime requestedSlotStart;

    /**
     * End date/time to which the reservation is requested.
     */
    private DateTime requestedSlotEnd;

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
    @Column(nullable = false, columnDefinition = "varchar(255) default 'USER'")
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
     * @return {@link #requestedSlotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getRequestedSlotStart()
    {
        return requestedSlotStart;
    }

    /**
     * @param requestedSlotStart sets the {@link #requestedSlotStart}
     */
    public void setRequestedSlotStart(DateTime requestedSlotStart)
    {
        this.requestedSlotStart = requestedSlotStart;
    }

    /**
     * @return {@link #requestedSlotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getRequestedSlotEnd()
    {
        return requestedSlotEnd;
    }

    /**
     * @param requestedSlotEnd sets the {@link #requestedSlotEnd}
     */
    public void setRequestedSlotEnd(DateTime requestedSlotEnd)
    {
        this.requestedSlotEnd = requestedSlotEnd;
    }

    /**
     * @return requested slot ({@link #requestedSlotStart}, {@link #requestedSlotEnd})
     */
    @Transient
    public Interval getRequestedSlot()
    {
        return new Interval(requestedSlotStart, requestedSlotEnd);
    }

    /**
     * @param requestedSlot sets the requested slot
     */
    @Transient
    public void setRequestedSlot(Interval requestedSlot)
    {
        setRequestedSlotStart(requestedSlot.getStart());
        setRequestedSlotEnd(requestedSlot.getEnd());
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

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("state", getState());
        map.put("slot", getRequestedSlot());
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
        reservationRequestApi.setSlot(getRequestedSlot());
        reservationRequestApi.setSpecification(getSpecification().toApi(domain));
        reservationRequestApi.setState(getState().toApi());
        reservationRequestApi.setStateReport(getReportText());
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
            setRequestedSlot(reservationRequestApi.getSlot());
        }
        if (reservationRequestApi.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SPECIFICATION)) {
            cz.cesnet.shongo.controller.api.Specification specificationApi = reservationRequestApi.getSpecification();
            if (specificationApi == null) {
                setSpecification(null);
            }
            else if (getSpecification() != null && getSpecification().getId()
                    .equals(specificationApi.getId().longValue())) {
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

        /**
         * @return {@link cz.cesnet.shongo.controller.api.ReservationRequest.State} from the {@link State}
         */
        public cz.cesnet.shongo.controller.api.ReservationRequest.State toApi()
        {
            switch (this) {
                case NOT_COMPLETE:
                    return cz.cesnet.shongo.controller.api.ReservationRequest.State.NOT_COMPLETE;
                case COMPLETE:
                    return cz.cesnet.shongo.controller.api.ReservationRequest.State.NOT_ALLOCATED;
                case ALLOCATED:
                    return cz.cesnet.shongo.controller.api.ReservationRequest.State.ALLOCATED;
                case ALLOCATION_FAILED:
                    return cz.cesnet.shongo.controller.api.ReservationRequest.State.ALLOCATION_FAILED;
                default:
                    throw new TodoImplementException();
            }
        }
    }
}
