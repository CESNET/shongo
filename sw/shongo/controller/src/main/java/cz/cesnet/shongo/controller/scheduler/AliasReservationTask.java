package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AliasCache;
import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link AliasReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasReservationTask extends ReservationTask
{
    /**
     * {@link Technology}s which the allocated {@link AliasReservation} must provide.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * {@link cz.cesnet.shongo.AliasType} for the {@link AliasReservation}.
     */
    private AliasType aliasType;

    /**
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link DeviceResource} for which the {@link AliasReservation} is being allocated.
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
        this.technologies.add(technology);
    }

    /**
     * @param aliasType sets the {@link #aliasType}
     */
    public void setAliasType(AliasType aliasType)
    {
        this.aliasType = aliasType;
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
        Cache.Transaction cacheTransaction = getCacheTransaction();
        AvailableAlias availableAlias = null;

        Interval interval = getInterval();
        AliasCache aliasCache = getCache().getAliasCache();
        AliasCache.Transaction aliasCacheTransaction = getCacheTransaction().getAliasCacheTransaction();

        // Get alias providers
        Collection<AliasProviderCapability> aliasProviders;
        if (aliasProviderCapabilities.size() > 0) {
            // Use only specified alias providers
            aliasProviders = aliasProviderCapabilities;
        }
        else {
            // Use all alias providers from the cache
            aliasProviders = aliasCache.getObjects();
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

        // Find available alias in the alias providers
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            if (aliasProvider.isRestrictedToResource() && targetResource != null) {
                // Skip alias providers which cannot be used for specified target resource
                if (!aliasProvider.getResource().getId().equals(targetResource.getId())) {
                    continue;
                }
            }
            if (technologies.size() > 0 && !aliasProvider.providesAliasTechnologies(technologies)) {
                continue;
            }
            if (aliasType != null && !aliasProvider.providesAliasType(aliasType)) {
                continue;
            }
            availableAlias = aliasCache.getAvailableAlias(aliasProvider, value, interval, aliasCacheTransaction);
            if (availableAlias != null) {
                break;
            }
        }
        if (availableAlias == null) {
            throw new NoAvailableAliasReport(technologies, aliasType).exception();
        }

        // Reuse existing reservation
        AliasReservation providedAliasReservation = availableAlias.getAliasReservation();
        if (providedAliasReservation != null) {
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(getInterval());
            existingReservation.setReservation(providedAliasReservation);
            cacheTransaction.removeProvidedReservation(providedAliasReservation);
            return existingReservation;
        }

        AliasProviderCapability aliasProviderCapability = availableAlias.getAliasProviderCapability();

        // Create new reservation
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(getInterval());
        aliasReservation.setAliasProviderCapability(aliasProviderCapability);
        aliasReservation.setAliasValue(availableAlias.getAliasValue());

        // If alias should be allocated as permanent room, create room endpoint with zero licenses
        // (so we don't need reservation for the room)
        if (aliasProviderCapability.isPermanentRoom()) {
            Resource resource = aliasProviderCapability.getResource();
            if (!resource.hasCapability(RoomProviderCapability.class)) {
                throw new IllegalStateException("Permanent room should be enabled only for device resource"
                        + " with room provider capability.");
            }
            ResourceRoomEndpoint roomEndpoint = new ResourceRoomEndpoint();
            roomEndpoint.setUserId(getContext().getUserId());
            roomEndpoint.setDeviceResource((DeviceResource) resource);
            roomEndpoint.setRoomName(getContext().getReservationRequest().getName());
            roomEndpoint.setSlot(getInterval());
            roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
            Set<Technology> technologies = roomEndpoint.getTechnologies();
            for (Alias alias : aliasReservation.getAliases()) {
                if (technologies.contains(alias.getTechnology())) {
                    roomEndpoint.addAssignedAlias(alias);
                }
            }
            aliasReservation.setExecutable(roomEndpoint);
        }
        return aliasReservation;
    }
}
