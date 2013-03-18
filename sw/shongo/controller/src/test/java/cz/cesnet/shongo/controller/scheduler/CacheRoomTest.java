package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.cache.RoomCache;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.*;

import static junitx.framework.Assert.assertEquals;

/**
 * Tests for caching of virtual rooms by {@link Cache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CacheRoomTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        // -----------------------------------------------------
        // Create two MCUs and allocate some virtual rooms on it
        // -----------------------------------------------------
        Cache cache = Cache.createTestingCache();

        DeviceResource mcu1 = new DeviceResource();
        mcu1.setName("mcu1");
        mcu1.addTechnology(Technology.H323);
        mcu1.addTechnology(Technology.ADOBE_CONNECT);
        mcu1.addCapability(new RoomProviderCapability(50));
        mcu1.setAllocatable(true);
        cache.addResource(mcu1);

        RoomReservation room1 = new RoomReservation();
        room1.setUserId(Authorization.ROOT_USER_ID);
        room1.setRoomProviderCapability(mcu1.getCapability(RoomProviderCapability.class));
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.getRoomConfiguration().setLicenseCount(25);
        cache.addReservation(room1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu2");
        mcu2.addTechnology(Technology.SIP);
        mcu2.addTechnology(Technology.ADOBE_CONNECT);
        mcu2.addCapability(new RoomProviderCapability(100));
        mcu2.setAllocatable(true);
        cache.addResource(mcu2);

        RoomReservation room2 = new RoomReservation();
        room2.setUserId(Authorization.ROOT_USER_ID);
        room2.setRoomProviderCapability(mcu2.getCapability(RoomProviderCapability.class));
        room2.setSlot(DateTime.parse("50"), DateTime.parse("150"));
        room2.getRoomConfiguration().setLicenseCount(50);
        cache.addReservation(room2);

        RoomReservation room3 = new RoomReservation();
        room3.setUserId(Authorization.ROOT_USER_ID);
        room3.setRoomProviderCapability(mcu2.getCapability(RoomProviderCapability.class));
        room3.setSlot(DateTime.parse("100"), DateTime.parse("200"));
        room3.getRoomConfiguration().setLicenseCount(30);
        cache.addReservation(room3);

        // ---------------------------------
        // Test find available virtual rooms
        // ---------------------------------
        List<AvailableRoom> result;

        // Test different intervals
        result = findAvailableRooms(cache, Interval.parse("0/1"), 50);
        assertEquals(2, result.size());

        result = findAvailableRooms(cache, Interval.parse("200/250"), 50);
        assertEquals(2, result.size());

        result = findAvailableRooms(cache, Interval.parse("50/100"), 50);
        assertEquals(1, result.size());

        result = findAvailableRooms(cache, Interval.parse("100/150"), 50);
        assertEquals(1, result.size());

        // Test different technologies
        result = findAvailableRooms(cache, Interval.parse("100/149"), 10,
                new Technology[]{Technology.H323, Technology.ADOBE_CONNECT});
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());

        result = findAvailableRooms(cache, Interval.parse("100/149"), 10,
                new Technology[]{Technology.SIP, Technology.ADOBE_CONNECT});
        assertEquals(1, result.size());
        assertEquals(mcu2, result.get(0).getDeviceResource());

        // Test different number of required ports
        result = findAvailableRooms(cache, Interval.parse("100/149"), 10,
                new Technology[]{Technology.ADOBE_CONNECT});
        sortResult(result);
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = findAvailableRooms(cache, Interval.parse("100/149"), 20,
                new Technology[]{Technology.ADOBE_CONNECT});
        sortResult(result);
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = findAvailableRooms(cache, Interval.parse("100/149"), 21,
                new Technology[]{Technology.ADOBE_CONNECT});
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
    }

    /**
     * Find {@link cz.cesnet.shongo.controller.cache.AvailableRoom}s in given {@code interval} which have
     * at least {@code requiredLicenseCount} available licenses and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredLicenseCount
     * @param technologies
     * @return list of {@link cz.cesnet.shongo.controller.cache.AvailableRoom}
     */
    public List<AvailableRoom> findAvailableRooms(Cache cache, Interval interval,
            int requiredLicenseCount, Set<Technology> technologies)
    {
        RoomCache roomCache = cache.getRoomCache();
        List<AvailableRoom> availableRooms = new ArrayList<AvailableRoom>();
        for (RoomProviderCapability roomProviderCapability : roomCache.getRoomProviders(technologies)) {
            AvailableRoom availableRoom = roomCache.getAvailableRoom(
                    roomProviderCapability,
                    new ReservationTask.Context(cache, interval));
            if (availableRoom.getAvailableLicenseCount() >= requiredLicenseCount) {
                availableRooms.add(availableRoom);
            }
        }
        return availableRooms;
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    public List<AvailableRoom> findAvailableRooms(Cache cache, Interval interval,
            int requiredLicenseCount, Technology[] technologies)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableRooms(cache, interval, requiredLicenseCount, technologySet);
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    public List<AvailableRoom> findAvailableRooms(Cache cache, Interval interval,
            int requiredLicenseCount)
    {
        return findAvailableRooms(cache, interval, requiredLicenseCount, (Set<Technology>) null);
    }

    private void sortResult(List<AvailableRoom> result)
    {
        Collections.sort(result, new Comparator<AvailableRoom>()
        {
            @Override
            public int compare(AvailableRoom o1, AvailableRoom o2)
            {
                return o1.getDeviceResource().getId().compareTo(o2.getDeviceResource().getId());
            }
        });
    }
}
