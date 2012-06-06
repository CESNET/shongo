package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.common.Person;

import java.util.List;

/**
 * Represents an allocated resource in an {@link cz.cesnet.shongo.controller.scheduler.AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedDeviceResource extends AllocatedResource
{
    /**
     * List of persons which use the resource in specified date/time slot.
     */
    private List<Person> persons;
}
