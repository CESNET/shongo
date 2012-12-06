package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
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
     * Type of aliases.
     */
    public static final String ALIASES = "aliases";

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
     * @param type       to be added as {@link Alias} to {@link #ALIASES}
     * @param pattern    to be added to the {@link #PATTERNS}
     */
    public AliasProviderCapability(AliasType type, String pattern)
    {
        addAlias(new Alias(type, "{value}"));
        addPattern(pattern);
    }

    /**
     * Constructor.
     *
     * @param type       to be added as {@link Alias} to {@link #ALIASES}
     * @param pattern                   to be added to the {@link #PATTERNS}
     * @param restrictedToOwnerResource sets the {@link #RESTRICTED_TO_OWNER_RESOURCE}
     */
    public AliasProviderCapability(AliasType type, String pattern,
            boolean restrictedToOwnerResource)
    {
        addAlias(new Alias(type, "{value}"));
        addPattern(pattern);
        setRestrictedToOwnerResource(restrictedToOwnerResource);
    }

    /**
     * @return {@link #ALIASES}
     */
    @Required
    public List<Alias> getAliases()
    {
        return getPropertyStorage().getCollection(ALIASES, List.class);
    }

    /**
     * @param aliases sets the {@link #ALIASES}
     */
    public void setAliases(List<Alias> aliases)
    {
        getPropertyStorage().setValue(ALIASES, aliases);
    }

    /**
     * @param alias to be added to the {@link #ALIASES}
     */
    public void addAlias(Alias alias)
    {
        getPropertyStorage().addCollectionItem(ALIASES, alias, List.class);
    }

    /**
     * @param alias to be removed from the {@link #ALIASES}
     */
    public void removeAlias(Alias alias)
    {
        getPropertyStorage().removeCollectionItem(ALIASES, alias);
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
