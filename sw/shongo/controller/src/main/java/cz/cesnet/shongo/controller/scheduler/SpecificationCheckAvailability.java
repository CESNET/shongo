package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.Specification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;

/**
 * Interface which can be implemented by a {@link Specification} and it tells that the {@link Specification} can
 * be checked if it is available in specific date/time slot.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SpecificationCheckAvailability
{
    /**
     * Check availability of this {@link Specification} in given {@code slot}.
     *
     * @param slot to be checked
     * @param entityManager which can be used for checking
     */
    public void checkAvailability(Interval slot, EntityManager entityManager) throws ReportException;
}
