package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;

import javax.persistence.*;


/**
 * Represents a {@link Reservation} for a value from {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ValueReservation extends Reservation
{
    /**
     * {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider} from which the value is allocated.
     */
    private ValueProvider valueProvider;

    /**
     * Value which is allocated.
     */
    private String value;

    /**
     * Constructor.
     */
    public ValueReservation()
    {
    }

    /**
     * @return {@link #valueProvider}
     */
    @ManyToOne(optional = false)
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

    /**
     * @return {@link #value}
     */
    @Column(nullable = false)
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public cz.cesnet.shongo.controller.api.ValueReservation toApi()
    {
        return (cz.cesnet.shongo.controller.api.ValueReservation) super.toApi();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api)
    {
        cz.cesnet.shongo.controller.api.ValueReservation valueReservationApi =
                (cz.cesnet.shongo.controller.api.ValueReservation) api;
        Resource valueProviderResource = valueProvider.getCapabilityResource();
        valueReservationApi.setResourceId(EntityIdentifier.formatId(valueProviderResource));
        valueReservationApi.setResourceName(valueProviderResource.getName());
        valueReservationApi.setValue(getValue());
        super.toApi(api);
    }

    /**
     * @param value to be evaluated
     * @return evaluated value
     */
    public String evaluateValue(String value)
    {
        value = value.replace("{value}", this.value);
        return value;
    }
}
