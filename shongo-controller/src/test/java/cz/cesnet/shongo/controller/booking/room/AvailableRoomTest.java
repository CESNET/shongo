package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.ReservationRequestSet;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.AvailableRoom;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests for caching of virtual rooms by {@link Cache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AvailableRoomTest extends AbstractSchedulerTest
{
    @Test
    public void testSingleDevice() throws Exception
    {
        // -----------------------------------------------------
        // Create one MCU and allocate some virtual rooms on it
        // -----------------------------------------------------
        DeviceResource mcu = new DeviceResource();
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.ADOBE_CONNECT);
        mcu.addCapability(new RoomProviderCapability(50));
        mcu.setAllocatable(true);
        createResource(mcu);

        RoomReservation room1 = new RoomReservation();
        room1.setRoomProviderCapability(mcu.getCapability(RoomProviderCapability.class));
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.setLicenseCount(10);
        createReservation(room1);

        RoomReservation room2 = new RoomReservation();
        room2.setRoomProviderCapability(mcu.getCapability(RoomProviderCapability.class));
        room2.setSlot(DateTime.parse("5"), DateTime.parse("50"));
        room2.setLicenseCount(20);
        createReservation(room2);

        RoomReservation room3 = new RoomReservation();
        room3.setRoomProviderCapability(mcu.getCapability(RoomProviderCapability.class));
        room3.setSlot(DateTime.parse("50"), DateTime.parse("95"));
        room3.setLicenseCount(30);
        createReservation(room3);

        RoomReservation room4 = new RoomReservation();
        room4.setRoomProviderCapability(mcu.getCapability(RoomProviderCapability.class));
        room4.setSlot(DateTime.parse("10"), DateTime.parse("40"));
        room4.setLicenseCount(5);
        createReservation(room4);

        RoomReservation room5 = new RoomReservation();
        room5.setRoomProviderCapability(mcu.getCapability(RoomProviderCapability.class));
        room5.setSlot(DateTime.parse("60"), DateTime.parse("90"));
        room5.setLicenseCount(5);
        createReservation(room5);

        // -----------------------------
        // Test available virtual rooms
        // -----------------------------

        checkAvailableRoom(Interval.parse("20/30"), 15);
        checkAvailableRoom(Interval.parse("70/90"), 5);
        checkAvailableRoom(Interval.parse("1/100"), 5);
        checkAvailableRoom(Interval.parse("1/5"), 40);
        checkAvailableRoom(Interval.parse("95/100"), 40);
    }

    @Test
    public void testMultipleDevices() throws Exception
    {
        // -----------------------------------------------------
        // Create two MCUs and allocate some virtual rooms on it
        // -----------------------------------------------------
        DeviceResource mcu1 = new DeviceResource();
        mcu1.addTechnology(Technology.H323);
        mcu1.addTechnology(Technology.ADOBE_CONNECT);
        mcu1.addCapability(new RoomProviderCapability(50));
        mcu1.setAllocatable(true);
        createResource(mcu1);

        RoomReservation room1 = new RoomReservation();
        room1.setRoomProviderCapability(mcu1.getCapability(RoomProviderCapability.class));
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.setLicenseCount(25);
        createReservation(room1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.addTechnology(Technology.SIP);
        mcu2.addTechnology(Technology.ADOBE_CONNECT);
        mcu2.addCapability(new RoomProviderCapability(100));
        mcu2.setAllocatable(true);
        createResource(mcu2);

        RoomReservation room2 = new RoomReservation();
        room2.setRoomProviderCapability(mcu2.getCapability(RoomProviderCapability.class));
        room2.setSlot(DateTime.parse("50"), DateTime.parse("150"));
        room2.setLicenseCount(50);
        createReservation(room2);

        RoomReservation room3 = new RoomReservation();
        room3.setRoomProviderCapability(mcu2.getCapability(RoomProviderCapability.class));
        room3.setSlot(DateTime.parse("100"), DateTime.parse("200"));
        room3.setLicenseCount(30);
        createReservation(room3);

        // ---------------------------------
        // Test find available virtual rooms
        // ---------------------------------
        List<AvailableRoom> result;

        // Test different intervals
        result = findAvailableRooms(Interval.parse("0/1"), 50);
        Assert.assertEquals(2, result.size());

        result = findAvailableRooms(Interval.parse("200/250"), 50);
        Assert.assertEquals(2, result.size());

        result = findAvailableRooms(Interval.parse("50/100"), 50);
        Assert.assertEquals(1, result.size());

        result = findAvailableRooms(Interval.parse("100/150"), 50);
        Assert.assertEquals(1, result.size());

        // Test different technologies
        result = findAvailableRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.H323, Technology.ADOBE_CONNECT});
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(mcu1, result.get(0).getDeviceResource());

        result = findAvailableRooms(Interval.parse("100/149"), 10,
                new Technology[]{Technology.SIP, Technology.ADOBE_CONNECT});
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(mcu2, result.get(0).getDeviceResource());

        // Test different number of required ports
        result = findAvailableRooms(Interval.parse("100/149"), 10, new Technology[]{Technology.ADOBE_CONNECT});
        sortResult(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(mcu1, result.get(0).getDeviceResource());
        Assert.assertEquals(50, result.get(0).getAvailableLicenseCount());
        Assert.assertEquals(mcu2, result.get(1).getDeviceResource());
        Assert.assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = findAvailableRooms(Interval.parse("100/149"), 20, new Technology[]{Technology.ADOBE_CONNECT});
        sortResult(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(mcu1, result.get(0).getDeviceResource());
        Assert.assertEquals(50, result.get(0).getAvailableLicenseCount());
        Assert.assertEquals(mcu2, result.get(1).getDeviceResource());
        Assert.assertEquals(20, result.get(1).getAvailableLicenseCount());

        result = findAvailableRooms(Interval.parse("100/149"), 21, new Technology[]{Technology.ADOBE_CONNECT});
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(mcu1, result.get(0).getDeviceResource());
        Assert.assertEquals(50, result.get(0).getAvailableLicenseCount());
    }

    /**
     * Find {@link AvailableRoom}s in given {@code interval} which have
     * at least {@code requiredLicenseCount} available licenses and which supports given {@code technologies}.
     *
     * @param slot
     * @param licenseCount
     * @param technologies
     * @return list of {@link AvailableRoom}
     */
    private List<AvailableRoom> findAvailableRooms(Interval slot, int licenseCount, Set<Technology> technologies)
    {
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        List<AvailableRoom> availableRooms = new ArrayList<AvailableRoom>();
        for (RoomProviderCapability roomProviderCapability :
                resourceCache.getDeviceCapabilities(RoomProviderCapability.class, technologies)) {
            SchedulerContext schedulerContext = createSchedulerContext(slot);
            AvailableRoom availableRoom = schedulerContext.getAvailableRoom(
                    roomProviderCapability, slot, new RoomReservationTask(schedulerContext, slot));
            if (availableRoom.getAvailableLicenseCount() >= licenseCount) {
                availableRooms.add(availableRoom);
            }
        }
        return availableRooms;
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    private List<AvailableRoom> findAvailableRooms(Interval interval, int requiredLicenseCount,
            Technology[] technologies)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableRooms(interval, requiredLicenseCount, technologySet);
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    private List<AvailableRoom> findAvailableRooms(Interval interval, int requiredLicenseCount)
    {
        return findAvailableRooms(interval, requiredLicenseCount, (Set<Technology>) null);
    }

    /**
     * Check available room license count.
     *
     * @param interval
     * @param availableLicenseCount
     */
    private void checkAvailableRoom(Interval interval, int availableLicenseCount)
    {
        List<AvailableRoom> availableRooms = findAvailableRooms(interval, 0);
        Assert.assertEquals(1, availableRooms.size());
        AvailableRoom availableRoom = availableRooms.get(0);
        Assert.assertEquals(availableLicenseCount, availableRoom.getAvailableLicenseCount());
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
