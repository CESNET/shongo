package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * {@link Specification} for one or multiple external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointSetSpecification extends ParticipantSpecification
{
    /**
     * Technology of the resource.
     */
    public static final String TECHNOLOGY = "technology";

    /**
     * Number of same resources.
     */
    public static final String COUNT = "count";

    /**
     * Constructor.
     */
    public ExternalEndpointSetSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #TECHNOLOGY}
     * @param count      sets the {@link #COUNT}
     */
    public ExternalEndpointSetSpecification(Technology technology, int count)
    {
        setTechnology(technology);
        setCount(count);
    }

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

    /**
     * @return {@link #COUNT}
     */
    public Integer getCount()
    {
        return getPropertyStorage().getValue(COUNT);
    }

    /**
     * @param count sets the {@link #COUNT}
     */
    public void setCount(Integer count)
    {
        getPropertyStorage().setValue(COUNT, count);
    }
}
