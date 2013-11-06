package cz.cesnet.shongo.controller.booking.value;

import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.booking.resource.Capability;

import javax.persistence.*;

/**
 * Capability tells that the resource can allocate unique values base on the patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ValueProviderCapability extends Capability
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider} which will be used for generating values.
     */
    private ValueProvider valueProvider;

    /**
     * Constructor.
     */
    public ValueProviderCapability()
    {
    }

    /**
     * @return {@link #valueProvider}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
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

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.ValueProviderCapability valueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProviderCapability) api;
        valueProviderApi.setValueProvider(valueProvider.toApi());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ValueProviderCapability valueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProviderCapability) api;
        setValueProvider(ValueProvider.modifyFromApi(
                valueProviderApi.getValueProvider(), this.valueProvider, this, entityManager));

        super.fromApi(api, entityManager);
    }
}
