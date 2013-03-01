package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;

/**
 * Capability tells that the resource can allocated can allocate aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasProviderCapability extends Capability
{
    /**
     * Identifier of resource with {@link ValueProviderCapability} or instance of the {@link ValueProvider}.
     * The value provider will be used for allocation of alias values.
     */
    public static final String VALUE_PROVIDER = "valueProvider";

    /**
     * Type of aliases.
     */
    public static final String ALIASES = "aliases";

    /**
     * Specifies the maximum future for which the {@link AliasProviderCapability} can be scheduled.
     */
    public static final String MAXIMUM_FUTURE = "maximumFuture";

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
     * @param pattern for construction of {@link ValueProvider}
     */
    public AliasProviderCapability(String pattern)
    {
        setValueProvider(new ValueProvider.Pattern(pattern));
    }

    /**
     * Constructor.
     *
     * @param pattern for construction of {@link ValueProvider}
     * @param type    to be added as {@link cz.cesnet.shongo.api.Alias} to {@link #ALIASES}
     */
    public AliasProviderCapability(String pattern, AliasType type)
    {
        this(pattern);

        addAlias(new Alias(type, "{value}"));
    }

    /**
     * Constructor.
     *
     * @param pattern for construction of {@link ValueProvider}
     * @param type    to be added as {@link Alias} to {@link #ALIASES}
     */
    public AliasProviderCapability(String pattern, AliasType type, String value)
    {
        this(pattern);

        addAlias(new Alias(type, value));
    }

    /**
     * @return this {@link AliasProviderCapability} with {@link #VALUE_PROVIDER} which has
     *         {@link ValueProvider.Pattern#ALLOW_ANY_REQUESTED_VALUE} set to true
     */
    public AliasProviderCapability withAllowedAnyRequestedValue()
    {
        ((ValueProvider.Pattern) getValueProvider()).setAllowAnyRequestedValue(true);
        return this;
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
     * @return {@link #VALUE_PROVIDER}
     */
    @Required
    @AllowedTypes({String.class, ValueProvider.class})
    public Object getValueProvider()
    {
        return getPropertyStorage().getValue(VALUE_PROVIDER);
    }

    /**
     * @param valueProvider sets the {@link #VALUE_PROVIDER}
     */
    public void setValueProvider(Object valueProvider)
    {
        getPropertyStorage().setValue(VALUE_PROVIDER, valueProvider);
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
     * @return {@link #RESTRICTED_TO_RESOURCE}
     */
    public Boolean getRestrictedToResource()
    {
        return getPropertyStorage().getValueAsBoolean(RESTRICTED_TO_RESOURCE, false);
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
        return getPropertyStorage().getValueAsBoolean(PERMANENT_ROOM, false);
    }

    /**
     * @param permanentRoom sets the {@link #PERMANENT_ROOM}
     */
    public void setPermanentRoom(Boolean permanentRoom)
    {
        getPropertyStorage().setValue(PERMANENT_ROOM, permanentRoom);
    }
}
