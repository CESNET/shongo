package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
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
     * Requests allocation of {@link Alias}es for each {@link AliasType}s.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} of the {@link AliasType}s or if {@link #aliasTypes} is empty it requests
     * allocation of {@link Alias}es for each {@link Technology}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link DeviceResource} for which the {@link AliasReservation} is being allocated and to which it
     * will be assigned.
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
    protected Reservation createReservation() throws ReportException
    {
        Interval interval = getInterval();
        Cache cache = getCache();
        Cache.Transaction cacheTransaction = getCacheTransaction();

        // Get alias providers
        Collection<AliasProviderCapability> aliasProviders;
        if (aliasProviderCapabilities.size() > 0) {
            // Use only specified alias providers
            aliasProviders = aliasProviderCapabilities;
        }
        else {
            // Use all alias providers from the cache
            aliasProviders = cache.getAliasProviders();
        }

        // If target resource for which the alias will be used is not specified,
        // we should prefer alias providers which doesn't restrict target resource
        if (targetResource == null) {
            // Sort the alias providers in the way that the ones which restricts target resource are at the end
            List<AliasProviderCapability> aliasProviderList =
                    new ArrayList<AliasProviderCapability>(aliasProviders);
            Collections.sort(aliasProviderList, new Comparator<AliasProviderCapability>()
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
            aliasProviders = aliasProviderList;
        }

        // Find available value in the alias providers
        Reservation availableValueReservation = null;
        AliasProviderCapability availableAliasProvider = null;
        for (AliasProviderCapability aliasProvider : aliasProviders) {
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

            // Preferably reuse provided alias reservation
            Collection<AliasReservation> providedAliasReservations =
                    cacheTransaction.getProvidedAliasReservations(aliasProvider);
            if (providedAliasReservations.size() > 0) {
                AliasReservation providedAliasReservation = null;
                if (value != null) {
                    for (AliasReservation possibleProvidedAliasReservation : providedAliasReservations) {
                        if (possibleProvidedAliasReservation.getValue().equals(value)) {
                            providedAliasReservation = possibleProvidedAliasReservation;
                        }
                    }
                }
                else {
                    providedAliasReservation = providedAliasReservations.iterator().next();
                }
                // Reuse existing alias reservation
                if (providedAliasReservation != null) {
                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(getInterval());
                    existingReservation.setReservation(providedAliasReservation);
                    cacheTransaction.removeProvidedReservation(providedAliasReservation);
                    return existingReservation;
                }
            }

            // Check whether alias provider can be allocated
            Resource resource = aliasProvider.getResource();
            DateTime referenceDateTime = cache.getReferenceDateTime();
            if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), referenceDateTime)) {
                continue;
            }

            // Get new available value
            Reservation valueReservation = ValueReservationTask.createReservation(
                    aliasProvider.getValueProvider(), value, interval, cache.getValueCache(), cacheTransaction);
            if (valueReservation != null) {
                availableValueReservation = valueReservation;
                availableAliasProvider = aliasProvider;
                break;
            }
        }
        if (availableValueReservation == null) {
            throw new NoAvailableAliasReport(technologies, aliasTypes, value).exception();
        }

        ValueReservation valueReservation = addChildReservation(availableValueReservation, ValueReservation.class);

        // Create new reservation
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(getInterval());
        aliasReservation.setAliasProviderCapability(availableAliasProvider);
        aliasReservation.setValueReservation(valueReservation);

        // If alias should be allocated as permanent room, create room endpoint with zero licenses
        // (so we don't need reservation for the room).
        // The permanent room should not be created if the alias will be used for any specified target resource.
        if (availableAliasProvider.isPermanentRoom() && targetResource == null) {
            Resource resource = availableAliasProvider.getResource();
            if (!resource.hasCapability(RoomProviderCapability.class)) {
                throw new IllegalStateException("Permanent room should be enabled only for device resource"
                        + " with room provider capability.");
            }
            ResourceRoomEndpoint roomEndpoint = new ResourceRoomEndpoint();
            roomEndpoint.setUserId(getContext().getUserId());
            roomEndpoint.setSlot(getInterval());
            roomEndpoint.setDeviceResource((DeviceResource) resource);
            roomEndpoint.setRoomDescription(getContext().getReservationRequest().getDescription());
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
