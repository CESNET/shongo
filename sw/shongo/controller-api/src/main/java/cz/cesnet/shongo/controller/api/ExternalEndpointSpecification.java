package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.Set;

/**
 * {@link Specification} for single external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointSpecification extends ParticipantSpecification
{
    /**
     * Set of technologies of the external endpoint.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Number of same resources.
     */
    public static final String ALIAS = "alias";

    /**
     * Constructor.
     */
    public ExternalEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     */
    public ExternalEndpointSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     * @param alias      sets the {@link #ALIAS}
     */
    public ExternalEndpointSpecification(Technology technology, Alias alias)
    {
        addTechnology(technology);
        setAlias(alias);
    }

    /**
     * Constructor.
     *
     * @param technologies to be added to the {@link #TECHNOLOGIES}
     * @param alias        sets the {@link #ALIAS}
     */
    public ExternalEndpointSpecification(Technology[] technologies, Alias alias)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
        setAlias(alias);
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
     * @return {@link #ALIAS}
     */
    public Alias getAlias()
    {
        return getPropertyStorage().getValue(ALIAS);
    }

    /**
     * @param alias sets the {@link #ALIAS}
     */
    public void setAlias(Alias alias)
    {
        getPropertyStorage().setValue(ALIAS, alias);
    }
}
