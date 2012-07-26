package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Resource extends IdentifiedChangeableObject
{
    /**
     * String representing unmanaged mode.
     */
    public static final String UNMANAGED_MODE = "UNMANAGED";

    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * Parent resource identifier for the resource.
     */
    public static final String PARENT_RESOURCE_IDENTIFIER = "parentResourceIdentifier";

    /**
     * Name of the resource.
     */
    public static final String NAME = "name";

    /**
     * Description of the resource.
     */
    public static final String DESCRIPTION = "description";

    /**
     * List of capabilities which the resource has.
     */
    public static final String CAPABILITIES = "capabilities";

    /**
     * Set of technologies which the resource supports.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Specifies the mode of the resource.
     */
    public static final String MODE = "mode";

    /**
     * Specifies whether resource can be scheduled by a scheduler.
     */
    public static final String SCHEDULABLE = "schedulable";

    /**
     * Specifies the maximum future for which the resource can be scheduled.
     */
    public static final String MAX_FUTURE = "maxFuture";

    /**
     * Child resources identifiers.
     */
    private List<String> childResourceIdentifiers = new ArrayList<String>();

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #PARENT_RESOURCE_IDENTIFIER}
     */
    @Required
    public String getParentIdentifier()
    {
        return getPropertyStorage().getValue(PARENT_RESOURCE_IDENTIFIER);
    }

    /**
     * @param parentIdentifier sets the {@link #PARENT_RESOURCE_IDENTIFIER}
     */
    public void setParentIdentifier(String parentIdentifier)
    {
        getPropertyStorage().setValue(PARENT_RESOURCE_IDENTIFIER, parentIdentifier);
    }

    /**
     * @return {@link #NAME}
     */
    @Required
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
     * @return {@link #DESCRIPTION}
     */
    @Required
    public String getDescription()
    {
        return getPropertyStorage().getValue(DESCRIPTION);
    }

    /**
     * @param description sets the {@link #DESCRIPTION}
     */
    public void setDescription(String description)
    {
        getPropertyStorage().setValue(DESCRIPTION, description);
    }

    /**
     * @return {@link #CAPABILITIES}
     */
    public List<Capability> getCapabilities()
    {
        return getPropertyStorage().getCollection(CAPABILITIES, List.class);
    }

    /**
     * @param capabilities sets the {@link #CAPABILITIES}
     */
    public void setCapabilities(List<Capability> capabilities)
    {
        getPropertyStorage().setCollection(CAPABILITIES, capabilities);
    }

    /**
     * @param capability capability to be added to the {@link #CAPABILITIES}
     */
    public void addCapability(Capability capability)
    {
        getPropertyStorage().addCollectionItem(CAPABILITIES, capability, List.class);
    }

    /**
     * @param capability capability to be removed from the {@link #CAPABILITIES}
     */
    public void removeCapability(Capability capability)
    {
        getPropertyStorage().removeCollectionItem(CAPABILITIES, capability);
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
     * @return {@link #MODE}
     */
    @AllowedTypes({String.class, ManagedMode.class})
    public Object getMode()
    {
        return getPropertyStorage().getValue(MODE);
    }

    /**
     * @param mode sets the {@link #MODE}
     */
    public void setMode(Object mode)
    {
        getPropertyStorage().setValue(MODE, mode);
    }

    /**
     * @return {@link #SCHEDULABLE}
     */
    public Boolean getSchedulable()
    {
        return getPropertyStorage().getValue(SCHEDULABLE);
    }

    /**
     * @param schedulable sets the {@link #SCHEDULABLE}
     */
    public void setSchedulable(Boolean schedulable)
    {
        getPropertyStorage().setValue(SCHEDULABLE, schedulable);
    }

    /**
     * @return {@link #MAX_FUTURE}
     */
    @AllowedTypes({DateTime.class, Period.class})
    public Object getMaxFuture()
    {
        return getPropertyStorage().getValue(MAX_FUTURE);
    }

    /**
     * @param maxFuture sets the {@link #MAX_FUTURE}
     */
    public void setMaxFuture(Object maxFuture)
    {
        getPropertyStorage().setValue(MAX_FUTURE, maxFuture);
    }

    /**
     * @return {@link #childResourceIdentifiers}
     */
    @ReadOnly
    public List<String> getChildResourceIdentifiers()
    {
        return childResourceIdentifiers;
    }

    /**
     * @param childResourceIdentifier identifier to be added to the {@link #childResourceIdentifiers}
     */
    public void addChildResourceIdentifier(String childResourceIdentifier)
    {
        childResourceIdentifiers.add(childResourceIdentifier);
    }
}
