package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import org.joda.time.*;
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
     * Length of working interval from now.
     */
    private Period intervalLength;

    /**
     * @see Preprocessor
     */
    private Preprocessor preprocessor;

    /**
     * @see Scheduler
     */
    private Scheduler scheduler;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * {@link EntityManagerFactory} for {@link Preprocessor} and {@link Scheduler}.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     *
     * @param preprocessor         sets the {@link #preprocessor}
     * @param scheduler            sets the {@link #scheduler}
     * @param authorization
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public WorkerThread(Preprocessor preprocessor, Scheduler scheduler, Authorization authorization,
            EntityManagerFactory entityManagerFactory)
    {
        setName("worker");
        if (preprocessor == null || scheduler == null) {
            throw new IllegalArgumentException("Preprocessor, Scheduler and EntityManagerFactory must be not-empty!");
        }
        this.preprocessor = preprocessor;
        this.scheduler = scheduler;
        this.authorization = authorization;
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
     * @param intervalLength sets the {@link #intervalLength}
     */
    public void setIntervalLength(Period intervalLength)
    {
        this.intervalLength = intervalLength;
    }

    @Override
    public void run()
    {
        logger.debug("Worker started!");

        if (period == null) {
            throw new IllegalStateException("Worker must have period set!");
        }
        if (intervalLength == null) {
            throw new IllegalStateException("Worker must have interval length set!");
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

            ReadableDateTime dateTimeNow = DateMidnight.now();
            Interval interval = new Interval(dateTimeNow, DateMidnight.now().plus(intervalLength));

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
            try {
                // Run preprocessor and scheduler
                preprocessor.run(interval, entityManager);
                scheduler.run(interval, entityManager);

                // Propagate ACL records to authorization server
                authorizationManager.propagate(authorization);
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.WORKER, exception);
            }
            finally {
                entityManager.close();
            }

            //logger.debug("Worker releasing lock...  ]]]]]");
        }
        //logger.debug("Worker lock released...");
    }
}
