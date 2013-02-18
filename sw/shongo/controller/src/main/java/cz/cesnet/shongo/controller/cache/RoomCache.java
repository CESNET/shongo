package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a cache of {@link RoomReservation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomCache extends AbstractReservationCache<RoomProviderCapability, RoomReservation>
{
    private static Logger logger = LoggerFactory.getLogger(RoomCache.class);

    /**
     * @see ResourceCache
     */
    private ResourceCache resourceCache;

    /**
     * Constructor.
     *
     * @param resourceCache sets the {@link #resourceCache}
     */
    public RoomCache(ResourceCache resourceCache)
    {
        this.resourceCache = resourceCache;
    }

    /**
     * Map of {@link RoomProviderCapability} by {@link Resource#getId()}.
     */
    private Map<Long, RoomProviderCapability> roomProviderByResourceId = new HashMap<Long, RoomProviderCapability>();

    @Override
    public void addObject(RoomProviderCapability object, EntityManager entityManager)
    {
        roomProviderByResourceId.put(object.getResource().getId(), object);

        super.addObject(object, entityManager);
    }

    @Override
    protected void updateObjectState(RoomProviderCapability object, Interval workingInterval,
            EntityManager entityManager)
    {
        // Get all allocated values for the value provider and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<RoomReservation> roomReservations = resourceManager.listRoomReservationsInInterval(object.getId(),
                getWorkingInterval());
        for (RoomReservation roomReservation : roomReservations) {
            addReservation(object, roomReservation);
        }
    }

    /**
     * Remove room provider from given {@code resource}.
     *
     * @param resource from which the room provider comes from
     */
    public void removeRoomProviderCapability(Resource resource)
    {
        RoomProviderCapability roomProviderCapability = roomProviderByResourceId.get(resource.getId());
        if (roomProviderCapability != null) {
            removeObject(roomProviderCapability);
        }
    }

    @Override
    public void clear()
    {
        roomProviderByResourceId.clear();
        super.clear();
    }

    /**
     * @param roomProviderCapability
     * @param context
     * @return {@link AvailableRoom} for given {@code roomProviderCapability} in given {@code interval}
     */
    public AvailableRoom getAvailableRoom(RoomProviderCapability roomProviderCapability,
            ReservationTask.Context context)
    {
        int usedLicenseCount = 0;
        if (resourceCache.isResourceAvailable(roomProviderCapability.getResource(), context)) {
            ObjectState<RoomReservation> roomProviderState = getObjectState(roomProviderCapability);
            Set<RoomReservation> roomReservations = roomProviderState.getReservations(context.getInterval(),
                    context.getCacheTransaction().getRoomCacheTransaction());
            for (RoomReservation roomReservation : roomReservations) {
                usedLicenseCount += roomReservation.getRoomConfiguration().getLicenseCount();
            }
        }
        else {
            usedLicenseCount = roomProviderCapability.getLicenseCount();
        }
        AvailableRoom availableRoom = new AvailableRoom();
        availableRoom.setRoomProviderCapability(roomProviderCapability);
        availableRoom.setMaximumLicenseCount(roomProviderCapability.getLicenseCount());
        availableRoom.setAvailableLicenseCount(roomProviderCapability.getLicenseCount() - usedLicenseCount);
        return availableRoom;
    }

    /**
     * @param roomProvider
     * @param interval
     * @param cacheTransaction
     * @return collection of {@link RoomReservation}s for given {@code roomProviderCapability} in given
     *         {@code interval} and {@code cacheTransaction}
     */
    public Collection<RoomReservation> getRoomReservations(RoomProviderCapability roomProvider, Interval interval,
            CacheTransaction cacheTransaction)
    {
        ObjectState<RoomReservation> roomProviderState = getObjectState(roomProvider);
        return roomProviderState.getReservations(interval, cacheTransaction.getRoomCacheTransaction());
    }

    /**
     * @param technologies to be lookup-ed
     * @return list of {@link RoomProviderCapability}s which supports given {@code technologies}
     */
    public Collection<RoomProviderCapability> getRoomProviders(Set<Technology> technologies)
    {
        Set<RoomProviderCapability> roomProviders = new HashSet<RoomProviderCapability>();
        for (RoomProviderCapability roomProvider : getObjects()) {
            if (technologies == null || roomProvider.getDeviceResource().hasTechnologies(technologies)) {
                roomProviders.add(roomProvider);
            }
        }
        return roomProviders;
    }
}
