package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.*;

/**
 * Capability tells that the resource can allocate unique values base on the patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ValueProviderCapability extends Capability
{
    /**
     * {@link ValueProvider} which will be used for generating values.
     */
    private ValueProvider valueProvider = new ValueProvider(this);

    /**
     * Constructor.
     */
    public ValueProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern to be added to the {@link #valueProvider#patterns}
     */
    public ValueProviderCapability(String pattern)
    {
        valueProvider.addPattern(pattern);
    }

    /**
     * @return {@link #valueProvider}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public ValueProvider getValueProvider()
    {
        return valueProvider;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.ValueProviderCapability apiValueProvider =
                (cz.cesnet.shongo.controller.api.ValueProviderCapability) api;
        for (String pattern : valueProvider.getPatterns()) {
            apiValueProvider.addPattern(pattern);
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ValueProviderCapability apiValueProvider =
                (cz.cesnet.shongo.controller.api.ValueProviderCapability) api;

        // Create patterns
        for (String pattern : apiValueProvider.getPatterns()) {
            if (api.isPropertyItemMarkedAsNew(cz.cesnet.shongo.controller.api.ValueProviderCapability.PATTERNS,
                    pattern)) {
                valueProvider.addPattern(pattern);
            }
        }
        // Delete patterns
        Set<String> patternsToDelete =
                api.getPropertyItemsMarkedAsDeleted(cz.cesnet.shongo.controller.api.ValueProviderCapability.PATTERNS);
        for (String pattern : patternsToDelete) {
            valueProvider.removePattern(pattern);
        }

        super.fromApi(api, entityManager);
    }
}
