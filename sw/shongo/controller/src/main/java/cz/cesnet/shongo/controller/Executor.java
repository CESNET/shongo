package cz.cesnet.shongo.controller;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends Component implements Component.WithThread, Runnable
{
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * Period in which the executor works.
     */
    private Duration period = Duration.parse("PT1S");

    @Override
    public Thread getThread()
    {
        Thread thread = new Thread(this);
        thread.setName("executor");
        return thread;
    }

    @Override
    public void run()
    {
        logger.info("Executor started!");

        while (!Thread.interrupted()) {
            execute();
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }

        logger.info("Executor stopped!");
    }

    private void execute()
    {
        // TODO:
    }
}
