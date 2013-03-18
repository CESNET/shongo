package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.scheduler.report.DurationLongerThanMaximumReport;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.*;

/**
 * Represents a {@link Scheduler} task which results into {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReservationTask
{
    /**
     * Context.
     */
    private Context context;

    /**
     * List of child {@link Reservation}s.
     */
    private List<Reservation> childReservations = new ArrayList<Reservation>();

    /**
     * List of reports.
     */
    private List<Report> reports = new ArrayList<Report>();

    /**
     * Stack of active {@link Report}s.
     */
    private Stack<Report> activeReports = new Stack<Report>();

    /**
     * Set of {@link Report}s which should not propagate errors to parent reports.
     */
    private Set<Report> disabledErrorPropagation = new HashSet<Report>();

    /**
     * Constructor.
     */
    public ReservationTask(Context context)
    {
        this.context = context;
    }

    /**
     * @return {@link #context}
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * @return {@link Context#getInterval()}
     */
    public Interval getInterval()
    {
        return context.getInterval();
    }

    /**
     * @return {@link Context#cache}
     */
    public Cache getCache()
    {
        return context.getCache();
    }

    /**
     * @return {@link Context#cacheTransaction}
     */
    protected CacheTransaction getCacheTransaction()
    {
        return context.getCacheTransaction();
    }

    /**
     * @return {@link #childReservations}
     */
    protected List<Reservation> getChildReservations()
    {
        return childReservations;
    }

    /**
     * @param reservation to be added to the {@link #childReservations}
     * @return given {@code reservation} or {@link ExistingReservation#getTargetReservation()}
     */
    public Reservation addChildReservation(Reservation reservation)
    {
        childReservations.add(reservation);
        getCacheTransaction().addAllocatedReservation(reservation);
        return reservation.getTargetReservation();
    }

    /**
     * @param reservation to be added to the {@link #childReservations}
     * @return given {@code reservation} or {@link ExistingReservation#getTargetReservation()}
     */
    public final <R extends Reservation> R addChildReservation(Reservation reservation, Class<R> reservationClass)
    {
        childReservations.add(reservation);
        getCacheTransaction().addAllocatedReservation(reservation);
        return reservationClass.cast(reservation.getTargetReservation());
    }

    /**
     * @param reservationTask to be performed
     * @return resulting {@link Reservation}
     * @throws ReportException
     */
    private Reservation performChildReservationTask(ReservationTask reservationTask) throws ReportException
    {
        try {
            Reservation reservation = reservationTask.perform();
            addReports(reservationTask);
            return reservation;
        }
        catch (ReportException exception) {
            addReport(exception.getTopReport());
            exception.setReport(createReportFailureForThrowing());
            throw exception;
        }
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final Reservation addChildReservation(ReservationTask reservationTask)
            throws ReportException
    {
        return addChildReservation(performChildReservationTask(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws ReportException
    {
        return reservationClass.cast(addChildReservation(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> Collection<R> addMultiChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws ReportException
    {
        Reservation reservation = performChildReservationTask(reservationTask);
        List<R> reservations = new ArrayList<R>();
        Reservation targetReservation = reservation.getTargetReservation();
        if (reservationClass.isInstance(targetReservation)) {
            childReservations.add(reservation);
            reservations.add(reservationClass.cast(targetReservation));
        }
        else {
            for (Reservation childReservation : reservation.getChildReservations()) {
                childReservations.add(childReservation);
                reservations.add(childReservation.getTargetReservation(reservationClass));
            }
        }
        return reservations;
    }

    /**
     * Add child {@link Reservation} to the task allocated from a {@link ReservationTask} created from given
     * {@code reservationTaskProvider}.
     *
     * @param reservationTaskProvider used for creating {@link ReservationTask}
     */
    public final Reservation addChildReservation(ReservationTaskProvider reservationTaskProvider)
            throws ReportException
    {
        ReservationTask reservationTask = reservationTaskProvider.createReservationTask(getContext());
        return addChildReservation(reservationTask);
    }

    /**
     * Add child {@link Reservation} to the task allocated from a {@link ReservationTask} created from given
     * {@code reservationTaskProvider}.
     *
     * @param reservationTaskProvider used for creating {@link ReservationTask}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTaskProvider reservationTaskProvider,
            Class<R> reservationClass) throws ReportException
    {
        Reservation reservation = addChildReservation(reservationTaskProvider);
        return reservationClass.cast(reservation);
    }

    /**
     * @return {@link #reports}
     */
    public List<Report> getReports()
    {
        return reports;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    protected <T extends Report> T addReport(T report)
    {
        if (activeReports.empty()) {
            reports.add(report);
        }
        else {
            activeReports.peek().addChildReport(report);
        }
        return report;
    }

    /**
     * Add report from another {@code reservationTask}.
     *
     * @param reservationTask
     */
    protected void addReports(ReservationTask reservationTask)
    {
        for (Report report : reservationTask.getReports()) {
            addReport(report);
        }
    }

    /**
     * @param report         to be added and to be used as parent for next reports until {@link #endReport()} is called
     * @param propagateError specifies whether error should be propagated to parent report
     */
    protected void beginReport(Report report, boolean propagateError)
    {
        addReport(report);
        activeReports.push(report);
        if (!propagateError) {
            disabledErrorPropagation.add(report);
        }
    }

    /**
     * Stop using active report as parent for next reports
     */
    protected void endReport()
    {
        activeReports.pop();
        disabledErrorPropagation.remove(reports);
    }

    /**
     * Stop using active report as parent for next reports
     *
     * @param errorReport to be added to the active report as error
     */
    protected void endReportError(Report errorReport)
    {
        Report report = activeReports.pop();
        disabledErrorPropagation.remove(reports);

        errorReport.setState(Report.State.ERROR);
        report.setState(Report.State.ERROR);
        report.addChildReport(errorReport);
    }

    /**
     * @return {@link Report} in {@link Report.State#ERROR} for throwing
     */
    protected Report createReportFailureForThrowing()
    {
        Report report;
        if (activeReports.empty()) {
            if (reports.size() == 0) {
                throw new IllegalStateException("No report is active");
            }
            // Use the last added
            report = reports.get(reports.size() - 1);
        }
        else {
            report = activeReports.peek();
        }

        report.setState(Report.State.ERROR);
        while (report.hasParentReport() && !disabledErrorPropagation.contains(report)) {
            report = report.getParentReport();
            report.setState(Report.State.ERROR);
        }
        return report;
    }

    /**
     * @return {@link Report} in {@link Report.State#ERROR} for throwing
     */
    protected void throwNewReportFailure(Report report) throws ReportException
    {
        addReport(report);
        report.setState(Report.State.ERROR);
        while (report.hasParentReport()) {
            report = report.getParentReport();
            report.setState(Report.State.ERROR);
        }
        throw report.exception();
    }

    /**
     * @return {@link Report} which should be added to the {@link #reports} when the {@link Reservation} is started
     *         allocating, or null when no report should be added
     */
    protected Report createdMainReport()
    {
        return null;
    }

    /**
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws ReportException when the {@link ReservationTask} failed
     */
    public final Reservation perform() throws ReportException
    {
        Reservation reservation = null;
        Report report = createdMainReport();
        if (report != null) {
            beginReport(report, false);
            try {
                reservation = createReservation();
                validateReservation(reservation);
            }
            catch (ReportException exception) {
                Report exceptionReport = exception.getTopReport();
                if (exceptionReport != report) {
                    // Report from exception isn't added to root report so we add it and throw the root report
                    addReport(exceptionReport);
                    exception.setReport(report);
                }
                throw exception;
            }
            finally {
                endReport();
            }
        }
        else {
            reservation = createReservation();
        }

        reservation.setUserId(context.getUserId());
        for (Reservation childReservation : getChildReservations()) {
            childReservation.setUserId(context.getUserId());
            reservation.addChildReservation(childReservation);
        }

        // Add reservation to the cache
        getCacheTransaction().addAllocatedReservation(reservation);

        return reservation;
    }

    /**
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws ReportException when the {@link ReservationTask} failed
     */
    public final <R> R perform(Class<R> reservationClass) throws ReportException
    {
        return reservationClass.cast(perform());
    }

    /**
     * @return created {@link Reservation}
     * @throws ReportException when the {@link Reservation} cannot be created
     */
    protected abstract Reservation createReservation() throws ReportException;

    /**
     * @param reservation to be validated
     * @throws ReportException when the validation failed
     */
    protected void validateReservationSlot(Reservation reservation) throws ReportException
    {
        // Check maximum duration
        if (context.isMaximumFutureAndDurationRestricted()) {
            if (reservation instanceof ResourceReservation) {
                checkMaximumDuration(reservation.getSlot(), context.getCache().getResourceReservationMaximumDuration());
            }
            else if (reservation instanceof ValueReservation) {
                checkMaximumDuration(reservation.getSlot(), context.getCache().getValueReservationMaximumDuration());
            }
        }
    }

    /**
     * @param reservation to be validated
     * @throws ReportException when the validation failed
     */
    protected void validateReservation(Reservation reservation) throws ReportException
    {
        validateReservationSlot(reservation);
    }

    /**
     * Check maximum duration.
     *
     * @param slot            to be checked
     * @param maximumDuration maximum allowed duration
     * @throws ReportException
     */
    private static void checkMaximumDuration(Interval slot, Period maximumDuration) throws ReportException
    {
        Period duration = Temporal.getIntervalDuration(slot);
        if (Temporal.isPeriodLongerThan(duration, maximumDuration)) {
            throw new DurationLongerThanMaximumReport(duration, maximumDuration).exception();
        }
    }

    /**
     * Context for the {@link ReservationTask}.
     */
    public static class Context
    {
        /**
         * User-id to owner of new reservations.
         */
        private String userId;

        /**
         * {@link AbstractReservationRequest} for which the {@link Reservation} should be allocated.
         */
        private ReservationRequest reservationRequest;

        /**
         * @see Cache
         */
        private Cache cache;

        /**
         * @see {@link CacheTransaction}
         */
        private CacheTransaction cacheTransaction;

        /**
         * Constructor.
         *
         * @param userId   sets the {@link #userId}
         * @param cache    sets the {@link #cache}
         * @param interval sets the {@link CacheTransaction#interval}
         */
        public Context(String userId, Cache cache, Interval interval)
        {
            this.userId = userId;
            this.cache = cache;
            this.cacheTransaction = new CacheTransaction(interval);
        }

        /**
         * Constructor.
         *
         * @param reservationRequest sets the {@link #reservationRequest}
         * @param cache              sets the {@link #cache}
         * @param interval           sets the {@link CacheTransaction#interval}
         */
        public Context(ReservationRequest reservationRequest, Cache cache, Interval interval)
        {
            this(reservationRequest.getUserId(), cache, interval);
            this.reservationRequest = reservationRequest;
        }

        /**
         * Constructor.
         *
         * @param cache    sets the {@link #cache}
         * @param interval sets the {@link CacheTransaction#interval}
         */
        public Context(Cache cache, Interval interval)
        {
            this(Authorization.ROOT_USER_ID, cache, interval);
        }

        /**
         * @return description of {@link #reservationRequest}
         */
        public String getReservationDescription()
        {
            if (reservationRequest == null) {
                return null;
            }
            return reservationRequest.getDescription();
        }

        /**
         * @return {@link #userId}
         */
        public String getUserId()
        {
            return userId;
        }

        /**
         * @return true whether executables should be allocated,
         *         false otherwise
         */
        public boolean isExecutableAllowed()
        {
            return reservationRequest == null || reservationRequest.getPurpose().isExecutableAllowed();
        }

        /**
         * @return true whether only owned resource by the reservation request owner can be allocated,
         *         false otherwise
         */
        public boolean isOwnerRestricted()
        {
            return reservationRequest != null && reservationRequest.getPurpose().isByOwner();
        }

        /**
         * @return true whether maximum future and maximum duration should be checked,
         *         false otherwise
         */
        public boolean isMaximumFutureAndDurationRestricted()
        {
            return reservationRequest != null && !reservationRequest.getPurpose().isByOwner();
        }

        /**
         * @return {@link CacheTransaction#interval}
         */
        public Interval getInterval()
        {
            return cacheTransaction.getInterval();
        }

        /**
         * @return {@link #cache}
         */
        public Cache getCache()
        {
            return cache;
        }

        /**
         * @return {@link #cacheTransaction}
         */
        public CacheTransaction getCacheTransaction()
        {
            return cacheTransaction;
        }

        /**
         * @param userIds       to be checked
         * @param authorization which is used for retrieving owners for the {@link #reservationRequest}
         * @return true if the {@link #reservationRequest} has an owner who is in the given {@code userIds},
         *         false otherwise
         */
        public boolean containsOwnerId(Set<String> userIds, Authorization authorization)
        {
            if (reservationRequest == null) {
                throw new IllegalStateException("Reservation request must not be null.");
            }
            Set<String> ownerIds = authorization.getUserIdsWithRole(reservationRequest, Role.OWNER);
            ownerIds.retainAll(userIds);
            return ownerIds.size() > 0;
        }
    }
}
