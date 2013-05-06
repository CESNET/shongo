package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
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
     * @param schedulerContext sets the {@link #schedulerContext}
     */
    public AliasReservationTask(SchedulerContext schedulerContext)
    {
        super(schedulerContext);
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
    protected SchedulerReport createMainReport()
    {
        return new SchedulerReportSet.AllocatingAliasReport(technologies, aliasTypes, value);
    }

    @Override
    protected Reservation createReservation() throws SchedulerException
    {
        SchedulerContext schedulerContext = getSchedulerContext();
        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

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
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
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
            addReport(new SchedulerReportSet.ResourceReport(aliasProvider.getResource()));
        }
        if (aliasProviders.size() == 0) {
            throw new SchedulerException(getCurrentReport());
        }
        endReport();

        // Build map of available alias reservation by alias provider
        final Map<AliasProviderCapability, AvailableReservation<AliasReservation>> availableAliasByAliasProvider =
                new HashMap<AliasProviderCapability, AvailableReservation<AliasReservation>>();
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            Collection<AvailableReservation<AliasReservation>> availableReservations =
                    schedulerContext.getAvailableAliasReservations(aliasProvider);
            if (availableReservations.size() > 0) {
                AvailableReservation<AliasReservation> availableAliasReservation = null;
                String value = valueByAliasProvider.get(aliasProvider);
                if (value != null) {
                    for (AvailableReservation<AliasReservation> possibleAvailableReservation : availableReservations) {
                        if (possibleAvailableReservation.getTargetReservation().getValue().equals(value)) {
                            availableAliasReservation = possibleAvailableReservation;
                            break;
                        }
                    }
                }
                else {
                    availableAliasReservation = availableReservations.iterator().next();
                }
                if (availableAliasReservation != null) {
                    availableAliasByAliasProvider.put(aliasProvider, availableAliasReservation);
                }
            }
        }

        // Sort alias providers, prefer the ones with provided aliases
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(aliasProviders, new Comparator<AliasProviderCapability>()
        {
            @Override
            public int compare(AliasProviderCapability provider1, AliasProviderCapability provider2)
            {
                boolean firstHasProvidedAlias = availableAliasByAliasProvider.containsKey(provider1);
                boolean secondHasProvidedAlias = availableAliasByAliasProvider.containsKey(provider2);
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
            beginReport(new SchedulerReportSet.AllocatingResourceReport(aliasProvider.getResource()));

            // Preferably reuse provided alias reservation
            AvailableReservation<AliasReservation> availableAliasReservation = availableAliasByAliasProvider.get(aliasProvider);
            if (availableAliasReservation != null) {
                Reservation originalReservation = availableAliasReservation.getOriginalReservation();
                if (availableAliasReservation.getType().equals(AvailableReservation.Type.REALLOCATABLE)) {
                    throw new TodoImplementException("reallocate alias");
                }
                else {
                    addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(interval);
                    existingReservation.setReservation(originalReservation);
                    schedulerContext.removeAvailableReservation(availableAliasReservation);
                    return existingReservation;
                }
            }

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(aliasProvider, schedulerContext);
            }
            catch (SchedulerException exception) {
                endReportError(exception.getReport());
                continue;
            }

            // Get new available value
            SchedulerContext.Savepoint schedulerContextSavepoint = schedulerContext.createSavepoint();
            try {
                String value = valueByAliasProvider.get(aliasProvider);
                ValueReservationTask valueReservationTask =
                        new ValueReservationTask(schedulerContext, aliasProvider.getValueProvider(), value);
                availableValueReservation = addChildReservation(valueReservationTask, ValueReservation.class);
            }
            catch (SchedulerException exception) {
                schedulerContextSavepoint.revert();

                // End allocating current alias provider and try to allocate next one
                endReport();
                continue;
            }
            finally {
                schedulerContextSavepoint.destroy();
            }

            availableAliasProvider = aliasProvider;
            endReport();
            break;
        }
        if (availableValueReservation == null) {
            throw new SchedulerException(getCurrentReport());
        }

        // Create new reservation
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(interval);
        aliasReservation.setAliasProviderCapability(availableAliasProvider);
        aliasReservation.setValueReservation(availableValueReservation);

        // If alias should be allocated as permanent room, create room endpoint with zero licenses
        // (so we don't need reservation for the room).
        // The permanent room should not be created if the alias will be used for any specified target resource.
        if (availableAliasProvider.isPermanentRoom() && schedulerContext.isExecutableAllowed() && targetResource == null) {
            Resource resource = availableAliasProvider.getResource();
            RoomProviderCapability roomProvider = resource.getCapability(RoomProviderCapability.class);
            if (roomProvider == null) {
                throw new RuntimeException("Permanent room should be enabled only for device resource"
                        + " with room provider capability.");
            }
            ResourceRoomEndpoint roomEndpoint = new ResourceRoomEndpoint();
            roomEndpoint.setSlot(interval);
            roomEndpoint.setRoomProviderCapability(roomProvider);
            roomEndpoint.setRoomDescription(schedulerContext.getReservationDescription());
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