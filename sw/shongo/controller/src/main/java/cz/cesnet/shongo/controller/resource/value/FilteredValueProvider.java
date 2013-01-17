package cz.cesnet.shongo.controller.resource.value;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.ValueProviderCapability;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.text.Normalizer;
import java.util.Set;

/**
 * Object which can allocate unique values based on the specified patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class FilteredValueProvider extends ValueProvider
{
    /**
     * {@link ValueProvider} which is used for generating values.
     */
    private ValueProvider valueProvider;

    /**
     * Type of filter which is used on requested values.
     */
    private FilterType type;

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

    /**
     * @return {@link #valueProvider}
     */
    @ManyToOne
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
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public FilterType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(FilterType type)
    {
        this.type = type;
    }

    @Override
    public void loadLazyCollections()
    {
        super.loadLazyCollections();

        valueProvider.loadLazyCollections();
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

        filteredValueProviderApi.setType(getType());

        Resource valueProviderResource = valueProvider.getCapabilityResource();
        if (valueProviderResource != getCapability().getResource()) {
            filteredValueProviderApi.setValueProvider(Domain.getLocalDomain().formatId(valueProviderResource));
        }
        else {
            filteredValueProviderApi.setValueProvider(valueProvider.toApi());
        }
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
            throws FaultException
    {
        super.fromApi(valueProviderApi, entityManager);

        cz.cesnet.shongo.controller.api.ValueProvider.Filtered filteredValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Filtered) valueProviderApi;

        if (filteredValueProviderApi.isPropertyFilled(filteredValueProviderApi.TYPE)) {
            setType(filteredValueProviderApi.getType());
        }
        if (valueProviderApi.isPropertyFilled(filteredValueProviderApi.VALUE_PROVIDER)) {
            Object valueProvider = filteredValueProviderApi.getValueProvider();
            if (valueProvider instanceof String) {
                Long resourceId = Domain.getLocalDomain().parseId((String) valueProvider);
                ResourceManager resourceManager = new ResourceManager(entityManager);
                Resource resource = resourceManager.get(resourceId);
                ValueProviderCapability valueProviderCapability = resource.getCapability(ValueProviderCapability.class);
                if (valueProviderCapability == null) {
                    throw new FaultException("Resource '%s' doesn't have value provider capability.", valueProvider);
                }
                setValueProvider(valueProviderCapability.getValueProvider());
            }
            else {
                if (this.valueProvider == null) {
                    this.valueProvider =
                            ValueProvider.createFromApi((cz.cesnet.shongo.controller.api.ValueProvider) valueProvider,
                                    getCapability(), entityManager);
                }
                this.valueProvider
                        .fromApi((cz.cesnet.shongo.controller.api.ValueProvider) valueProvider, entityManager);
            }
        }
    }

    @Override
    public String generateValue(Set<String> usedValues)
    {
        return FilterType.applyFilter(valueProvider.generateValue(usedValues), type);
    }

    @Override
    @Transient
    public String generateValue(Set<String> usedValues, String requestedValue)
    {
        return valueProvider.generateValue(usedValues, FilterType.applyFilter(requestedValue, type));
    }

    @Override
    @Transient
    public ValueProvider getTargetValueProvider()
    {
        return valueProvider;
    }
}
