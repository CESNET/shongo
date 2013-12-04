package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.LinkedList;
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
    private Object valueProvider;

    /**
     * Type of aliases.
     */
    private List<Alias> aliases = new LinkedList<Alias>();

    /**
     * Specifies the maximum future for which the {@link AliasProviderCapability} can be scheduled.
     */
    private Object maximumFuture;

    /**
     * Specifies whether the {@link AliasProviderCapability} can allocate {@link Alias}es only for
     * the owner resource or for all {@link Resource}s in the resource database.
     */
    private Boolean restrictedToResource;

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
     * @param type    to be added as {@link cz.cesnet.shongo.api.Alias} to {@link #aliases}
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
     * @param type    to be added as {@link Alias} to {@link #aliases}
     */
    public AliasProviderCapability(String pattern, AliasType type, String value)
    {
        this(pattern);

        addAlias(new Alias(type, value));
    }

    /**
     * @return this {@link AliasProviderCapability} with {@link #valueProvider} which has
     *         {@link ValueProvider.Pattern#ALLOW_ANY_REQUESTED_VALUE} set to true
     */
    public AliasProviderCapability withAllowedAnyRequestedValue()
    {
        ((ValueProvider.Pattern) getValueProvider()).setAllowAnyRequestedValue(true);
        return this;
    }

    /**
     * @return this {@link AliasProviderCapability} with {@link #restrictedToResource} set to {@code true}
     */
    public AliasProviderCapability withRestrictedToResource()
    {
        setRestrictedToResource(true);
        return this;
    }

    /**
     * @return {@link #valueProvider}
     */
    public Object getValueProvider()
    {
        return valueProvider;
    }

    /**
     * @param valueProvider sets the {@link #valueProvider}
     */
    public void setValueProvider(Object valueProvider)
    {
        if (valueProvider instanceof ValueProvider || valueProvider instanceof String) {
            this.valueProvider = valueProvider;
        }
        else {
            throw new TodoImplementException(valueProvider.getClass());
        }
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @param alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        aliases.remove(alias);
    }

    /**
     * @return {@link #maximumFuture}
     */
    public Object getMaximumFuture()
    {
        return maximumFuture;
    }

    /**
     * @param maximumFuture sets the {@link #maximumFuture}
     */
    public void setMaximumFuture(Object maximumFuture)
    {
        if (maximumFuture instanceof Period || maximumFuture instanceof DateTime || maximumFuture instanceof String) {
            this.maximumFuture = maximumFuture;
        }
        else {
            throw new TodoImplementException(maximumFuture.getClass());
        }
    }

    /**
     * @return {@link #restrictedToResource}
     */
    public Boolean getRestrictedToResource()
    {
        return restrictedToResource;
    }

    /**
     * @param restrictedToResource sets the {@link #restrictedToResource}
     */
    public void setRestrictedToResource(Boolean restrictedToResource)
    {
        this.restrictedToResource = restrictedToResource;
    }

    public static final String VALUE_PROVIDER = "valueProvider";
    public static final String ALIASES = "aliases";
    public static final String MAXIMUM_FUTURE = "maximumFuture";
    public static final String RESTRICTED_TO_RESOURCE = "restrictedToResource";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();

        dataMap.set(ALIASES, aliases);
        dataMap.set(RESTRICTED_TO_RESOURCE, restrictedToResource);

        if (valueProvider instanceof String) {
            dataMap.set(VALUE_PROVIDER, (String) valueProvider);
        }
        else if (valueProvider instanceof ValueProvider) {
            dataMap.set(VALUE_PROVIDER, (ValueProvider) valueProvider);
        }

        if (maximumFuture instanceof DateTime) {
            dataMap.set(MAXIMUM_FUTURE, (DateTime) maximumFuture);
        }
        else if (maximumFuture instanceof Period) {
            dataMap.set(MAXIMUM_FUTURE, (Period) maximumFuture);
        }
        else if (maximumFuture instanceof String) {
            dataMap.set(MAXIMUM_FUTURE, (String) maximumFuture);
        }

        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        valueProvider = dataMap.getVariantRequired(VALUE_PROVIDER, ValueProvider.class, String.class);
        aliases = dataMap.getListRequired(ALIASES, Alias.class);
        maximumFuture = dataMap.getVariant(MAXIMUM_FUTURE, DateTime.class, Period.class);
        restrictedToResource = dataMap.getBool(RESTRICTED_TO_RESOURCE);
    }
}
