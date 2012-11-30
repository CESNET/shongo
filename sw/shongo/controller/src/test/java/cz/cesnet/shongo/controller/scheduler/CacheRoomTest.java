package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.List;

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
        room1.setResource(mcu1);
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.getRoomConfiguration().setLicenseCount(25);
        cache.addReservation(room1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu1");
        mcu2.addTechnology(Technology.SIP);
        mcu2.addTechnology(Technology.ADOBE_CONNECT);
        mcu2.addCapability(new RoomProviderCapability(100));
        mcu2.setAllocatable(true);
        cache.addResource(mcu2);

        RoomReservation room2 = new RoomReservation();
        room2.setResource(mcu2);
        room2.setSlot(DateTime.parse("50"), DateTime.parse("150"));
        room2.getRoomConfiguration().setLicenseCount(50);
        cache.addReservation(room2);

        RoomReservation room3 = new RoomReservation();
        room3.setResource(mcu2);
        room3.setSlot(DateTime.parse("100"), DateTime.parse("200"));
        room3.getRoomConfiguration().setLicenseCount(30);
        cache.addReservation(room3);

        // ---------------------------------
        // Test find available virtual rooms
        // ---------------------------------
        List<AvailableRoom> result;

        // Test different intervals
        result = cache.findAvailableRooms(Interval.parse("0/1"), 50, null);
        assertEquals(2, result.size());

        result = cache.findAvailableRooms(Interval.parse("200/250"), 50, null);
        assertEquals(2, result.size());

        result = cache.findAvailableRooms(Interval.parse("50/100"), 50, null);
        assertEquals(1, result.size());

        result = cache.findAvailableRooms(Interval.parse("100/150"), 50, null);
        assertEquals(1, result.size());

        // Test different technologies
        result = cache.findAvailableRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.H323, Technology.ADOBE_CONNECT}, null);
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());

        result = cache.findAvailableRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.SIP, Technology.ADOBE_CONNECT}, null);
        assertEquals(1, result.size());
        assertEquals(mcu2, result.get(0).getDeviceResource());

        // Test different number of required ports
        result = cache.findAvailableRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.ADOBE_CONNECT}, null);
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = cache.findAvailableRooms(Interval.parse("100/149"), 20,
                new Technology[]{Technology.ADOBE_CONNECT}, null);
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = cache.findAvailableRooms(Interval.parse("100/149"), 21,
                new Technology[]{Technology.ADOBE_CONNECT}, null);
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailableLicenseCount());
    }
}
