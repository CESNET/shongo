package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
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
     * Specifies whether the {@link AliasProviderCapability} can allocate {@link Alias}es only for
     * the owner resource or for all {@link Resource}s in the resource database.
     */
    public static final String RESTRICTED_TO_RESOURCE = "restrictedToResource";

    /**
     * Specifies whether the {@link Alias}es allocated by the {@link AliasProviderCapability} should represent
     * permanent rooms (should get allocated {@link Executable.ResourceRoom}).
     */
    public static final String PERMANENT_ROOM = "permanentRoom";

    /**
     * Constructor.
     */
    public AliasProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern to be added to the {@link #PATTERNS}
     * @param type    to be added as {@link cz.cesnet.shongo.api.Alias} to {@link #ALIASES}
     */
    public AliasProviderCapability(String pattern, AliasType type)
    {
        addAlias(new Alias(type, "{value}"));
        addPattern(pattern);
    }

    /**
     * Constructor.
     *
     * @param type                      to be added as {@link Alias} to {@link #ALIASES}
     * @param pattern                   to be added to the {@link #PATTERNS}
     */
    public AliasProviderCapability(String pattern, AliasType type, String value)
    {
        addAlias(new Alias(type, value));
        addPattern(pattern);
    }

    /**
     * @return this {@link AliasProviderCapability} with {@link #RESTRICTED_TO_RESOURCE} set to {@code true}
     */
    public AliasProviderCapability withRestrictedToResource()
    {
        setRestrictedToResource(true);
        return this;
    }

    /**
     * @return this {@link AliasProviderCapability} with {@link #PERMANENT_ROOM} set to {@code true}
     */
    public AliasProviderCapability withPermanentRoom()
    {
        setPermanentRoom(true);
        return this;
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
     * @return {@link #RESTRICTED_TO_RESOURCE}
     */
    public Boolean getRestrictedToResource()
    {
        return getPropertyStorage().getValueAsBoolean(RESTRICTED_TO_RESOURCE);
    }

    /**
     * @param restrictedToResource sets the {@link #RESTRICTED_TO_RESOURCE}
     */
    public void setRestrictedToResource(Boolean restrictedToResource)
    {
        getPropertyStorage().setValue(RESTRICTED_TO_RESOURCE, restrictedToResource);
    }

    /**
     * @return {@link #PERMANENT_ROOM}
     */
    public Boolean getPermanentRoom()
    {
        return getPropertyStorage().getValueAsBoolean(PERMANENT_ROOM);
    }

    /**
     * @param permanentRoom sets the {@link #PERMANENT_ROOM}
     */
    public void setPermanentRoom(Boolean permanentRoom)
    {
        getPropertyStorage().setValue(PERMANENT_ROOM, permanentRoom);
    }
}
