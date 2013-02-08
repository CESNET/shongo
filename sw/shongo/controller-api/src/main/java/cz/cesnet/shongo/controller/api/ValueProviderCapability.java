package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

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
    public static final String VALUE_PROVIDER = "valueProvider";

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
     * @return this {@link ValueProviderCapability} with {@link #VALUE_PROVIDER} which has
     *         {@link ValueProvider.Pattern#ALLOW_ANY_REQUESTED_VALUE} set to true
     */
    public ValueProviderCapability withAllowedAnyRequestedValue()
    {
        ((ValueProvider.Pattern) getValueProvider()).setAllowAnyRequestedValue(true);
        return this;
    }

    /**
     * @return {@link #VALUE_PROVIDER}
     */
    @Required
    public ValueProvider getValueProvider()
    {
        return getPropertyStorage().getValue(VALUE_PROVIDER);
    }

    /**
     * @param valueProvider sets the {@link #VALUE_PROVIDER}
     */
    public void setValueProvider(ValueProvider valueProvider)
    {
        getPropertyStorage().setValue(VALUE_PROVIDER, valueProvider);
    }
}
