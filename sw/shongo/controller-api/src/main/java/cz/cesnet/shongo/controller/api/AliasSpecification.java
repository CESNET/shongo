package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * Represents a {@link Specification} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasSpecification extends Specification
{
    /**
     * Technology of the resource.
     */
    public static final String TECHNOLOGY = "technology";

    /**
     * Alias type.
     */
    public static final String ALIAS_TYPE = "aliasType";

    /**
     * {@link Resource} with {@link AliasProviderCapability}.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #TECHNOLOGY}
     */
    public AliasSpecification(Technology technology)
    {
        setTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #TECHNOLOGY}
     * @param aliasType sets the {@link #ALIAS_TYPE}
     */
    public AliasSpecification(Technology technology, AliasType aliasType)
    {
        setTechnology(technology);
        setAliasType(aliasType);
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
     * @return {@link #ALIAS_TYPE}
     */
    public AliasType getAliasType()
    {
        return getPropertyStorage().getValue(ALIAS_TYPE);
    }

    /**
     * @param aliasType sets the {@link #ALIAS_TYPE}
     */
    public void setAliasType(AliasType aliasType)
    {
        getPropertyStorage().setValue(ALIAS_TYPE, aliasType);
    }

    /**
     * @return {@link #RESOURCE_IDENTIFIER}
     */
    public String getResourceIdentifier()
    {
        return getPropertyStorage().getValue(RESOURCE_IDENTIFIER);
    }

    /**
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        getPropertyStorage().setValue(RESOURCE_IDENTIFIER, resourceIdentifier);
    }
}
