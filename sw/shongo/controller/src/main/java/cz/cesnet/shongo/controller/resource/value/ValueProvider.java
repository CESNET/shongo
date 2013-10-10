package cz.cesnet.shongo.controller.resource.value;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.ValueProviderCapability;
import cz.cesnet.shongo.TodoImplementException;

import javax.persistence.*;
import java.util.Set;

/**
 * Object which can allocate unique values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class ValueProvider extends PersistentObject
{
    /**
     * {@link Capability} which owns the {@link ValueProvider}.
     */
    private Capability capability;

    /**
     * Constructor.
     */
    public ValueProvider()
    {
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public ValueProvider(Capability capability)
    {
        this.capability = capability;
    }

    /**
     * @return {@link #capability}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Capability getCapability()
    {
        return capability;
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.resource.Resource} from {@link #capability}
     */
    @Transient
    public Resource getCapabilityResource()
    {
        return capability.getResource();
    }

    @Override
    public void loadLazyProperties()
    {
        getCapabilityResource().loadLazyProperties();
        super.loadLazyProperties();
    }

    /**
     * @return converted {@link PatternValueProvider} to API
     */
    public final cz.cesnet.shongo.controller.api.ValueProvider toApi()
    {
        cz.cesnet.shongo.controller.api.ValueProvider api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link ValueProvider} from API
     */
    public static ValueProvider createFromApi(cz.cesnet.shongo.controller.api.ValueProvider api,
            Capability capability, EntityManager entityManager)
    {
        ValueProvider valueProvider;
        if (api instanceof cz.cesnet.shongo.controller.api.ValueProvider.Pattern) {
            valueProvider = new PatternValueProvider(capability);
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ValueProvider.Filtered) {
            valueProvider = new FilteredValueProvider(capability);
        }
        else {
            throw new TodoImplementException(api.getClass());
        }
        valueProvider.fromApi(api, entityManager);
        return valueProvider;
    }

    /**
     * @return new instance of API capability
     */
    protected abstract cz.cesnet.shongo.controller.api.ValueProvider createApi();

    /**
     * @param valueProviderApi to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi)
    {
        valueProviderApi.setId(getId());
    }

    /**
     * Synchronize capability from API
     *
     * @param valueProviderApi
     * @param entityManager
     */
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
    {
    }

    /**
     * @param usedValues set of already used values (which should not be generated)
     * @return new generated value if available,
     *         null otherwise
     */
    public abstract String generateValue(Set<String> usedValues) throws NoAvailableValueException;

    /**
     * @param usedValues     set of already used values (which should not be generated)
     * @param requestedValue which should be generated
     * @return new generated value based on {@code value} if available,
     *         null otherwise
     */
    @Transient
    public abstract String generateValue(Set<String> usedValues, String requestedValue)
            throws ValueAlreadyAllocatedException, InvalidValueException;

    /**
     * @param usedValues set of already used values (which should not be generated)
     * @return new generated value and append the value to the {@code usedValues}
     */
    public final String generateAddedValue(Set<String> usedValues) throws NoAvailableValueException
    {
        String generatedValue = generateValue(usedValues);
        usedValues.add(generatedValue);
        return generatedValue;
    }

    /**
     * @return {@link ValueProvider} which should be used in {@link ValueReservation}s for values generated
     *         by this {@link ValueProvider}.
     */
    @Transient
    public ValueProvider getTargetValueProvider()
    {
        return this;
    }

    /**
     * @param object
     * @param valueProvider
     * @param capability
     * @param entityManager
     * @return {@link ValueProvider}
     */
    public static ValueProvider modifyFromApi(Object object, ValueProvider valueProvider, Capability capability,
            EntityManager entityManager)
    {
        if (object instanceof String) {
            Long resourceId = EntityIdentifier.parseId(
                    cz.cesnet.shongo.controller.resource.Resource.class, (String) object);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            Resource resource = resourceManager.get(resourceId);
            ValueProviderCapability valueProviderCapability = resource.getCapability(ValueProviderCapability.class);
            if (valueProviderCapability == null) {
                throw new RuntimeException(String.format("Resource '%s' doesn't have value provider capability.",
                        resourceId));
            }
            if (valueProvider != null) {
                entityManager.remove(valueProvider);
            }
            return valueProviderCapability.getValueProvider();
        }
        else {
            cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi =
                    (cz.cesnet.shongo.controller.api.ValueProvider) object;

            // Create new value provider from API
            ValueProvider newValueProvider = ValueProvider.createFromApi(valueProviderApi, capability, entityManager);

            // Clear value provider if it is set by the resource id
            if (valueProvider != null && valueProvider.getCapability() != capability) {
                valueProvider = null;
            }
            // If value provider is not set, set the new
            if (valueProvider == null) {
                return newValueProvider;
            }
            // If the new value provider is of different type, delete the old and set the new
            else if (!valueProvider.getClass().equals(newValueProvider.getClass())
                    || valueProviderApi.getId() == null) {
                entityManager.remove(valueProvider);
                return newValueProvider;
            }
            // Otherwise discard the new vlaue provider and modify the existing one
            else {
                valueProvider.fromApi(valueProviderApi, entityManager);
                return valueProvider;
            }
        }
    }

    /**
     * No value can be generated because all values are already allocated.
     */
    public static class NoAvailableValueException extends Exception
    {
    }

    /**
     * Requested value doesn't match the constraints.
     */
    public static class InvalidValueException extends Exception
    {
    }

    /**
     * Requested value is already allocated.
     */
    public static class ValueAlreadyAllocatedException extends Exception
    {
    }
}
