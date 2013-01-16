package cz.cesnet.shongo.controller.resource.value;

import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Transient;

/**
 * Object which can allocate unique values based on the specified patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class FilteredValueProvider extends ValueProvider
{
    private ValueProvider valueProvider;

    /**
     * Constructor.
     */
    public FilteredValueProvider()
    {
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public FilteredValueProvider(Capability capability)
    {
        super(capability);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.ValueProvider createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueProvider.Filtered();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi)
    {
        super.toApi(valueProviderApi);

        cz.cesnet.shongo.controller.api.ValueProvider.Filtered filteredValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Filtered) valueProviderApi;

        throw new TodoImplementException();
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
            throws FaultException
    {
        super.fromApi(valueProviderApi, entityManager);

        cz.cesnet.shongo.controller.api.ValueProvider.Filtered filteredValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Filtered) valueProviderApi;

        throw new TodoImplementException();
    }

    @Transient
    public ValueGenerator getValueGenerator()
    {
        throw new TodoImplementException();
    }
}
