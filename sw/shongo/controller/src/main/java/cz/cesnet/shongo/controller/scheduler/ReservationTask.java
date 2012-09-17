package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Scheduler} task which receives {@link Specification} and results into {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReservationTask<R extends Reservation>
{
    /**
     * Context.
     */
    private Context context;

    /**
     * List of child {@link Reservation}.
     */
    private List<Reservation> childReservations = new ArrayList<Reservation>();

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
        context.addReport(report);
    }

    /**
     * @return {@link Context#interval}
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
        getCacheTransaction().addReservation(reservation);
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
        Reservation reservation = reservationTask.perform();
        addChildReservation(reservation);
        return reservation;
    }

    /**
     * Add child {@link Reservation} to the task allocated from a {@link ReservationTask} created from given
     * {@code reservationTaskProvider}.
     *
     * @param reservationTaskProvider used for creating {@link ReservationTask}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTaskProvider reservationTaskProvider,
            Class<R> reservationClass)
            throws ReportException
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
    public final R perform() throws ReportException
    {
        R reservation = createReservation();

        // Add reservation to the cache
        getCacheTransaction().addReservation(reservation);

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
    protected abstract R createReservation() throws ReportException;

    /**
     * Context for the {@link ReservationTask}.
     */
    public static class Context
    {
        /**
         * Interval for which the task is performed.
         */
        private Interval interval;

        /**
         * @see Cache
         */
        private Cache cache;

        /**
         * @see {@link Cache.Transaction}
         */
        private Cache.Transaction cacheTransaction = new Cache.Transaction();

        /**
         * List of reports.
         */
        private List<Report> reports = new ArrayList<Report>();

        /**
         * Constructor.
         *
         * @param interval sets the {@link #interval}
         * @param cache    sets the {@link #cache}
         */
        public Context(Interval interval, Cache cache)
        {
            this.interval = interval;
            this.cache = cache;
        }

        /**
         * @return {@link #interval}
         */
        public Interval getInterval()
        {
            return interval;
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
