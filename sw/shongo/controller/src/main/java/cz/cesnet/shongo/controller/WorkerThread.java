package cz.cesnet.shongo.controller;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Constructor.
     *
     * @param preprocessor sets the {@link #preprocessor}
     * @param scheduler    sets the {@link #scheduler}
     */
    public WorkerThread(Preprocessor preprocessor, Scheduler scheduler)
    {
        setName("worker");
        if (preprocessor == null || scheduler == null) {
            throw new IllegalArgumentException("Both preprocessor and scheduler must be not-empty!");
        }
        this.preprocessor = preprocessor;
        this.scheduler = scheduler;
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
        Interval interval = new Interval(DateTime.now().minus(Period.days(1)), DateTime.now().plus(Period.months(1)));
        try {
            preprocessor.run(interval);
        }
        catch (Exception exception) {
            logger.error("Preprocessor failed: ", exception);
        }
        try {
            scheduler.run(interval);
        }
        catch (Exception exception) {
            logger.error("Scheduler failed: ", exception);
        }
    }
}
