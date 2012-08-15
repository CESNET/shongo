package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * Special type of requested resource which searches for available resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LookupResourceSpecification extends ResourceSpecification
{
    /**
     * Technology of the resource.
     */
    public static final String TECHNOLOGY = "technology";

    /**
     * @return {@link #TECHNOLOGY}
     */
    @Required
    public Technology getTechnology()
    {
        return getPropertyStorage().getValue(TECHNOLOGY);
    }

    /**
     * @param technology sets the {@link #TECHNOLOGY}
     */
    public void setTechnology(Technology technology)
    {
        getPropertyStorage().setValue(TECHNOLOGY, technology);
    }
}
