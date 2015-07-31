package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservationTask;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.scheduler.*;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for one or multiple {@link AliasReservation}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasReservationTask extends ReservationTask
{
    /**
     * Restricts {@link AliasType} for allocation of {@link cz.cesnet.shongo.controller.booking.alias.Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} for allocation of {@link cz.cesnet.shongo.controller.booking.alias.Alias}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Requested {@link String} value for the {@link cz.cesnet.shongo.controller.booking.alias.Alias}.
     */
    private String requestedValue;

    /**
     * {@link DeviceResource} for which the {@link AliasReservation} is being allocated and to which it
     * will be assigned.
     */
    private DeviceResource targetResource;

    /**
     * {@link cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability}s for which the {@link AliasReservation} can be allocated.
     */
    private List<AliasProviderCapability> aliasProviderCapabilities = new ArrayList<AliasProviderCapability>();

    /**
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     * @param slot             sets the {@link #slot}
     */
    public AliasReservationTask(SchedulerContext schedulerContext, Interval slot)
    {
        super(schedulerContext, slot);
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
     * @param requestedValue sets the {@link #requestedValue}
     */
    public void setRequestedValue(String requestedValue)
    {
        this.requestedValue = requestedValue;
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
        return new SchedulerReportSet.AllocatingAliasReport(technologies, aliasTypes, requestedValue);
    }

    @Override
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
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
            aliasProviderCapabilities = resourceCache.getCapabilities(AliasProviderCapability.class);
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
            if (this.requestedValue != null) {
                aliasProvider.setRequestedValue(aliasProviderCapability.parseValue(requestedValue));
            }

            // Get available alias reservations for alias provider
            for (AvailableReservation<AliasReservation> availableAliasReservation :
                    schedulerContextState.getAvailableAliasReservations(aliasProviderCapability, this.slot)) {
                aliasProvider.addAvailableAliasReservation(availableAliasReservation);
            }
            sortAvailableReservations(aliasProvider.getAvailableAliasReservations());

            // Add alias provider
            aliasProviders.add(aliasProvider);

            addReport(new SchedulerReportSet.ResourceReport(aliasProviderCapability.getResource()));
        }
        if (aliasProviders.size() == 0) {
            throw new SchedulerReportSet.ResourceNotFoundException();
        }
        endReport();

        // Sort alias providers
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(aliasProviders, new Comparator<AliasProvider>()
        {
            @Override
            public int compare(AliasProvider first, AliasProvider second)
            {
                // If target resource for which the alias will be used is not specified,
                if (targetResource == null) {
                    // Prefer alias providers which doesn't restrict target resource
                    boolean firstRestricted = first.aliasProviderCapability.isRestrictedToResource();
                    boolean secondRestricted = second.aliasProviderCapability.isRestrictedToResource();
                    if (firstRestricted && !secondRestricted) {
                        return 1;
                    }
                    else if (!firstRestricted && secondRestricted) {
                        return -1;
                    }
                }
                return 0;
            }
        });

        // Allocate alias reservation in some matching alias provider
        for (AliasProvider aliasProvider : aliasProviders) {
            AliasProviderCapability aliasProviderCapability = aliasProvider.getAliasProviderCapability();
            String requestedValue = aliasProvider.getRequestedValue();
            beginReport(new SchedulerReportSet.AllocatingResourceReport(aliasProviderCapability.getResource()));

            // Preferably use available alias reservation
            for (AvailableReservation<AliasReservation> availableAliasReservation :
                    aliasProvider.getAvailableAliasReservations()) {
                // Check available alias reservation
                Reservation originalReservation = availableAliasReservation.getOriginalReservation();
                AliasReservation aliasReservation = availableAliasReservation.getTargetReservation();

                // Only reusable available reservations
                if (!availableAliasReservation.isType(AvailableReservation.Type.REUSABLE)) {
                    continue;
                }

                // Original reservation slot must contain requested slot
                if (!originalReservation.getSlot().contains(slot)) {
                    continue;
                }

                // Value must match requested value
                if (requestedValue != null && !aliasReservation.getValue().equals(requestedValue)) {
                    continue;
                }

                // Available reservation will be returned so remove it from context (to not be used again)
                schedulerContextState.removeAvailableReservation(availableAliasReservation);

                // Create new existing alias reservation
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                ExistingReservation existingValueReservation = new ExistingReservation();
                existingValueReservation.setSlot(slot);
                existingValueReservation.setReusedReservation(originalReservation);
                return existingValueReservation;
            }

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(aliasProviderCapability, this.slot, schedulerContext, this);
            }
            catch (SchedulerException exception) {
                endReportError(exception.getReport());
                continue;
            }

            // Get new available value
            SchedulerContextState.Savepoint schedulerContextSavepoint = schedulerContextState.createSavepoint();
            try {
                ValueReservationTask valueReservationTask = new ValueReservationTask(schedulerContext, this.slot,
                        aliasProviderCapability.getValueProvider(), aliasProvider.getRequestedValue());
                ValueReservation valueReservation = addChildReservation(valueReservationTask, ValueReservation.class);

                // Create new alias reservation
                AliasReservation aliasReservation = new AliasReservation();
                aliasReservation.setSlot(slot);
                aliasReservation.setAliasProviderCapability(aliasProviderCapability);
                aliasReservation.setValueReservation(valueReservation);

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
        private String requestedValue = null;

        /**
         * List of {@link AvailableReservation}s ({@link AliasReservation}s).
         */
        private List<AvailableReservation<AliasReservation>> availableAliasReservations =
                new LinkedList<AvailableReservation<AliasReservation>>();

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
         * @return {@link #requestedValue}
         */
        public String getRequestedValue()
        {
            return requestedValue;
        }

        /**
         * @param requestedValue sets the {@link #requestedValue}
         */
        public void setRequestedValue(String requestedValue)
        {
            this.requestedValue = requestedValue;
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
        }
    }
}