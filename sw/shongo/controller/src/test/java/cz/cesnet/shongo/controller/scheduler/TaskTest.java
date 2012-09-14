package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.allocationaold.*;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.CallInitiation;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests for {@link Task}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TaskTest
{
    @Test
    public void testFailures() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new Cache());

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        try {
            task.createReservation();
            fail("Exception about not enough requested ports should be thrown.");
        } catch (ReportException exception) {
        }

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.SIP}));
        try {
            task.createReservation();
            fail("Exception about no available virtual room should be thrown.");
        } catch (ReportException exception) {
        }

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        try {
            task.createReservation();
            fail("Exception about no alias available should be thrown.");
        } catch (ReportException exception) {
        }
    }

    @Test
    public void testNoVirtualRoom() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new Cache());
        task.addAllocatedItem(new SimpleAllocatedEndpoint(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        Reservation reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(2, reservation.getChildReservations().size());
        assertEquals(1, reservation.getConnections().size());
    }

    @Test
    public void testSingleVirtualRoom() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAddress(Address.LOCALHOST);
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new VirtualRoomsCapability(100));
        cache.addResource(deviceResource);

        Task task = new Task(Interval.parse("2012/2013"), cache);
        Reservation reservation;

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(false, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323}));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(3, reservation.getChildReservations().size());
        assertEquals(2, reservation.getConnections().size());

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.SIP}));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(3, reservation.getChildReservations().size());
        assertEquals(2, reservation.getConnections().size());
    }

    @Test
    public void testSingleVirtualRoomFromMultipleEndpoints() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        DeviceResource mcu = new DeviceResource();
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(100));
        mcu.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "95[ddd]"));
        cache.addResource(mcu);

        DeviceResource terminal = new DeviceResource();
        terminal.setAllocatable(true);
        terminal.setTechnology(Technology.H323);
        terminal.addCapability(new StandaloneTerminalCapability());
        cache.addResource(terminal);

        Task task = new Task(Interval.parse("2012/2013"), cache);
        task.addAllocatedItem(new AllocatedExternalEndpoint(new ExternalEndpointSpecification(Technology.H323, 50)));
        task.addAllocatedItem(new AllocatedDevice(terminal));
        Reservation reservation = task.createReservation();
        assertEquals(4, reservation.getChildReservations().size());
    }

    @Test
    public void testAliasAllocation() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new VirtualRoomsCapability(100));
        cache.addResource(deviceResource);

        Resource resource = new Resource();
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "950[ddd]"));
        resource.addCapability(new AliasProviderCapability(Technology.SIP, AliasType.URI, "001@cesnet.cz"));
        cache.addResource(resource);

        Task task = new Task(Interval.parse("2012/2013"), cache);
        Reservation reservation;

        task.clear();
        task.setCallInitiation(CallInitiation.TERMINAL);
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(4, reservation.getChildReservations().size());
        assertEquals(2, reservation.getConnections().size());

        task.clear();
        task.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(5, reservation.getChildReservations().size());
        assertEquals(2, reservation.getConnections().size());

        try {
            task.clear();
            task.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
            task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.SIP}));
            task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.SIP}));
            reservation = task.createReservation();
            fail("Only one SIP alias should be possible to allocate.");
        } catch (ReportException exception) {
        }
    }

    @Test
    public void testDependentResource() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        Resource room = new Resource();
        room.setAllocatable(true);
        cache.addResource(room);

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.setParentResource(room);
        terminal1.setAllocatable(true);
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new StandaloneTerminalCapability());
        cache.addResource(terminal1);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.setParentResource(room);
        terminal2.setAllocatable(true);
        terminal2.addTechnology(Technology.H323);
        terminal2.addCapability(new StandaloneTerminalCapability());
        cache.addResource(terminal2);

        Task task = new Task(Interval.parse("2012/2013"), cache);
        Reservation reservation;

        task.clear();
        task.addResource(new ExistingEndpointSpecification(terminal1));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(2, reservation.getChildReservations().size());
        assertEquals(0, reservation.getConnections().size());

        task.clear();
        task.addResource(new ExistingEndpointSpecification(terminal1));
        task.addResource(new ExistingEndpointSpecification(terminal2));
        reservation = task.createReservation();
        assertNotNull(reservation);
        assertEquals(3, reservation.getChildReservations().size());
        assertEquals(1, reservation.getConnections().size());
    }

    private static class SimpleAllocatedEndpoint extends AllocatedItem implements AllocatedEndpoint
    {
        private Address address = null;
        private boolean standalone = false;
        private Set<Technology> technologies = new HashSet<Technology>();

        public SimpleAllocatedEndpoint(Address address, boolean standalone, Technology[] technologies)
        {
            this.address = address;
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleAllocatedEndpoint(boolean standalone, Technology[] technologies)
        {
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleAllocatedEndpoint(Technology[] technologies)
        {
            this(false, technologies);
        }

        @Override
        public int getCount()
        {
            return 1;
        }

        @Override
        public Set<Technology> getSupportedTechnologies()
        {
            return technologies;
        }

        @Override
        public boolean isStandalone()
        {
            return standalone;
        }

        @Override
        public void assignAlias(Alias alias)
        {
        }

        @Override
        public List<Alias> getAssignedAliases()
        {
            return new ArrayList<Alias>();
        }

        @Override
        public Address getAddress()
        {
            return address;
        }
    }
}
