package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link ReservationTask} for one or multiple {@link AliasReservation}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasReservationTask extends ReservationTask
{
    /**
     * Restricts {@link AliasType} for allocation of {@link Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} for allocation of {@link Alias}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link DeviceResource} for which the {@link AliasReservation} is being allocated and to which it
     * will be assigned (so no permanent room should be allocated).
     */
    private DeviceResource targetResource;

    /**
     * {@link AliasProviderCapability}s for which the {@link AliasReservation} can be allocated.
     */
    private List<AliasProviderCapability> aliasProviderCapabilities = new ArrayList<AliasProviderCapability>();

    /**
     * Constructor.
     *
     * @param context sets the {@link #context}
     */
    public AliasReservationTask(Context context)
    {
        super(context);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        this.technologies.addAll(technologies);
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public void addAliasType(AliasType aliasType)
    {
        aliasTypes.add(aliasType);
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @param targetResource sets the {@link #targetResource}
     */
    public void setTargetResource(DeviceResource targetResource)
    {
        this.targetResource = targetResource;
    }

    /**
     * @param aliasProviderCapabilities sets the {@link #aliasProviderCapabilities}
     */
    public void setAliasProviderCapabilities(List<AliasProviderCapability> aliasProviderCapabilities)
    {
        this.aliasProviderCapabilities.clear();
        this.aliasProviderCapabilities.addAll(aliasProviderCapabilities);
    }

    /**
     * @param aliasProviderCapability to be added to the {@link #aliasProviderCapabilities}
     */
    public void addAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapabilities.add(aliasProviderCapability);
    }

    @Override
    protected Report createdMainReport()
    {
        return new AllocatingAliasReport(technologies, aliasTypes, value);
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        Context context = getContext();
        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        CacheTransaction cacheTransaction = getCacheTransaction();

        // Get possible alias providers
        Collection<AliasProviderCapability> possibleAliasProviders;
        if (aliasProviderCapabilities.size() > 0) {
            // Use only specified alias providers
            possibleAliasProviders = aliasProviderCapabilities;
        }
        else {
            // Use all alias providers from the cache
            possibleAliasProviders = cache.getAliasProviders();
        }

        // Find matching alias providers
        beginReport(new FindingAvailableResourceReport(), true);
        List<AliasProviderCapability> aliasProviders = new ArrayList<AliasProviderCapability>();
        Map<AliasProviderCapability, String> valueByAliasProvider = new HashMap<AliasProviderCapability, String>();
        for (AliasProviderCapability aliasProvider : possibleAliasProviders) {
            // Check whether alias provider matches the criteria
            if (aliasProvider.isRestrictedToResource() && targetResource != null) {
                // Skip alias providers which cannot be used for specified target resource
                if (!aliasProvider.getResource().getId().equals(targetResource.getId())) {
                    continue;
                }
            }
            if (technologies.size() > 0 && !aliasProvider.providesAliasTechnology(technologies)) {
                continue;
            }
            if (aliasTypes.size() > 0 && !aliasProvider.providesAliasType(aliasTypes)) {
                continue;
            }
            if (value != null) {
                String value = aliasProvider.parseValue(this.value);
                valueByAliasProvider.put(aliasProvider, value);
            }
            aliasProviders.add(aliasProvider);
            addReport(new ResourceReport(aliasProvider, Report.State.NONE));
        }
        if (aliasProviders.size() == 0) {
            throw createReportFailureForThrowing().exception();
        }
        endReport();

        // Build map of provided alias reservation by alias provider
        final Map<AliasProviderCapability, AliasReservation> availableProvidedAlias =
                new HashMap<AliasProviderCapability, AliasReservation>();
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            Collection<AliasReservation> providedAliasReservations =
                    cacheTransaction.getProvidedAliasReservations(aliasProvider);
            if (providedAliasReservations.size() > 0) {
                AliasReservation providedAliasReservation = null;
                String value = valueByAliasProvider.get(aliasProvider);
                if (value != null) {
                    for (AliasReservation possibleProvidedAliasReservation : providedAliasReservations) {
                        if (possibleProvidedAliasReservation.getValue().equals(value)) {
                            providedAliasReservation = possibleProvidedAliasReservation;
                            break;
                        }
                    }
                }
                else {
                    providedAliasReservation = providedAliasReservations.iterator().next();
                }
                if (providedAliasReservation != null) {
                    availableProvidedAlias.put(aliasProvider, providedAliasReservation);
                }
            }
        }

        // Sort alias providers, prefer the ones with provided aliases
        addReport(new SortingResourcesReport());
        Collections.sort(aliasProviders, new Comparator<AliasProviderCapability>()
        {
            @Override
            public int compare(AliasProviderCapability provider1, AliasProviderCapability provider2)
            {
                boolean firstHasProvidedAlias = availableProvidedAlias.containsKey(provider1);
                boolean secondHasProvidedAlias = availableProvidedAlias.containsKey(provider2);
                if (firstHasProvidedAlias && !secondHasProvidedAlias) {
                    return -1;
                }
                if (!firstHasProvidedAlias && secondHasProvidedAlias) {
                    return 1;
                }
                return 0;
            }
        });

        // If target resource for which the alias will be used is not specified,
        // we should prefer alias providers which doesn't restrict target resource
        if (targetResource == null) {
            // Sort the alias providers in the way that the ones which restricts target resource are at the end
            Collections.sort(aliasProviders, new Comparator<AliasProviderCapability>()
            {
                @Override
                public int compare(AliasProviderCapability provider1, AliasProviderCapability provider2)
                {
                    if (!provider1.isRestrictedToResource() && provider2.isRestrictedToResource()) {
                        return -1;
                    }
                    if (provider1.isRestrictedToResource() && !provider2.isRestrictedToResource()) {
                        return 1;
                    }
                    return 0;
                }
            });
        }

        // Find available value in the alias providers
        ValueReservation availableValueReservation = null;
        AliasProviderCapability availableAliasProvider = null;
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            beginReport(new AllocatingResourceReport(aliasProvider), true);

            // Preferably reuse provided alias reservation
            AliasReservation providedAliasReservation = availableProvidedAlias.get(aliasProvider);
            if (providedAliasReservation != null) {
                addReport(new ReusingReservationReport(providedAliasReservation));

                ExistingReservation existingReservation = new ExistingReservation();
                existingReservation.setSlot(getInterval());
                existingReservation.setReservation(providedAliasReservation);
                cacheTransaction.removeProvidedReservation(providedAliasReservation);
                return existingReservation;
            }

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(aliasProvider, context);
            } catch (ReportException exception) {
                endReportError(exception.getReport());
                continue;
            }

            // Get new available value
            CacheTransaction.Savepoint cacheTransactionSavepoint = cacheTransaction.createSavepoint();
            try {
                String value = valueByAliasProvider.get(aliasProvider);
                ValueReservationTask valueReservationTask =
                        new ValueReservationTask(context, aliasProvider.getValueProvider(), value);
                availableValueReservation = addChildReservation(valueReservationTask, ValueReservation.class);
            }
            catch (ReportException exception) {
                cacheTransactionSavepoint.revert();

                // End allocating current alias provider and try to allocate next one
                endReport();
                continue;
            }
            finally {
                cacheTransactionSavepoint.destroy();
            }

            availableAliasProvider = aliasProvider;
            endReport();
            break;
        }
        if (availableValueReservation == null) {
            throw createReportFailureForThrowing().exception();
        }

        // Create new reservation
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(getInterval());
        aliasReservation.setAliasProviderCapability(availableAliasProvider);
        aliasReservation.setValueReservation(availableValueReservation);

        // If alias should be allocated as permanent room, create room endpoint with zero licenses
        // (so we don't need reservation for the room).
        // The permanent room should not be created if the alias will be used for any specified target resource.
        if (availableAliasProvider.isPermanentRoom() && context.isExecutableAllowed() && targetResource == null) {
            Resource resource = availableAliasProvider.getResource();
            RoomProviderCapability roomProvider = resource.getCapability(RoomProviderCapability.class);
            if (roomProvider == null) {
                throw new RuntimeException("Permanent room should be enabled only for device resource"
                        + " with room provider capability.");
            }
            ResourceRoomEndpoint roomEndpoint = new ResourceRoomEndpoint();
            roomEndpoint.setSlot(getInterval());
            roomEndpoint.setRoomProviderCapability(roomProvider);
            roomEndpoint.setRoomDescription(context.getReservationDescription());
            roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
            Set<Technology> technologies = roomEndpoint.getTechnologies();
            for (Alias alias : aliasReservation.getAliases()) {
                if (alias.getTechnology().isCompatibleWith(technologies)) {
                    roomEndpoint.addAssignedAlias(alias);
                }
            }
            aliasReservation.setExecutable(roomEndpoint);
        }

        return aliasReservation;
    }
}