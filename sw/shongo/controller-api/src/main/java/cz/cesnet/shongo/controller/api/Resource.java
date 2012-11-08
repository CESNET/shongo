package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Resource extends IdentifiedChangeableObject
{
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
     * Specifies whether resource can be scheduled by a scheduler.
     */
    public static final String ALLOCATABLE = "allocatable";

    /**
     * Specifies the maximum future for which the resource can be scheduled.
     */
    public static final String MAXIMUM_FUTURE = "maximumFuture";

    /**
     * Child resources identifiers.
     */
    private List<String> childResourceIdentifiers = new ArrayList<String>();

    /**
     * @return {@link #PARENT_RESOURCE_IDENTIFIER}
     */
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
     * @return {@link #ALLOCATABLE}
     */
    public Boolean getAllocatable()
    {
        Boolean allocatable = getPropertyStorage().getValue(ALLOCATABLE);
        return (allocatable != null ? allocatable : Boolean.FALSE);
    }

    /**
     * @param allocatable sets the {@link #ALLOCATABLE}
     */
    public void setAllocatable(Boolean allocatable)
    {
        getPropertyStorage().setValue(ALLOCATABLE, allocatable);
    }

    /**
     * @return {@link #MAXIMUM_FUTURE}
     */
    @AllowedTypes({DateTime.class, Period.class})
    public Object getMaximumFuture()
    {
        return getPropertyStorage().getValue(MAXIMUM_FUTURE);
    }

    /**
     * @param maximumFuture sets the {@link #MAXIMUM_FUTURE}
     */
    public void setMaximumFuture(Object maximumFuture)
    {
        getPropertyStorage().setValue(MAXIMUM_FUTURE, maximumFuture);
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
