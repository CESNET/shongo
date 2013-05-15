package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.ValueProviderCapability;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a {@link Specification} for value(s) from {@link ValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ValueSpecification extends Specification
        implements ReservationTaskProvider
{
    /**
     * {@link ValueProvider} from which the value(s) should be allocated.
     */
    private ValueProvider valueProvider;

    /**
     * Values which should be allocated.
     */
    private Set<String> values = new HashSet<String>();

    /**
     * Constructor.
     */
    public ValueSpecification()
    {
    }

    /**
     * @return {@link #valueProvider}
     */
    @ManyToOne(optional = false)
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
     * @return {@link #technologies}
     */
    @ElementCollection()
    @Column(name = "requested_values")
    @Access(AccessType.FIELD)
    public Set<String> getValues()
    {
        return Collections.unmodifiableSet(values);
    }

    /**
     * @param values sets the {@link #values}
     */
    public void setValues(Set<String> values)
    {
        this.values.clear();
        this.values.addAll(values);
    }

    /**
     * @param value to be added to the {@link #values}
     */
    public void addValue(String value)
    {
        values.add(value);
    }

    /**
     * @param value to be removed from the {@link #values}
     */
    public void removeValue(String value)
    {
        values.remove(value);
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        ValueSpecification valueSpecification = (ValueSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getValues(), valueSpecification.getValues())
                || !ObjectHelper.isSame(getValueProvider(), valueSpecification.getValueProvider());

        setValueProvider(valueSpecification.getValueProvider());
        setValues(valueSpecification.getValues());

        return modified;
    }

    @Override
    public ValueSpecification clone()
    {
        ValueSpecification aliasSpecification = new ValueSpecification();
        aliasSpecification.setValueProvider(getValueProvider());
        aliasSpecification.setValues(getValues());
        aliasSpecification.updateTechnologies();
        return aliasSpecification;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        int valuesCount = values.size();
        if (valuesCount == 0) {
            return new ValueReservationTask(schedulerContext, valueProvider, null);
        }
        else if (valuesCount == 1) {
            return new ValueReservationTask(schedulerContext, valueProvider, values.iterator().next());
        }
        else {
            return new ReservationTask(schedulerContext)
            {
                @Override
                protected Reservation allocateReservation() throws SchedulerException
                {
                    for (String value : values) {
                        addChildReservation(new ValueReservationTask(schedulerContext, valueProvider, value));
                    }
                    // Create compound reservation request
                    Reservation reservation = new Reservation();
                    reservation.setSlot(getInterval());
                    return reservation;
                }
            };
        }
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueSpecification();
    }

    @Override
    public cz.cesnet.shongo.controller.api.ValueSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.ValueSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ValueSpecification valueSpecificationApi =
                (cz.cesnet.shongo.controller.api.ValueSpecification) specificationApi;
        valueSpecificationApi.setResourceId(
                EntityIdentifier.formatId(getValueProvider().getCapabilityResource()));
        for (String value : getValues()) {
            valueSpecificationApi.addValue(value);
        }

        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ValueSpecification valueSpecificationApi =
                (cz.cesnet.shongo.controller.api.ValueSpecification) specificationApi;
        Long resourceId = EntityIdentifier.parseId(
                Resource.class, valueSpecificationApi.getResourceId());
        ResourceManager resourceManager = new ResourceManager(entityManager);
        Resource resource = resourceManager.get(resourceId);
        ValueProviderCapability valueProviderCapability = resource.getCapability(ValueProviderCapability.class);
        if (valueProviderCapability == null) {
            throw new RuntimeException(String.format("Resource '%s' doesn't have %s.",
                    ValueProviderCapability.class.getSimpleName(), valueSpecificationApi.getResourceId()));
        }
        setValueProvider(valueProviderCapability.getValueProvider());

        // Create values
        for (String value : valueSpecificationApi.getValues()) {
            if (valueSpecificationApi.isPropertyItemMarkedAsNew(valueSpecificationApi.VALUES, value)) {
                addValue(value);
            }
        }
        // Delete values
        Set<String> valuesToDelete =
                valueSpecificationApi.getPropertyItemsMarkedAsDeleted(valueSpecificationApi.VALUES);
        for (String value : valuesToDelete) {
            removeValue(value);
        }

        super.fromApi(specificationApi, entityManager);
    }
}
