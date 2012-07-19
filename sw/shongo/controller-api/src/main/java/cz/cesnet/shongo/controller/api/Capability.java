package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.Set;

/**
 * Represents a capability of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Capability extends IdentifiedChangeableObject
{
    /**
     * Set of technologies for which the resource supports capability.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * @return {@link #TECHNOLOGIES}
     */
    @Required
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    private void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }
}
