package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.request.Specification;

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
     * @param schedulerContext
     */
    public void checkAvailability(SchedulerContext schedulerContext) throws SchedulerException;
}
