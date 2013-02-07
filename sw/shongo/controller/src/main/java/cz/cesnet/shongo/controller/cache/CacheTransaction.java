package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.Interval;

import java.util.*;

/**
 * Transaction for the {@link cz.cesnet.shongo.controller.Cache}.
 */
public class CacheTransaction
{
    /**
     * Interval for which the task is performed.
     */
    private final Interval interval;

    /**
     * {@link AbstractReservationCache.Transaction} for {@link ResourceReservation}s.
     */
    private AbstractReservationCache.Transaction<ResourceReservation> resourceCacheTransaction =
            new AbstractReservationCache.Transaction<ResourceReservation>();

    /**
     * {@link AbstractReservationCache.Transaction} for {@link ValueReservation}s.
     */
    private AbstractReservationCache.Transaction<ValueReservation> valueProviderCacheTransaction =
            new AbstractReservationCache.Transaction<ValueReservation>();

    /**
     * Set of resources referenced from {@link ResourceReservation}s in the transaction.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by {@link cz.cesnet.shongo.controller.reservation.Reservation} which allocates them.
     */
    private Map<Executable, Reservation> providedReservationByExecutable = new HashMap<Executable, Reservation>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by {@link cz.cesnet.shongo.controller.reservation.Reservation} which allocates them.
     */
    private Map<Long, Set<AliasReservation>> providedReservationsByAliasProviderId =
            new HashMap<Long, Set<AliasReservation>>();

    /**
     * Set of allocated {@link cz.cesnet.shongo.controller.reservation.Reservation}s.
     */
    private Set<Reservation> allocatedReservations = new HashSet<Reservation>();

