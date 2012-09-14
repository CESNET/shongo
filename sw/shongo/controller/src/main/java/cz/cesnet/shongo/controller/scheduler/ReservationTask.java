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
public abstract class ReservationTask<S extends Specification, R extends Reservation>
{
    /**
     * @see {@link Specification}
     */
    private S specification;

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
    public ReservationTask(S specification, Context context)
    {
        this.specification = specification;
        this.context = context;
    }

    /**
     * @return {@link #specification}
     */
    protected S getSpecification()
    {
        return specification;
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
    protected void addChildReservation(Reservation reservation, Specification specification)
    {
        childReservations.add(reservation);
        getCacheTransaction().addReservation(reservation);
    }

    /**
     * Add child {@link Specification} to the task.
     *
     * @param specification child {@link Specification} to be added
     */
    public Reservation addChildSpecification(Specification specification)
            throws ReportException
    {
        ReservationTask reservationTask = specification.createReservationTask(getContext());
        Reservation reservation = reservationTask.perform();
        addChildReservation(reservation, specification);
        return reservation;
    }

    /**
     * Add child {@link Specification} to the task.
     *
     * @param specification child {@link Specification} to be added
     */
    public <R extends Reservation> R addChildSpecification(Specification specification, Class<R> reservationClass)
            throws ReportException
    {
        Reservation reservation = addChildSpecification(specification);
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
        R reservation = createReservation(getSpecification());

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
    protected abstract R createReservation(S specification) throws ReportException;

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
