package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
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
    protected Reservation allocateReservation(Reservation allocatedReservation) throws SchedulerException
    {
        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

        // Get possible alias providers
        Collection<AliasProviderCapability> aliasProviderCapabilities;
        if (this.aliasProviderCapabilities.size() > 0) {
            // Use only specified alias providers
            aliasProviderCapabilities = this.aliasProviderCapabilities;
        }
        else {
            // Use all alias providers from the cache
            aliasProviderCapabilities = cache.getAliasProviders();
        }

        // Find matching alias providers
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        List<AliasProvider> aliasProviders = new LinkedList<AliasProvider>();
        for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
            // Check whether alias provider matches the criteria
            if (aliasProviderCapability.isRestrictedToResource() && targetResource != null) {
                // Skip alias providers which cannot be used for specified target resource
                if (!aliasProviderCapability.getResource().getId().equals(targetResource.getId())) {
                    continue;
                }
            }
            if (technologies.size() > 0 && !aliasProviderCapability.providesAliasTechnology(technologies)) {
                continue;
            }
            if (aliasTypes.size() > 0 && !aliasProviderCapability.providesAliasType(aliasTypes)) {
                continue;
            }

            // Initialize alias provider
            AliasProvider aliasProvider = new AliasProvider(aliasProviderCapability);

            // Set value for alias provider
            String value = this.value;
            if (value != null) {
                value = aliasProviderCapability.parseValue(value);
                aliasProvider.setValue(value);
            }

            // Get available alias reservations for alias provider
            for (AvailableReservation<AliasReservation> availableAliasReservation :
                    schedulerContext.getAvailableAliasReservations(aliasProviderCapability)) {
                if (value != null && !availableAliasReservation.getTargetReservation().getValue().equals(value)) {
                    continue;
                }
                if (allocatedReservation != null) {
                    if (availableAliasReservation.getOriginalReservation().equals(allocatedReservation)) {
                        aliasProvider.setAllocated(true);
                    }
                }
                aliasProvider.addAvailableAliasReservation(availableAliasReservation);
            }
            sortAvailableReservations(aliasProvider.getAvailableAliasReservations(), allocatedReservation);

            // Add alias provider
            aliasProviders.add(aliasProvider);

            addReport(new SchedulerReportSet.ResourceReport(aliasProviderCapability.getResource()));
        }
        if (aliasProviders.size() == 0) {
            throw new SchedulerException(getCurrentReport());
        }
        endReport();

        // Sort alias providers
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(aliasProviders, new Comparator<AliasProvider>()
        {
            @Override
            public int compare(AliasProvider first, AliasProvider second)
            {
                // Prefer alias provider which is already allocated by allocatedReservation
                if (second.isAllocated()) {
                    return 1;
                }

                // Prefer alias providers which has reallocatable available alias reservation
                boolean firstReallocatable = first.hasReallocatableAvailableAliasReservation();
                boolean secondReallocatable = second.hasReallocatableAvailableAliasReservation();
                if (!firstReallocatable && secondReallocatable) {
                    return 1;
                }

                // If target resource for which the alias will be used is not specified,
                if (targetResource == null) {
                    // Prefer alias providers which doesn't restrict target resource
                    boolean firstRestricted = first.aliasProviderCapability.isRestrictedToResource();
                    boolean secondRestricted = second.aliasProviderCapability.isRestrictedToResource();
                    if (firstRestricted && !secondRestricted) {
                        return 1;
                    }
                }
                return 0;
            }
        });

        // Allocate alias reservation in some matching alias provider
        for (AliasProvider aliasProvider : aliasProviders) {
            AliasProviderCapability aliasProviderCapability = aliasProvider.getAliasProviderCapability();
            beginReport(new SchedulerReportSet.AllocatingResourceReport(aliasProviderCapability.getResource()));

            // Preferably use available alias reservation
            for (AvailableReservation<AliasReservation> availableAliasReservation :
                    aliasProvider.getAvailableAliasReservations()) {
                // Check available alias reservation
                Reservation originalReservation = availableAliasReservation.getOriginalReservation();

                // Original reservation slot must contain requested slot
                if (!originalReservation.getSlot().contains(interval)) {
                    // Original reservation slot doesn't contain the requested
                    continue;
                }

                // Available reservation will be returned so remove it from context (to not be used again)
                schedulerContext.removeAvailableReservation(availableAliasReservation);

                // Return available reservation
                if (availableAliasReservation.isExistingReservationRequired()) {
                    addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(interval);
                    existingReservation.setReservation(originalReservation);
                    return existingReservation;
                }
                else {
                    addReport(new SchedulerReportSet.ReservationReallocatingReport(originalReservation));
                    return originalReservation;
                }
            }

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(aliasProviderCapability, schedulerContext);
            }
            catch (SchedulerException exception) {
                endReportError(exception.getReport());
                continue;
            }

            // Get new available value
            SchedulerContext.Savepoint schedulerContextSavepoint = schedulerContext.createSavepoint();
            try {
                ValueReservationTask valueReservationTask = new ValueReservationTask(schedulerContext,
                        aliasProviderCapability.getValueProvider(), aliasProvider.getValue());
                ValueReservation valueReservation = addChildReservation(valueReservationTask, ValueReservation.class);

                // Allocate reservation
                AliasReservation aliasReservation;
                if (allocatedReservation != null && allocatedReservation instanceof AliasReservation) {
                    // Reallocate existing alias reservation
                    addReport(new SchedulerReportSet.ReservationReallocatingReport(allocatedReservation));
                    aliasReservation = (AliasReservation) allocatedReservation;
                    aliasReservation.clearChildReservations();
                }
                else {
                    // Find empty available alias reservation (without child reservations)
                    aliasReservation = popEmptyAvailableReservation(AliasReservation.class);
                    if (aliasReservation != null) {
                        addReport(new SchedulerReportSet.ReservationReallocatingReport(aliasReservation));
                    }
                    // Else create new alias reservation
                    else {
                        aliasReservation = new AliasReservation();
                    }
                }
                aliasReservation.setSlot(interval);
                aliasReservation.setAliasProviderCapability(aliasProviderCapability);
                aliasReservation.setValueReservation(valueReservation);

                // If alias should be allocated as permanent room, create room endpoint with zero licenses
                // (so we don't need reservation for the room).
                // The permanent room should not be created if the alias will be used for any specified target resource.
                if (aliasProviderCapability.isPermanentRoom() && schedulerContext.isExecutableAllowed()
                        && targetResource == null) {
                    Resource resource = aliasProviderCapability.getResource();
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

                endReport();
                return aliasReservation;
            }
            catch (SchedulerException exception) {
                schedulerContextSavepoint.revert();

                // End allocating current alias provider and try to allocate next one
                endReport();
            }
            finally {
                schedulerContextSavepoint.destroy();
            }
        }
        throw new SchedulerException(getCurrentReport());
    }

    /**
     * Represents {@link AliasProviderCapability} which can be allocated by the {@link AliasReservationTask}.
     */
    private static class AliasProvider
    {
        /**
         * @see AliasProviderCapability
         */
        private AliasProviderCapability aliasProviderCapability;

        /**
         * Value which is requested for allocation.
         */
        private String value = null;

        /**
         * List of {@link AvailableReservation}s ({@link AliasReservation}s).
         */
        private List<AvailableReservation<AliasReservation>> availableAliasReservations =
                new LinkedList<AvailableReservation<AliasReservation>>();

        /**
         * Specifies whether already allocated reservation for the {@link #aliasProviderCapability} was specified
         * to the reservation task.
         */
        private boolean allocated = false;

        /**
         * Specifies whether {@link AvailableReservation.Type#REALLOCATABLE} {@link AvailableReservation} was specified
         * for the {@link #aliasProviderCapability}.
         */
        private boolean reallocatableAvailableAliasReservation = false;

        /**
         * Constructor.
         *
         * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
         */
        public AliasProvider(AliasProviderCapability aliasProviderCapability)
        {
            this.aliasProviderCapability = aliasProviderCapability;
        }

        /**
         * @return {@link #aliasProviderCapability}
         */
        public AliasProviderCapability getAliasProviderCapability()
        {
            return aliasProviderCapability;
        }

        /**
         * @return {@link #value}
         */
        public String getValue()
        {
            return value;
        }

        /**
         * @param value sets the {@link #value}
         */
        public void setValue(String value)
        {
            this.value = value;
        }

        /**
         * @return {@link #availableAliasReservations}
         */
        public List<AvailableReservation<AliasReservation>> getAvailableAliasReservations()
        {
            return availableAliasReservations;
        }

        /**
         * @param availableAliasReservation to be added to the {@link #availableAliasReservations}
         */
        public void addAvailableAliasReservation(AvailableReservation<AliasReservation> availableAliasReservation)
        {
            availableAliasReservations.add(availableAliasReservation);
            if (availableAliasReservation.getType().equals(AvailableReservation.Type.REALLOCATABLE)) {
                reallocatableAvailableAliasReservation = true;
            }
        }

        /**
         * @return {@link #allocated}
         */
        private boolean isAllocated()
        {
            return allocated;
        }

        /**
         * @param allocated sets the {@link #allocated}
         */
        private void setAllocated(boolean allocated)
        {
            this.allocated = allocated;
        }

        /**
         * @return {@link #reallocatableAvailableAliasReservation}
         */
        public boolean hasReallocatableAvailableAliasReservation()
        {
            return reallocatableAvailableAliasReservation;
        }
    }
}