    /**
     * Constructor.
     */
    public CacheTransaction(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @return {@link #resourceCacheTransaction}
     */
    public AbstractReservationCache.Transaction<ResourceReservation> getResourceCacheTransaction()
    {
        return resourceCacheTransaction;
    }

    /**
     * @return {@link #valueProviderCacheTransaction}
     */
    public AbstractReservationCache.Transaction<ValueReservation> getValueProviderCacheTransaction()
    {
        return valueProviderCacheTransaction;
    }

    /**
     * @param resource to be added to the {@link #referencedResources}
     */
    public void addReferencedResource(Resource resource)
    {
        referencedResources.add(resource);
    }

    /**
     * @param resource to be checked
     * @return true if given resource was referenced by any {@link ResourceReservation} added to the transaction,
     *         false otherwise
     */
    public boolean containsResource(Resource resource)
    {
        return referencedResources.contains(resource);
    }

    /**
     * @param reservation to be added to the {@link CacheTransaction} as already allocated.
     */
    public void addAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.add(reservation)) {
            // Reservation is already added as allocated to the transaction
            return;
        }

        if (reservation.getSlot().contains(getInterval())) {
            if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                Resource resource = resourceReservation.getResource();
                resourceCacheTransaction.addAllocatedReservation(resource.getId(), resourceReservation);
                addReferencedResource(resource);
            }
            else if (reservation instanceof ValueReservation) {
                ValueReservation valueReservation = (ValueReservation) reservation;
                valueProviderCacheTransaction.addAllocatedReservation(
                        valueReservation.getValueProvider().getId(), valueReservation);
            }
        }
    }

    /**
     * @param reservation to be added to the {@link CacheTransaction} as provided (the resources allocated by
     *                    the {@code reservation} are considered as available).
     */
    public void addProvidedReservation(Reservation reservation)
    {
        if (reservation.getSlot().contains(getInterval())) {
            Executable executable = reservation.getExecutable();
            if (executable != null) {
                providedReservationByExecutable.put(executable, reservation);
            }
            if (reservation instanceof ExistingReservation) {
                throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
                // It will be necessary to evaluate existing reservation to target reservation and to keep the
                // reference to the existing reservation.
                // In the end the existing reservation must be used as child reservation for any newly allocated
                // reservations and not the target reservation itself (a collision would occur).
            }
            if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                resourceCacheTransaction.addProvidedReservation(
                        resourceReservation.getResource().getId(), resourceReservation);
            }
            else if (reservation instanceof ValueReservation) {
                ValueReservation valueReservation = (ValueReservation) reservation;
                valueProviderCacheTransaction.addProvidedReservation(
                        valueReservation.getValueProvider().getId(), valueReservation);
            }
            else if (reservation instanceof AliasReservation) {
                AliasReservation aliasReservation = (AliasReservation) reservation;
                Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
                Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
                if (aliasReservations == null) {
                    aliasReservations = new HashSet<AliasReservation>();
                    providedReservationsByAliasProviderId.put(aliasProviderId, aliasReservations);
                }
                aliasReservations.add(aliasReservation);
            }
        }

        // Add all child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            addProvidedReservation(childReservation);
        }
    }

    /**
     * @param reservation to be removed from the provided {@link cz.cesnet.shongo.controller.reservation.Reservation}s from the {@link CacheTransaction}
     */
    public void removeProvidedReservation(Reservation reservation)
    {
        Executable executable = reservation.getExecutable();
        if (executable != null) {
            providedReservationByExecutable.remove(executable);
        }
        if (reservation instanceof ExistingReservation) {
            throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
        }
        if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            resourceCacheTransaction.removeProvidedReservation(
                    resourceReservation.getResource().getId(), resourceReservation);
        }
        else if (reservation instanceof ValueReservation) {
            ValueReservation aliasReservation = (ValueReservation) reservation;
            valueProviderCacheTransaction.removeProvidedReservation(
                    aliasReservation.getValueProvider().getId(), aliasReservation);
        }
        else if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
            Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
            if (aliasReservations != null) {
                aliasReservations.remove(aliasReservation);
            }
        }
    }

    /**
     * @param executableType
     * @return collection of provided {@link cz.cesnet.shongo.controller.executor.Executable}s of given {@code executableType}
     */
    public <T extends Executable> Collection<T> getProvidedExecutables(Class<T> executableType)
    {
        Set<T> providedExecutables = new HashSet<T>();
        for (Executable providedExecutable : providedReservationByExecutable.keySet()) {
            if (executableType.isInstance(providedExecutable)) {
                providedExecutables.add(executableType.cast(providedExecutable));
            }
        }
        return providedExecutables;
    }

    /**
     * @param executable
     * @return provided {@link cz.cesnet.shongo.controller.reservation.Reservation} for given {@code executable}
     */
    public Reservation getProvidedReservationByExecutable(Executable executable)
    {
        Reservation providedReservation = providedReservationByExecutable.get(executable);
        if (providedReservation == null) {
            throw new IllegalArgumentException("Provided reservation doesn't exists for given executable!");
        }
        return providedReservation;
    }

    /**
     * @param aliasProvider
     * @return collection of provided {@link cz.cesnet.shongo.controller.reservation.AliasReservation}
     */
    public Collection<AliasReservation> getProvidedAliasReservations(AliasProviderCapability aliasProvider)
    {
        Long aliasProviderId = aliasProvider.getId();
        Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
        if (aliasReservations != null) {
            return aliasReservations;
        }
        return Collections.emptyList();
    }

    /**
     * @param resource for which the provided {@link cz.cesnet.shongo.controller.reservation.ResourceReservation}s should be returned
     * @return provided {@link cz.cesnet.shongo.controller.reservation.ResourceReservation}s for given {@code resource}
     */
    public Set<ResourceReservation> getProvidedResourceReservations(Resource resource)
    {
        return resourceCacheTransaction.getProvidedReservations(resource.getId());
    }

    /**
     * @return {@link #valueProviderCacheTransaction}
     */
    public ValueCache.Transaction getValueCacheTransaction()
    {
        return valueProviderCacheTransaction;
    }
}
