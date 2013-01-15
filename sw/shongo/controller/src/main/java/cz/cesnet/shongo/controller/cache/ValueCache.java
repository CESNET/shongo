package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.TodoImplementException;
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
     * Map of {@link ValueProvider}s by resource id (used for removing all value providers of a given resource).
     */
    private Map<Long, Set<ValueProvider>> valueProviderByResourceId = new HashMap<Long, Set<ValueProvider>>();

    @Override
    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading value providers...");

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<ValueProvider> valueProviders = resourceManager.listValueProviders();
        for (ValueProvider valueProvider : valueProviders) {
            addObject(valueProvider, entityManager);
        }
    }

    @Override
    public void addObject(ValueProvider valueProvider, EntityManager entityManager)
    {
        Resource resource = valueProvider.getCapabilityResource();
        Long resourceId = resource.getId();

        // Load lazy collections
        valueProvider.getPatterns().size();

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

    @Override
    public void clear()
    {
        valueProviderByResourceId.clear();
        super.clear();
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
     * @param interval
     * @param transaction
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableValue getAvailableAlias(ValueProvider valueProvider, String requestedValue,
            Interval interval, Transaction transaction)
    {
        // Check if resource can be allocated and if it is available in the future
        Resource resource = valueProvider.getResource();
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return null;
        }

        // Find available alias value
        String aliasValue = null;
        // Provided alias reservation by which the alias value is already allocated
        AliasReservation aliasReservation = null;

        // Preferably use  provided alias
        Set<AliasReservation> aliasReservations = transaction.getProvidedReservations(valueProvider.getId());
        if (aliasReservations.size() > 0) {
            if (requestedValue != null) {
                for (AliasReservation possibleAliasReservation : aliasReservations) {
                    if (possibleAliasReservation.getAliasValue().equals(requestedValue)) {
                        aliasReservation = possibleAliasReservation;
                        aliasValue = aliasReservation.getAliasValue();
                        break;
                    }
                }
            }
            else {
                aliasReservation = aliasReservations.iterator().next();
                aliasValue = aliasReservation.getAliasValue();
            }
        }
        // Else use generated alias
        if (aliasValue == null) {
            ObjectState<AliasReservation> aliasProviderState = getObjectStateRequired(valueProvider);
            Set<AliasReservation> allocatedAliases = aliasProviderState.getReservations(interval, transaction);

            if (true) {
                throw new TodoImplementException();
            }
            ValueGenerator aliasGenerator = null;//aliasProviderCapability.getAliasGenerator();
            for (AliasReservation allocatedAliasReservation : allocatedAliases) {
                aliasGenerator.addValue(allocatedAliasReservation.getAliasValue());
            }
            if (requestedValue != null) {
                if (aliasGenerator.isValueAvailable(requestedValue)) {
                    aliasValue = requestedValue;
                }
            }
            else {
                aliasValue = aliasGenerator.generateValue();
            }
        }
        if (aliasValue == null) {
            return null;
        }
        AvailableValue availableAlias = new AvailableValue();
        availableAlias.setValueProvider(valueProvider);
        availableAlias.setValue(aliasValue);
        availableAlias.setValueReservation(aliasReservation);
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
