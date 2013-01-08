package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AliasCache;
import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (aliasProviderCapabilities.size() > 0) {
            // Allocate alias from one specified aliasProviderCapability
            for (AliasProviderCapability aliasProvider : aliasProviderCapabilities) {
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
        }
        else {
            // Allocate alias from all resources in the cache
            for (AliasProviderCapability aliasProvider : aliasCache.getObjects()) {
                if (aliasProvider.isRestrictedToOwnerResource()) {
                    if (targetResource == null) {
                        continue;
                    }
                    if(!aliasProvider.getResource().getId().equals(targetResource.getId())) {
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

        // Create new reservation
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(getInterval());
        aliasReservation.setAliasProviderCapability(availableAlias.getAliasProviderCapability());
        aliasReservation.setAliasValue(availableAlias.getAliasValue());
        return aliasReservation;
    }
}
