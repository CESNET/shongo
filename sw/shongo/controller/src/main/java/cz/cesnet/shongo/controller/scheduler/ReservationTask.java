package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

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
     * Current {@link Report}.
     */
    private Report currentReport = null;

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
     * @return {@link #currentReport}
     */
    public Report getCurrentReport()
    {
        return currentReport;
    }

    /**
     * @return {@link Context#reports}
     */
    public List<Report> getReports()
    {
        return context.getReports();
    }

    /**
     * @param report to be added to the {@link Context#reports}
     */
    protected void addReport(Report report)
    {
        if (currentReport != null) {
            currentReport.addChildReport(report);
        }
        else {
            context.addReport(report);
        }
    }

    /**
     * @param report to be added and to be used as parent for next reports until {@link #endReport()} is called
     */
    protected void beginReport(Report report)
    {
        addReport(report);
        currentReport = report;
    }

    /**
     * Stop using {@link #currentReport} as parent for next reports
     */
    protected void endReport()
    {
        if (currentReport == null) {
            throw new IllegalArgumentException("Current report should not be null.");
        }
        currentReport = currentReport.getParentReport();
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
    protected Cache.Transaction getCacheTransaction()
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
     */
    public void addChildReservation(Reservation reservation)
    {
        childReservations.add(reservation);
        getCacheTransaction().addAllocatedReservation(reservation);
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final Reservation addChildReservation(ReservationTask reservationTask)
            throws ReportException
    {
        Reservation reservation = reservationTask.perform();
        addChildReservation(reservation);
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            reservation = existingReservation.getTargetReservation();
        }
        return reservation;
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws ReportException
    {
        Reservation reservation = addChildReservation(reservationTask);
        return reservationClass.cast(reservation);
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
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws ReportException when the {@link ReservationTask} failed
     */
    public final Reservation perform() throws ReportException
    {
        Reservation reservation = createReservation();
        reservation.setUserId(context.getUserId());
        for (Reservation childReservation : getChildReservations()) {
            childReservation.setUserId(context.getUserId());
            reservation.addChildReservation(childReservation);
        }
        reservation.validate(getCache());

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
     * Context for the {@link ReservationTask}.
     */
    public static class Context
    {
        /**
         * {@link AbstractReservationRequest} for which the {@link Reservation} should be allocated.
         */
        private AbstractReservationRequest reservationRequest;

        /**
         * @see Cache
         */
        private Cache cache;

        /**
         * @see {@link Cache.Transaction}
         */
        private Cache.Transaction cacheTransaction;

        /**
         * List of reports.
         */
        private List<Report> reports = new ArrayList<Report>();

        /**
         * Constructor.
         *
         * @param cache    sets the {@link #cache}
         * @param interval sets the {@link cz.cesnet.shongo.controller.Cache.Transaction#interval}
         */
        public Context(AbstractReservationRequest reservationRequest, Cache cache, Interval interval)
        {
            this.reservationRequest = reservationRequest;
            this.cache = cache;
            this.cacheTransaction = new Cache.Transaction(interval);
        }

        /**
         * Constructor.
         *
         * @param cache    sets the {@link #cache}
         * @param interval sets the {@link cz.cesnet.shongo.controller.Cache.Transaction#interval}
         */
        public Context(Cache cache, Interval interval)
        {
            this(new ReservationRequest(Authorization.ROOT_USER_ID), cache, interval);
        }

        /**
         * @return {@link #reservationRequest}
         */
        public AbstractReservationRequest getReservationRequest()
        {
            return reservationRequest;
        }

        /**
         * @return {@link #reservationRequest#getUserId()}
         */
        public String getUserId()
        {
            return reservationRequest.getUserId();
        }

        /**
         * @return {@link Cache.Transaction#interval}
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
        public Cache.Transaction getCacheTransaction()
        {
            return cacheTransaction;
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
        protected void addReport(Report report)
        {
            reports.add(report);
        }
    }
}
