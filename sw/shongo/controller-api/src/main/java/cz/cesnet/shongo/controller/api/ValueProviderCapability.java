package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Capability tells that the resource can allocate unique values from given patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueProviderCapability extends Capability
{
    /**
     * Instance of the {@link ValueProvider}.
     */
    private ValueProvider valueProvider;

    /**
     * Constructor.
     */
    public ValueProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern for construction of {@link ValueProvider}
     */
    public ValueProviderCapability(String pattern)
    {
        setValueProvider(new ValueProvider.Pattern(pattern));
    }

    /**
     * @return this {@link ValueProviderCapability} with {@link #valueProvider} which has
     *         {@link ValueProvider.Pattern#ALLOW_ANY_REQUESTED_VALUE} set to true
     */
    public ValueProviderCapability withAllowedAnyRequestedValue()
    {
        ((ValueProvider.Pattern) getValueProvider()).setAllowAnyRequestedValue(true);
        return this;
    }

    /**
     * @return {@link #valueProvider}
     */
    public ValueProvider getValueProvider()
    {
        return valueProvider;
    }

    /**
     * @param valueProvider sets the {@link #valueProvider}
     */
    public void setValueProvider(ValueProvider valueProvider)
    {
        this.valueProvider = valueProvider;
    }

    public static final String VALUE_PROVIDER = "valueProvider";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(VALUE_PROVIDER, valueProvider);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        valueProvider = dataMap.getComplexTypeRequired(VALUE_PROVIDER, ValueProvider.class);
    }
}
