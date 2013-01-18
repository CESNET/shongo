package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a cache of {@link AliasReservation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueCache extends AbstractReservationCache<ValueProvider, ValueReservation>
{
    private static Logger logger = LoggerFactory.getLogger(ValueCache.class);

    /**
     * Map of {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}s by resource id (used for removing all value providers of a given resource).
     */
    private Map<Long, Set<ValueProvider>> valueProviderByResourceId = new HashMap<Long, Set<ValueProvider>>();

    @Override
    public void addObject(ValueProvider valueProvider, EntityManager entityManager)
    {
        Resource resource = valueProvider.getCapabilityResource();
        Long resourceId = resource.getId();

        // Load lazy collections
        valueProvider.loadLazyCollections();

        // Store capability for removing by resource
        Set<ValueProvider> aliasProviderCapabilities = valueProviderByResourceId.get(resourceId);
        if (aliasProviderCapabilities == null) {
            aliasProviderCapabilities = new HashSet<ValueProvider>();
            valueProviderByResourceId.put(resourceId, aliasProviderCapabilities);
        }
        aliasProviderCapabilities.add(valueProvider);

        super.addObject(valueProvider, entityManager);
    }

    @Override
    public void removeObject(ValueProvider object)
    {
        super.removeObject(object);
    }

    /**
     * Remove all managed {@link AliasProviderCapability}s from given {@code resource} from the {@link ValueCache}.
     *
     * @param resource
     */
    public void removeValueProviders(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove all states for alias providers
        Set<ValueProvider> valueProviders = valueProviderByResourceId.get(resourceId);
        if (valueProviders != null) {
            for (ValueProvider valueProvider : valueProviders) {
                removeObject(valueProvider);
            }
            valueProviders.clear();
        }
    }

    @Override
    public void clear()
    {
        valueProviderByResourceId.clear();
        super.clear();
    }

    @Override
    protected void updateObjectState(ValueProvider object, Interval workingInterval, EntityManager entityManager)
    {
        // Get all allocated values for the value provider and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<ValueReservation> valueReservations = resourceManager.listValueReservationsInInterval(object.getId(),
                getWorkingInterval());
        for (ValueReservation valueReservation : valueReservations) {
            addReservation(object, valueReservation);
        }
    }

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param valueProvider
     * @param requestedValue
     * @param interval
     * @param transaction
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableValue getAvailableValue(ValueProvider valueProvider, String requestedValue, Interval interval,
            Transaction transaction)
    {
        // Check if resource can be allocated and if it is available in the future
        Resource resource = valueProvider.getCapabilityResource();
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return null;
        }

        // Find available alias value
        String value = null;
        // Provided value reservation by which the value is already allocated
        ValueReservation valueReservation = null;

        // Preferably use  provided alias
        Set<ValueReservation> valueReservations = transaction.getProvidedReservations(valueProvider.getId());
        if (valueReservations.size() > 0) {
            if (requestedValue != null) {
                for (ValueReservation possibleValueReservation : valueReservations) {
                    if (possibleValueReservation.getValue().equals(requestedValue)) {
                        valueReservation = possibleValueReservation;
                        value = valueReservation.getValue();
                        break;
                    }
                }
            }
            else {
                valueReservation = valueReservations.iterator().next();
                value = valueReservation.getValue();
            }
        }
        // Else use generated value
        if (value == null) {
            ObjectState<ValueReservation> valueProviderState =
                    getObjectStateRequired(valueProvider.getTargetValueProvider());
            Set<ValueReservation> allocatedValues = valueProviderState.getReservations(interval, transaction);
            Set<String> usedValues = new HashSet<String>();
            for (ValueReservation allocatedValue : allocatedValues) {
                usedValues.add(allocatedValue.getValue());
            }
            if (requestedValue != null) {
                value = valueProvider.generateValue(usedValues, requestedValue);
            }
            else {
                value = valueProvider.generateValue(usedValues);
            }
        }
        if (value == null) {
            return null;
        }
        AvailableValue availableAlias = new AvailableValue();
        availableAlias.setValue(value);
        availableAlias.setValueReservation(valueReservation);
        return availableAlias;
    }

    /**
     * Transaction for {@link ValueCache}.
     */
    public static class Transaction
            extends AbstractReservationCache.Transaction<ValueReservation>
    {
    }
}
