package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.Resource;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
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
    private List<SchedulerReport> reports = new ArrayList<SchedulerReport>();

    /**
     * Stack of active {@link SchedulerReport}s.
     */
    private Stack<SchedulerReport> activeReports = new Stack<SchedulerReport>();

    /**
     * Set of {@link SchedulerReport}s which should not propagate errors to parent reports.
     */
    private Set<SchedulerReport> disabledErrorPropagation = new HashSet<SchedulerReport>();

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
     * @throws SchedulerException
     */
    private Reservation performChildReservationTask(ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform();
            addReports(reservationTask);
            return reservation;
        }
        catch (SchedulerException exception) {
            addReport(exception.getTopReport());
            exception.setReport(getCurrentTopReport());
            throw exception;
        }
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final Reservation addChildReservation(ReservationTask reservationTask)
            throws SchedulerException
    {
        return addChildReservation(performChildReservationTask(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws SchedulerException
    {
        return reservationClass.cast(addChildReservation(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> Collection<R> addMultiChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws SchedulerException
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
            throws SchedulerException
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
            Class<R> reservationClass) throws SchedulerException
    {
        Reservation reservation = addChildReservation(reservationTaskProvider);
        return reservationClass.cast(reservation);
    }

    /**
     * @return {@link #reports}
     */
    public List<SchedulerReport> getReports()
    {
        return reports;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    protected <T extends SchedulerReport> T addReport(T report)
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
        for (SchedulerReport report : reservationTask.getReports()) {
            addReport(report);
        }
    }

    /**
     * @param report         to be added and to be used as parent for next reports until {@link #endReport()} is called
     * @param propagateError specifies whether error should be propagated to parent report
     */
    protected void beginReport(SchedulerReport report, boolean propagateError)
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
    protected void endReportError(SchedulerReport errorReport)
    {
        SchedulerReport report = activeReports.pop();
        disabledErrorPropagation.remove(reports);

        report.addChildReport(errorReport);
    }

    /**
     * @return current top {@link SchedulerReport}
     */
    protected SchedulerReport getCurrentTopReport()
    {
        SchedulerReport report;
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

        while (report.hasParentReport() && !disabledErrorPropagation.contains(report)) {
            report = report.getParentReport();
        }
        return report;
    }

    /**
     * @return {@link SchedulerException} which should be added to the {@link #reports}
     *         when the {@link Reservation} is started allocating, or null when no report should be added
     */
    protected SchedulerReport createMainReport()
    {
        return null;
    }

    /**
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws SchedulerException when the {@link ReservationTask} failed
     */
    public final Reservation perform() throws SchedulerException
    {
        Reservation reservation = null;
        SchedulerReport mainReport = createMainReport();
        if (mainReport != null) {
            beginReport(mainReport, false);
            try {
                reservation = createReservation();
                validateReservation(reservation);
            }
            catch (SchedulerException exception) {
                SchedulerReport report = exception.getTopReport();
                if (report != mainReport) {
                    // Report from exception isn't added to root report so we add it and throw the root report
                    addReport(report);
                    exception.setReport(mainReport);
                    report = mainReport;
                }
                throw new SchedulerException(report);
            }
            finally {
                endReport();
            }
        }
        else {
            reservation = createReservation();
        }

        // Add child reservations
        for (Reservation childReservation : getChildReservations()) {
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
     * @throws SchedulerException when the {@link ReservationTask} failed
     */
    public final <R> R perform(Class<R> reservationClass) throws SchedulerException
    {
        return reservationClass.cast(perform());
    }

    /**
     * @return created {@link Reservation}
     * @throws SchedulerException when the {@link Reservation} cannot be created
     */
    protected abstract Reservation createReservation() throws SchedulerException;

    /**
     * @param type to be validated
     * @throws SchedulerReportSet.DurationLongerThanMaximumException
     *          when the validation failed
     */
    protected void validateReservationSlot(Class<? extends Reservation> type)
            throws SchedulerReportSet.DurationLongerThanMaximumException
    {
        // Check maximum duration
        if (context.isMaximumFutureAndDurationRestricted()) {
            if (type.equals(ResourceReservation.class)) {
                checkMaximumDuration(getInterval(), context.getCache().getResourceReservationMaximumDuration());
            }
            else if (type.equals(ValueReservation.class) || type.equals(AliasReservation.class)) {
                checkMaximumDuration(getInterval(), context.getCache().getValueReservationMaximumDuration());
            }
        }
    }

    /**
     * @param reservation to be validated
     * @throws SchedulerException when the validation failed
     */
    protected void validateReservation(Reservation reservation) throws SchedulerException
    {
        validateReservationSlot(reservation.getClass());
    }

    /**
     * Check maximum duration.
     *
     * @param slot            to be checked
     * @param maximumDuration maximum allowed duration
     * @throws SchedulerReportSet.DurationLongerThanMaximumException
     *
     */
    private static void checkMaximumDuration(Interval slot, Period maximumDuration)
            throws SchedulerReportSet.DurationLongerThanMaximumException
    {
        Period duration = Temporal.getIntervalDuration(slot);
        if (Temporal.isPeriodLongerThan(duration, maximumDuration)) {
            throw new SchedulerReportSet.DurationLongerThanMaximumException(duration, maximumDuration);
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
         * @see AuthorizationManager
         */
        private AuthorizationManager authorizationManager;

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
         * @param entityManager      which can be used
         */
        public Context(ReservationRequest reservationRequest, Cache cache, Interval interval,
                EntityManager entityManager)
        {
            this(reservationRequest.getUserId(), cache, interval);
            this.reservationRequest = reservationRequest;
            if (entityManager != null) {
                this.authorizationManager = new AuthorizationManager(entityManager);
            }
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
         * @return {@link #authorizationManager}
         */
        public AuthorizationManager getAuthorizationManager()
        {
            return authorizationManager;
        }

        /**
         * @param resource whose owner should be checked
         * @return true if the {@link #reservationRequest} has an owner who is in the given {@code userIds},
         *         false otherwise
         */
        public boolean containsOwnerId(Resource resource)
        {
            if (reservationRequest == null) {
                throw new IllegalStateException("Reservation request must not be null.");
            }
            if (authorizationManager == null) {
                throw new IllegalStateException("Authorization manager must not be null.");
            }
            Set<String> resourceOwnerIds = new HashSet<String>();
            EntityIdentifier resourceId = new EntityIdentifier(resource);
            resourceOwnerIds.addAll(authorizationManager.getUserIdsWithRole(resourceId, Role.OWNER));
            if (resourceOwnerIds.size() == 0) {
                resourceOwnerIds.add(resource.getUserId());
            }

            Set<String> ownerIds = new HashSet<String>();
            EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
            ownerIds.addAll(authorizationManager.getUserIdsWithRole(reservationRequestId, Role.OWNER));
            if (ownerIds.size() == 0) {
                ownerIds.add(reservationRequest.getUserId());
            }
            return !Collections.disjoint(resourceOwnerIds, ownerIds);
        }
    }
}
