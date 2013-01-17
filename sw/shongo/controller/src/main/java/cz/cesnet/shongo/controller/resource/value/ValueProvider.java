package cz.cesnet.shongo.controller.resource.value;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

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
    @OneToOne(optional = false)
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
     * @throws FaultException
     */
    public static ValueProvider createFromApi(cz.cesnet.shongo.controller.api.ValueProvider api,
            Capability capability, EntityManager entityManager) throws FaultException
    {
        ValueProvider valueProvider;
        if (api instanceof cz.cesnet.shongo.controller.api.ValueProvider.Pattern) {
            valueProvider = new PatternValueProvider(capability);
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ValueProvider.Filtered) {
            valueProvider = new FilteredValueProvider(capability);
        }
        else {
            throw new TodoImplementException(api.getClass().getName());
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
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
            throws FaultException
    {
    }

    /**
     * @param usedValues set of already used values (which should not be generated)
     * @return new generated value if available,
     *         null otherwise
     */
    public abstract String generateValue(Set<String> usedValues);

    /**
     *
     * @param usedValues     set of already used values (which should not be generated)
     * @param requestedValue which should be generated
     * @return new generated value based on {@code value} if available,
     *         null otherwise
     */
    @Transient
    public abstract String generateValue(Set<String> usedValues, String requestedValue);

    /**
     * @param usedValues set of already used values (which should not be generated)
     * @return new generated value and append the value to the {@code usedValues}
     */
    public final String generateAddedValue(Set<String> usedValues)
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
}
