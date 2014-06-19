package cz.cesnet.shongo.controller.booking.value;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.*;


/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a value from {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ValueReservation extends TargetedReservation
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider} from which the value is allocated.
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
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public ValueProvider getValueProvider()
    {
        return getLazyImplementation(valueProvider);
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
    @Column(nullable = false, length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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
    public cz.cesnet.shongo.controller.api.ValueReservation toApi(EntityManager entityManager, boolean administrator)
    {
        return (cz.cesnet.shongo.controller.api.ValueReservation) super.toApi(entityManager, administrator);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueReservation();
    }

    @Override
    protected void toApi(Reservation api, EntityManager entityManager, boolean admin)
    {
        cz.cesnet.shongo.controller.api.ValueReservation valueReservationApi =
                (cz.cesnet.shongo.controller.api.ValueReservation) api;
        Resource valueProviderResource = valueProvider.getCapabilityResource();
        valueReservationApi.setResourceId(ObjectIdentifier.formatId(valueProviderResource));
        valueReservationApi.setResourceName(valueProviderResource.getName());
        valueReservationApi.setValue(getValue());
        super.toApi(api, entityManager, admin);
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return valueProvider.getId();
    }

    @Override
    @Transient
    public Resource getAllocatedResource()
    {
        return valueProvider.getCapabilityResource();
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
