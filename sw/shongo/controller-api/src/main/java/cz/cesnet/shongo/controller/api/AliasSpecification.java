package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;

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
     * Alias value.
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
     * @param technology sets the {@link #TECHNOLOGY}
     */
    public AliasSpecification(Technology technology)
    {
        setTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param aliasType  sets the {@link #ALIAS_TYPE}
     */
    public AliasSpecification(AliasType aliasType)
    {
        setAliasType(aliasType);
    }

    /**
     * @return {@link #TECHNOLOGY}
     */
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
