package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * {@link Specification} for single external endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointSpecification extends ParticipantSpecification
{
    /**
     * Technology of the resource.
     */
    public static final String TECHNOLOGY = "technology";

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
     * @param technology sets the {@link #TECHNOLOGY}
     * @param alias      sets the {@link #ALIAS}
     */
    public ExternalEndpointSpecification(Technology technology, Alias alias)
    {
        setTechnology(technology);
        setAlias(alias);
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
