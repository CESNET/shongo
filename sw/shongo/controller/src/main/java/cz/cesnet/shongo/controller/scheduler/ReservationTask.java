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
public abstract class ReservationTask<T extends Specification>
{
    /**
     * @see {@link Specification}
     */
    private T specification;

    /**
     * Context.
     */
    private Context context;

    /**
     * Constructor.
     */
    public ReservationTask(T specification, Context context)
    {
        this.specification = specification;
        this.context = context;
    }

    /**
     * @return {@link #specification}
     */
    protected T getSpecification()
    {
        return specification;
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
     * Perform the {@link ReservationTask}.
     *
     * @param specification
     * @return created {@link Reservation}
     * @throws ReportException when the {@link ReservationTask} failed
     */
    public final Reservation perform(Specification specification) throws ReportException
    {
        return createReservation(specification);
    }

    /**
     * @return created {@link Reservation}
     * @throws ReportException when the {@link Reservation} cannot be created
     */
    protected abstract Reservation createReservation(Specification specification) throws ReportException;

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
         * @param cache sets the {@link #cache}
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
