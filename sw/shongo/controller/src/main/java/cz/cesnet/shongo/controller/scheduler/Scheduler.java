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
     * Constructor.
     */
    public Scheduler()
    {
    }

    /**
     * Constructor of scheduler.
     * @param entityManager sets the {@link #entityManager}
     */
    public Scheduler(EntityManager entityManager)
    {
        setEntityManager(entityManager);
        init();
    }

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * Initialize reservation database.
     */
    public void init()
    {
        if (entityManager == null) {
            throw new IllegalStateException("Scheduler doesn't have the entity manager set!");
        }

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
