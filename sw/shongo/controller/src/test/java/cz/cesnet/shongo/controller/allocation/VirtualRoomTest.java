package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.List;

import static junitx.framework.Assert.assertEquals;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomTest extends AbstractDatabaseTest
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
        mcu1.addCapability(new VirtualRoomsCapability(50));
        mcu1.setAllocatable(true);
        cache.addResource(mcu1);

        AllocatedVirtualRoom room1 = new AllocatedVirtualRoom();
        room1.setResource(mcu1);
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.setPortCount(25);
        cache.addAllocatedItem(room1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu1");
        mcu2.addTechnology(Technology.SIP);
        mcu2.addTechnology(Technology.ADOBE_CONNECT);
        mcu2.addCapability(new VirtualRoomsCapability(100));
        mcu2.setAllocatable(true);
        cache.addResource(mcu2);

        AllocatedVirtualRoom room2 = new AllocatedVirtualRoom();
        room2.setResource(mcu2);
        room2.setSlot(DateTime.parse("50"), DateTime.parse("150"));
        room2.setPortCount(50);
        cache.addAllocatedItem(room2);

        AllocatedVirtualRoom room3 = new AllocatedVirtualRoom();
        room3.setResource(mcu2);
        room3.setSlot(DateTime.parse("100"), DateTime.parse("200"));
        room3.setPortCount(30);
        cache.addAllocatedItem(room3);

        // ---------------------------------
        // Test find available virtual rooms
        // ---------------------------------
        List<AvailableVirtualRoom> result;

        // Test different intervals
        result = cache.findAvailableVirtualRooms(Interval.parse("0/1"), 50);
        assertEquals(2, result.size());

        result = cache.findAvailableVirtualRooms(Interval.parse("200/250"), 50);
        assertEquals(2, result.size());

        result = cache.findAvailableVirtualRooms(Interval.parse("50/100"), 50);
        assertEquals(1, result.size());

        result = cache.findAvailableVirtualRooms(Interval.parse("100/150"), 50);
        assertEquals(1, result.size());

        // Test different technologies
        result = cache.findAvailableVirtualRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.H323, Technology.ADOBE_CONNECT}
        );
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());

        result = cache.findAvailableVirtualRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.SIP, Technology.ADOBE_CONNECT}
        );
        assertEquals(1, result.size());
        assertEquals(mcu2, result.get(0).getDeviceResource());

        // Test different number of required ports
        result = cache.findAvailableVirtualRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.ADOBE_CONNECT}
        );
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailablePortCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailablePortCount());

        result = cache.findAvailableVirtualRooms(Interval.parse("100/149"), 20,
                new Technology[]{Technology.ADOBE_CONNECT}
        );
        assertEquals(2, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailablePortCount());
        assertEquals(mcu2, result.get(1).getDeviceResource());
        assertEquals(20, result.get(1).getAvailablePortCount());

        result = cache.findAvailableVirtualRooms(Interval.parse("100/149"), 21,
                new Technology[]{Technology.ADOBE_CONNECT}
        );
        assertEquals(1, result.size());
        assertEquals(mcu1, result.get(0).getDeviceResource());
        assertEquals(50, result.get(0).getAvailablePortCount());
    }
}
