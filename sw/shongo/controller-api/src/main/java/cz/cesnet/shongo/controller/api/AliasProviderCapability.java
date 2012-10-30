package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.List;

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
    public static final String PATTERNS = "patterns";

    /**
     * Specifies whether alias provider is restricted only to the owner resource or all resources can use the provider
     * for alias allocation.
     */
    public static final String RESTRICTED_TO_OWNER_RESOURCE = "restrictedToOwnerResource";

    /**
     * Constructor.
     */
    public AliasProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #TECHNOLOGY}
     * @param type       sets the {@link #TYPE}
     * @param pattern    to be added to the {@link #PATTERNS}
     */
    public AliasProviderCapability(Technology technology, AliasType type, String pattern)
    {
        setTechnology(technology);
        setType(type);
        addPattern(pattern);
    }

    /**
     * Constructor.
     *
     * @param technology                sets the {@link #TECHNOLOGY}
     * @param type                      sets the {@link #TYPE}
     * @param pattern                   to be added to the {@link #PATTERNS}
     * @param restrictedToOwnerResource sets the {@link #RESTRICTED_TO_OWNER_RESOURCE}
     */
    public AliasProviderCapability(Technology technology, AliasType type, String pattern,
            boolean restrictedToOwnerResource)
    {
        setTechnology(technology);
        setType(type);
        addPattern(pattern);
        setRestrictedToOwnerResource(restrictedToOwnerResource);
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
     * @return {@link #PATTERNS}
     */
    @Required
    public List<String> getPatterns()
    {
        return getPropertyStorage().getCollection(PATTERNS, List.class);
    }

    /**
     * @param patterns sets the {@link #PATTERNS}
     */
    public void setPatterns(List<String> patterns)
    {
        getPropertyStorage().setValue(PATTERNS, patterns);
    }

    /**
     * @param pattern to be added to the {@link #PATTERNS}
     */
    public void addPattern(String pattern)
    {
        getPropertyStorage().addCollectionItem(PATTERNS, pattern, List.class);
    }

    /**
     * @param pattern to be removed from the {@link #PATTERNS}
     */
    public void removePattern(String pattern)
    {
        getPropertyStorage().removeCollectionItem(PATTERNS, pattern);
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
