package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.List;
import java.util.Set;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Resource extends ComplexType
{
    /**
     * Name of the resource.
     */
    public static final String NAME = "name";

    /**
     * Set of technologies which the resource supports.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * List of capabilities which the resource has.
     */
    public static final String CAPABILITIES = "capabilities";

    /**
     * @return {@link #NAME}
     */
    public String getName()
    {
        return getPropertyStorage().getValue(NAME);
    }

    /**
     * @param name sets the {@link #NAME}
     */
    public void setName(String name)
    {
        getPropertyStorage().setValue(NAME, name);
    }

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

    /**
     * @return {@link #CAPABILITIES}
     */
    @Required
    public List<Capability> getCapabilities()
    {
        return getPropertyStorage().getCollection(CAPABILITIES);
    }

    /**
     * @param capabilities sets the {@link #CAPABILITIES}
     */
    private void setCapabilities(List<Capability> capabilities)
    {
        getPropertyStorage().setCollection(CAPABILITIES, capabilities);
    }

    /**
     * @param capability capability to be added to the {@link #CAPABILITIES}
     */
    public void addCapability(Capability capability)
    {
        getPropertyStorage().addCollectionItem(CAPABILITIES, capability);
    }

    /**
     * @param capability capability to be removed from the {@link #CAPABILITIES}
     */
    public void removeCapability(Capability capability)
    {
        getPropertyStorage().removeCollectionItem(CAPABILITIES, capability);
    }
}
