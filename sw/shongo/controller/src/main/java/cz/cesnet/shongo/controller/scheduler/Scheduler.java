package cz.cesnet.shongo.controller.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * Scheduler of a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * Entity manager that is used for loading/saving reservation requests.
     */
    private EntityManager entityManager;

    /**
     * Constructor of scheduler.
     * @param entityManager sets the {@link #entityManager}
     */
    public Scheduler(EntityManager entityManager)
    {
        this.entityManager = entityManager;

        logger.debug("Starting scheduler...");
    }

    /**
     * Destroy scheduler.
     */
    public void destroy()
    {
        logger.debug("Closing scheduler...");
    }
}
