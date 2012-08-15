package cz.cesnet.shongo.controller;

import org.joda.time.DateMidnight;
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
    private long period = 10000;

    /**
     * @see Preprocessor
     */
    private Preprocessor preprocessor;

    /**
     * @see Scheduler
     */
    private Scheduler scheduler;

    /**
     * {@link EntityManagerFactory} for {@link Preprocessor} and {@link Scheduler}.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     *
     * @param preprocessor         sets the {@link #preprocessor}
     * @param scheduler            sets the {@link #scheduler}
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public WorkerThread(Preprocessor preprocessor, Scheduler scheduler, EntityManagerFactory entityManagerFactory)
    {
        setName("worker");
        if (preprocessor == null || scheduler == null) {
            throw new IllegalArgumentException("Preprocessor, Scheduler and EntityManagerFactory must be not-empty!");
        }
        this.preprocessor = preprocessor;
        this.scheduler = scheduler;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param period sets the {@link #period}
     */
    public void setPeriod(long period)
    {
        this.period = period;
    }

    @Override
    public void run()
    {
        logger.info("Worker started!");

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (!Thread.interrupted()) {
            work();
            try {
                Thread.sleep(period);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }

        logger.info("Worker stopped!");
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     */
    private void work()
    {

        Interval interval = new Interval(DateMidnight.now().minus(Period.days(1)),
                DateMidnight.now().plus(Period.days(31)));

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            entityManager.getTransaction().begin();
            preprocessor.run(interval, entityManager);
            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            entityManager.getTransaction().rollback();
            logger.error("Preprocessor failed: ", exception);
        }

        try {
            entityManager.getTransaction().begin();
            scheduler.run(interval, entityManager);
            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            entityManager.getTransaction().rollback();
            logger.error("Scheduler failed: ", exception);
        }

        entityManager.close();
    }
}
