package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.calendar.CalendarManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Thread which periodically runs {@link Preprocessor} and {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class WorkerThread extends Thread
{
    private static Logger logger = LoggerFactory.getLogger(WorkerThread.class);

    /**
     * Period in which the worker works.
     */
    private Duration period;

    /**
     * Length of working interval which stars at "now()".
     */
    private Period lookahead;

    /**
     * @see Preprocessor
     */
    private Preprocessor preprocessor;

    /**
     * @see Scheduler
     */
    private Scheduler scheduler;

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    private CalendarManager calendarManager;

    /**
     * {@link EntityManagerFactory} for {@link Preprocessor} and {@link Scheduler}.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * {@link DateTime} when the last cache clearing was performed.
     */
    private DateTime clearCacheDateTime;

    /**
     * Constructor.
     *
     * @param preprocessor         sets the {@link #preprocessor}
     * @param scheduler            sets the {@link #scheduler}
     * @param notificationManager  sets the {@link #notificationManager}
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public WorkerThread(Preprocessor preprocessor, Scheduler scheduler, NotificationManager notificationManager, CalendarManager calendarManager,
            EntityManagerFactory entityManagerFactory)
    {
        setName("worker");
        if (preprocessor == null || scheduler == null) {
            throw new IllegalArgumentException("Preprocessor, Scheduler and EntityManagerFactory must be not-empty!");
        }
        this.preprocessor = preprocessor;
        this.scheduler = scheduler;
        this.notificationManager = notificationManager;
        this.calendarManager = calendarManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param period sets the {@link #period}
     */
    public void setPeriod(Duration period)
    {
        this.period = period;
    }

    /**
     * @param lookahead sets the {@link #lookahead}
     */
    public void setLookahead(Period lookahead)
    {
        this.lookahead = lookahead;
    }

    @Override
    public void run()
    {
        logger.debug("Worker started!");

        if (period == null) {
            throw new IllegalStateException("Worker must have period set!");
        }
        if (lookahead == null) {
            throw new IllegalStateException("Worker must have lookahead length set!");
        }

        try {
            Thread.sleep(period.getMillis());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (!Thread.interrupted()) {
            work();
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (notificationManager.hasNotifications()) {
            logger.info("Executing remaining notifications...");
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                notificationManager.executeNotifications(entityManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.WORKER, exception);
            }
            finally {
                entityManager.close();
            }
        }

        logger.debug("Worker stopped!");
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     */
    private void work()
    {
        // Globally synchronized (see ThreadLock documentation)
        //logger.debug("Worker waiting for lock...........................");
        synchronized (ThreadLock.class) {
            //logger.debug("Worker lock acquired...   [[[[[")

            // We want to pre-process and schedule only reservation requests in specific interval
            Interval interval = new Interval(Temporal.nowRoundedToSeconds(), lookahead);

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            EntityManager bypassEntityManager = entityManagerFactory.createEntityManager();
            Reporter reporter = Reporter.getInstance();
            try {
                // Run preprocessor, scheduler and notifications
                preprocessor.run(interval, entityManager);
                scheduler.run(interval, entityManager, bypassEntityManager);
                notificationManager.executeNotifications(entityManager);
                calendarManager.sendCalendarNotifications(entityManager);

                // Clear reporter cache once per hour
                DateTime clearCacheDateTime = Temporal.nowRoundedToHours();
                if (!clearCacheDateTime.equals(this.clearCacheDateTime)) {
                    reporter.clearCache(interval.getStart());
                    this.clearCacheDateTime = clearCacheDateTime;
                }
            }
            catch (Exception exception) {
                reporter.reportInternalError(Reporter.WORKER, exception);
            }
            finally {
                entityManager.close();
                bypassEntityManager.close();
            }

            //logger.debug("Worker releasing lock...  ]]]]]");
        }
        //logger.debug("Worker lock released...");
    }
}
