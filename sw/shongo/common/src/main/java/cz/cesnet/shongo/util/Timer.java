package cz.cesnet.shongo.util;

/**
 * Object for measuring duration.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Timer
{
    /**
     * Starting time
     */
    private long timeMillis;

    /**
     * Constructor which stars the timer.
     */
    public Timer()
    {
        start();
    }

    /**
     * Starts the timer (keeps the current time)
     */
    public void start()
    {
        timeMillis = System.currentTimeMillis();
    }

    /**
     * @return duration passed from {@link #start()}
     */
    public long stop()
    {
        return System.currentTimeMillis() - timeMillis;
    }

    /**
     * Stop the timer and print the duration from starting moment to {@link System#out}.
     */
    public void stopAndPrint()
    {
        long duration = stop();
        System.out.printf("Duration: %d ms\n", duration);
    }
}
