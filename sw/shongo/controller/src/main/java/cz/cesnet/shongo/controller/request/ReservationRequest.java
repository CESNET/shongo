package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a request created by an user to get allocated some resources for video conference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends AbstractReservationRequest implements Reporter.ReportContext
{
    /**
     * {@link Allocation} for which this {@link ReservationRequest} has been created as child.
     */
    private Allocation parentAllocation;

    /**
     * Start date/time from which the reservation is requested.
     */
    private DateTime slotStart;

    /**
     * End date/time to which the reservation is requested.
     */
    private DateTime slotEnd;

    /**
     * {@link cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState} of this {@link ReservationRequest}.
     */
    private AllocationState allocationState;

    /**
     * List of {@link SchedulerReport}s for this {@link ReservationRequest}.
     */
    private List<SchedulerReport> reports = new ArrayList<SchedulerReport>();

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * @return {@link #parentAllocation}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Allocation getParentAllocation()
    {
        return parentAllocation;
    }

    /**
     * @param parentAllocation sets the {@link #parentAllocation}
     */
    public void setParentAllocation(Allocation parentAllocation)
    {
        // Manage bidirectional association
        if (parentAllocation != this.parentAllocation) {
            if (this.parentAllocation != null) {
                Allocation oldParentAllocation = this.parentAllocation;
                this.parentAllocation = null;
                oldParentAllocation.removeChildReservationRequest(this);
            }
            if (parentAllocation != null) {
                this.parentAllocation = parentAllocation;
                this.parentAllocation.addChildReservationRequest(this);
            }
        }
    }

    /**
     * @return {@link #slotStart}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "DateTime")
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
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "DateTime")
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
     * @return {@link #allocationState}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    /**
     * @param allocationState sets the {@link #allocationState}
     */
    public void setAllocationState(AllocationState allocationState)
    {
        this.allocationState = allocationState;
    }

    /**
     * Clear {@link #allocationState}, useful for removing {@link cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState#ALLOCATED} state.
     */
    public void clearState()
    {
        this.allocationState = null;
    }

    /**
     * @return {@link #reports}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<SchedulerReport> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param reports sets the {@link #reports}
     */
    public void setReports(List<SchedulerReport> reports)
    {
        this.reports.clear();
        for (SchedulerReport report : reports) {
            this.reports.add(report);
        }
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(SchedulerReport report)
    {
        reports.add(report);
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(SchedulerReport report)
    {
        reports.remove(report);
    }

    /**
     * Remove all {@link SchedulerReport}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * @return report string containing report header and all {@link #reports}
     */
    @Transient
    public String getReportText(Report.MessageType messageType)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (SchedulerReport report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append(report.getMessageRecursive(messageType));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    /**
     * Update state of the {@link ReservationRequest} based on {@link #specification}.
     * <p/>
     * If {@link #specification} is instance of {@link StatefulSpecification} and it's
     * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}
     * the state of {@link ReservationRequest} is set to {@link cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState#NOT_COMPLETE}.
     * Otherwise the state is not changed or forced to {@link cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState#COMPLETE} in incorrect cases.
     *
     * @see cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState
     */
    public void updateStateBySpecification()
    {
        AllocationState newAllocationState = getAllocationState();
        if (newAllocationState == null || newAllocationState == ReservationRequest.AllocationState.NOT_COMPLETE) {
            newAllocationState = ReservationRequest.AllocationState.COMPLETE;
        }
        List<SchedulerReport> reports = new ArrayList<SchedulerReport>();
        Specification specification = getSpecification();
        if (specification instanceof StatefulSpecification) {
            StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
            if (statefulSpecification.getCurrentState().equals(StatefulSpecification.State.NOT_READY)) {
                newAllocationState = ReservationRequest.AllocationState.NOT_COMPLETE;
                reports.add(new SchedulerReportSet.SpecificationNotReadyReport(specification));
            }
        }

        if (newAllocationState != getAllocationState()) {
            setAllocationState(newAllocationState);
            setReports(reports);
        }
    }

    @Override
    protected void onCreate()
    {
        super.onCreate();

        if (allocationState == null) {
            updateStateBySpecification();
        }
    }

    /**
     * @return {@link #allocationState} converted to API
     */
    @Transient
    public cz.cesnet.shongo.controller.api.AllocationState getAllocationStateAsApi()
    {
        switch (getAllocationState()) {
            case NOT_COMPLETE:
                return cz.cesnet.shongo.controller.api.AllocationState.NOT_COMPLETE;
            case COMPLETE:
                return cz.cesnet.shongo.controller.api.AllocationState.NOT_ALLOCATED;
            case ALLOCATED:
                Reservation reservation = getAllocation().getCurrentReservation();
                if (reservation != null) {
                    Executable executable = reservation.getExecutable();
                    if (executable != null) {
                        switch (executable.getState()) {
                            case STARTED:
                                return cz.cesnet.shongo.controller.api.AllocationState.STARTED;
                            case MODIFIED:
                                return cz.cesnet.shongo.controller.api.AllocationState.STARTED;
                            case STARTING_FAILED:
                                return cz.cesnet.shongo.controller.api.AllocationState.STARTING_FAILED;
                            case STOPPED:
                                return cz.cesnet.shongo.controller.api.AllocationState.FINISHED;
                            case STOPPING_FAILED:
                                return cz.cesnet.shongo.controller.api.AllocationState.STARTED;
                            default:
                                return cz.cesnet.shongo.controller.api.AllocationState.ALLOCATED;
                        }
                    }
                }
                return cz.cesnet.shongo.controller.api.AllocationState.ALLOCATED;
            case ALLOCATION_FAILED:
                return cz.cesnet.shongo.controller.api.AllocationState.ALLOCATION_FAILED;
            default:
                throw new TodoImplementException();
        }
    }

    @Transient
    @Override
    public String getReportContextName()
    {
        return "reservation request " + EntityIdentifier.formatId(this);
    }

    @Transient
    @Override
    public String getReportContextDetail()
    {
        return null;
    }

    @Override
    public void validate() throws CommonReportSet.EntityInvalidException
    {
        validateSlotDuration(getSlot().toPeriod());

        super.validate();
    }

    @Override
    public ReservationRequest clone()
    {
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.synchronizeFrom(this);
        return reservationRequest;
    }

    @Override
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest)
    {
        boolean modified = super.synchronizeFrom(abstractReservationRequest);
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            modified |= !ObjectHelper.isSame(getSlotStart(), reservationRequest.getSlotStart())
                    || !ObjectHelper.isSame(getSlotEnd(), reservationRequest.getSlotEnd());

            setSlotStart(reservationRequest.getSlotStart());
            setSlotEnd(reservationRequest.getSlotEnd());
        }
        return modified;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi()
    {
        return new cz.cesnet.shongo.controller.api.ReservationRequest();
    }

    @Override
    public cz.cesnet.shongo.controller.api.ReservationRequest toApi(boolean admin)
    {
        return (cz.cesnet.shongo.controller.api.ReservationRequest) super.toApi(admin);
    }

    @Override
    public final cz.cesnet.shongo.controller.api.ReservationRequest toApi(Report.MessageType messageType)
    {
        return (cz.cesnet.shongo.controller.api.ReservationRequest) super.toApi(messageType);
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Report.MessageType messageType)
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        reservationRequestApi.setSlot(getSlot());
        reservationRequestApi.setAllocationState(getAllocationStateAsApi());
        reservationRequestApi.setAllocationStateReport(getReportText(messageType));
        for (Reservation reservation : getAllocation().getReservations()) {
            reservationRequestApi.addReservationId(EntityIdentifier.formatId(reservation));
        }
        super.toApi(api, messageType);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequestApi =
                (cz.cesnet.shongo.controller.api.ReservationRequest) api;
        setSlot(reservationRequestApi.getSlot());
        super.fromApi(api, entityManager);
    }

    /**
     * Enumeration of {@link ReservationRequest} allocation state.
     */
    public static enum AllocationState
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
        ALLOCATION_FAILED,
    }
}
