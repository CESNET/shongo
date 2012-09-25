package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

/**
 * Capability tells that the resource can allocated can allocate aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasProviderCapability extends Capability
{
    /**
     * Technology of aliases.
     */
    public static final String TECHNOLOGY = "technology";

    /**
     * Type of aliases.
     */
    public static final String TYPE = "type";

    /**
     * Pattern for aliases.
     * <p/>
     * Examples:
     * 1) "95[ddd]"     will generate 95001, 95002, 95003, ...
     * 2) "95[dd]2[dd]" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
     */
    public static final String PATTERN = "pattern";

    /**
     * Specifies whether alias provider is restricted only to the owner resource or all resources can use the provider
     * for alias allocation.
     */
    public static final String RESTRICTED_TO_OWNER_RESOURCE = "restrictedToOwnerResource";

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
     * @return {@link #TYPE}
     */
    @Required
    public AliasType getType()
    {
        return getPropertyStorage().getValue(TYPE);
    }

    /**
     * @param type sets the {@link #TYPE}
     */
    public void setType(AliasType type)
    {
        getPropertyStorage().setValue(TYPE, type);
    }

    /**
     * @return {@link #PATTERN}
     */
    @Required
    public String getPattern()
    {
        return getPropertyStorage().getValue(PATTERN);
    }

    /**
     * @param pattern sets the {@link #PATTERN}
     */
    public void setPattern(String pattern)
    {
        getPropertyStorage().setValue(PATTERN, pattern);
    }

    /**
     * @return {@link #RESTRICTED_TO_OWNER_RESOURCE}
     */
    public Boolean getRestrictedToOwnerResource()
    {
        Object restrictedToOwnerResource = getPropertyStorage().getValue(RESTRICTED_TO_OWNER_RESOURCE);
        if (restrictedToOwnerResource == null) {
            return false;
        }
        return (Boolean) restrictedToOwnerResource;
    }

    /**
     * @param restrictedToOwnerResource sets the {@link #RESTRICTED_TO_OWNER_RESOURCE}
     */
    public void setRestrictedToOwnerResource(Boolean restrictedToOwnerResource)
    {
        getPropertyStorage().setValue(RESTRICTED_TO_OWNER_RESOURCE, restrictedToOwnerResource);
    }
}
