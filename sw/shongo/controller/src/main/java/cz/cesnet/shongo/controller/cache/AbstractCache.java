package cz.cesnet.shongo.controller.cache;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;

/**
 * Represents an abstract cache.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractCache
{
    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Represents a reference data time which is a rounded now().
     */
    private DateTime referenceDateTime;

    /**
     * @return {@link #workingInterval}
     */
    protected Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @return {@link #referenceDateTime}
     */
    protected DateTime getReferenceDateTime()
    {
        return referenceDateTime;
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     * @param entityManager   used for reloading allocations of resources for the new interval
     */
    public void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        if (!workingInterval.equals(this.workingInterval)) {
            this.workingInterval = workingInterval;
            this.referenceDateTime = workingInterval.getStart();
            workingIntervalChanged(entityManager);
        }
    }

    /**
     * Method called when {@link #workingInterval} is changed.
     *
     * @param entityManager
     */
    protected abstract void workingIntervalChanged(EntityManager entityManager);
}
