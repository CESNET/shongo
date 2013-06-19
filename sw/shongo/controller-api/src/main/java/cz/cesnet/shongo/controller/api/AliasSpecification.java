package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;

import java.util.Collection;
import java.util.Set;

/**
 * Represents a {@link Specification} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasSpecification extends Specification
{
    /**
     * Restricts {@link AliasType} for allocation of {@link Alias}.
     */
    public static final String ALIAS_TYPES = "aliasTypes";

    /**
     * Restricts {@link Technology} for allocation of {@link Alias}.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Restricts {@link Alias#value}.
     */
    public static final String VALUE = "value";

    /**
     * {@link Resource} with {@link AliasProviderCapability} from which the {@link Alias} should be allocated.
     */
    public static final String RESOURCE_ID = "resourceId";

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #TECHNOLOGIES}
     */
    public AliasSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param aliasType to be added to the {@link #ALIAS_TYPES}
     */
    public AliasSpecification(AliasType aliasType)
    {
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param aliasTypes sets the {@link #ALIAS_TYPES}
     */
    public AliasSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAliasType(aliasType);
        }
    }

    /**
     * @param resourceId sets the {@link #RESOURCE_ID}
     * @return this {@link AliasSpecification} with {@link #RESOURCE_ID} set to {@code resourceId}
     */
    public AliasSpecification withResourceId(String resourceId)
    {
        setResourceId(resourceId);
        return this;
    }

    /**
     * @param value sets the {@link #VALUE}
     * @return this {@link AliasSpecification} with {@link #VALUE} set to {@code value}
     */
    public AliasSpecification withValue(String value)
    {
        setValue(value);
        return this;
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
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
     * @param technologies to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnologies(Collection<Technology> technologies)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @return {@link #ALIAS_TYPES}
     */
    public Set<AliasType> getAliasTypes()
    {
        return getPropertyStorage().getCollection(ALIAS_TYPES, Set.class);
    }

    /**
     * @param aliasTypes sets the {@link #ALIAS_TYPES}
     */
    public void setAliasTypes(Set<AliasType> aliasTypes)
    {
        getPropertyStorage().setCollection(ALIAS_TYPES, aliasTypes);
    }

    /**
     * @param aliasType to be added to the {@link #ALIAS_TYPES}
     */
    public void addAliasType(AliasType aliasType)
    {
        getPropertyStorage().addCollectionItem(ALIAS_TYPES, aliasType, Set.class);
    }

    /**
     * @param aliasType to be removed from the {@link #ALIAS_TYPES}
     */
    public void removeAliasType(AliasType aliasType)
    {
        getPropertyStorage().removeCollectionItem(ALIAS_TYPES, aliasType);
    }

    /**
     * @return {@link #VALUE}
     */
    public String getValue()
    {
        return getPropertyStorage().getValue(VALUE);
    }

    /**
     * @param value sets the {@link #VALUE}
     */
    public void setValue(String value)
    {
        getPropertyStorage().setValue(VALUE, value);
    }

    /**
     * @return {@link #RESOURCE_ID}
     */
    public String getResourceId()
    {
        return getPropertyStorage().getValue(RESOURCE_ID);
    }

    /**
     * @param resourceId sets the {@link #RESOURCE_ID}
     */
    public void setResourceId(String resourceId)
    {
        getPropertyStorage().setValue(RESOURCE_ID, resourceId);
    }
}
