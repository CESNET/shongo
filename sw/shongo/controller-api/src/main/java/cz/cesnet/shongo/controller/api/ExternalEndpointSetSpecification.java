package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.Set;

/**
 * {@link Specification} for one or multiple external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointSetSpecification extends ParticipantSpecification
{
    /**
     * Set of technologies of the external endpoint.
     */
    public static final String TECHNOLOGIES = "technologies";

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
     * @param technology to be added to the {@link #TECHNOLOGIES}
     * @param count      sets the {@link #COUNT}
     */
    public ExternalEndpointSetSpecification(Technology technology, int count)
    {
        addTechnology(technology);
        setCount(count);
    }

    /**
     * Constructor.
     *
     * @param technologies to be added to the {@link #TECHNOLOGIES}
     * @param count        sets the {@link #COUNT}
     */
    public ExternalEndpointSetSpecification(Technology[] technologies, int count)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
        setCount(count);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    @Required
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES, Set.class);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology, Set.class);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
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
