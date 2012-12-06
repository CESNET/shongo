package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.resource.*;
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
public class AliasCache extends AbstractReservationCache<AliasProviderCapability, AliasReservation>
{
    private static Logger logger = LoggerFactory.getLogger(AliasCache.class);

    /**
     * Map of {@link AliasProviderCapability}s by resource identifier (used for removing all capabilities
     * of a given resource).
     */
    private Map<Long, Set<AliasProviderCapability>> aliasProviderCapabilitiesByResourceId =
            new HashMap<Long, Set<AliasProviderCapability>>();

    @Override
    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading alias providers...");

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AliasProviderCapability> aliasProviders = resourceManager.listCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            addObject(aliasProvider, entityManager);
        }
    }

    @Override
    public void addObject(AliasProviderCapability aliasProviderCapability, EntityManager entityManager)
    {
        Resource resource = aliasProviderCapability.getResource();
        Long resourceId = resource.getId();

        // Store capability for removing by resource
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        if (aliasProviderCapabilities == null) {
            aliasProviderCapabilities = new HashSet<AliasProviderCapability>();
            aliasProviderCapabilitiesByResourceId.put(resourceId, aliasProviderCapabilities);
        }
        aliasProviderCapabilities.add(aliasProviderCapability);

        super.addObject(aliasProviderCapability, entityManager);
    }

    @Override
    public void removeObject(AliasProviderCapability object)
    {
        super.removeObject(object);
    }

    @Override
    public void clear()
    {
        aliasProviderCapabilitiesByResourceId.clear();
        super.clear();
    }

    /**
     * Remove all managed {@link AliasProviderCapability}s from given {@code resource} from the {@link AliasCache}.
     *
     * @param resource
     */
    public void removeAliasProviders(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove all states for alias providers
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        if (aliasProviderCapabilities != null) {
            for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
                removeObject(aliasProviderCapability);
            }
            aliasProviderCapabilities.clear();
        }
    }

    @Override
    protected void updateObjectState(AliasProviderCapability object, Interval workingInterval,
            EntityManager entityManager)
    {
        // Get all allocated aliases for the alias provider and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AliasReservation> aliasReservations = resourceManager.listAliasReservationsInInterval(object.getId(),
                getWorkingInterval());
        for (AliasReservation aliasReservation : aliasReservations) {
            addReservation(object, aliasReservation);
        }
    }

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param aliasProviderCapability
     * @param interval
     * @param transaction
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableAlias getAvailableAlias(AliasProviderCapability aliasProviderCapability, Interval interval,
            Transaction transaction)
    {
        // Check if resource can be allocated and if it is available in the future
        Resource resource = aliasProviderCapability.getResource();
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return null;
        }

        // Find available alias value
        String aliasValue;
        // Provided alias reservation by which the alias value is already allocated
        AliasReservation aliasReservation = null;

        // Preferably use  provided alias
        Set<AliasReservation> aliasReservations = transaction.getProvidedReservations(aliasProviderCapability.getId());
        if (aliasReservations.size() > 0) {
            aliasReservation = aliasReservations.iterator().next();
            aliasValue = aliasReservation.getAliasValue();
        }
        // Else use generated alias
        else {
            ObjectState<AliasReservation> aliasProviderState = getObjectState(aliasProviderCapability);
            Set<AliasReservation> allocatedAliases = aliasProviderState.getReservations(interval, transaction);
            AliasGenerator aliasGenerator = aliasProviderCapability.getAliasGenerator();
            for (AliasReservation allocatedAliasReservation : allocatedAliases) {
                aliasGenerator.addAliasValue(allocatedAliasReservation.getAliasValue());
            }
            aliasValue = aliasGenerator.generateValue();
        }
        if (aliasValue == null) {
            return null;
        }
        AvailableAlias availableAlias = new AvailableAlias();
        availableAlias.setAliasProviderCapability(aliasProviderCapability);
        availableAlias.setAliasValue(aliasValue);
        availableAlias.setAliasReservation(aliasReservation);
        return availableAlias;
    }

    /**
     * Transaction for {@link AliasCache}.
     */
    public static class Transaction
            extends AbstractReservationCache.Transaction<AliasReservation>
    {
    }
}
