package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.request.ExistingResourceSpecification;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.Task}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TaskTest
{
    @Test
    public void testFailures() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new ResourceDatabase());

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        try {
            task.createAllocatedCompartment();
            fail("Exception about not enough requested ports should be thrown.");
        } catch (FaultException exception) {
        }

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.SIP}));
        try {
            task.createAllocatedCompartment();
            fail("Exception about no available virtual room should be thrown.");
        } catch (FaultException exception) {
        }

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        try {
            task.createAllocatedCompartment();
            fail("Exception about no alias available should be thrown.");
        } catch (FaultException exception) {
        }
    }

    @Test
    public void testNoVirtualRoom() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new ResourceDatabase());
        task.addAllocatedItem(new SimpleAllocatedEndpoint(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        AllocatedCompartment allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(2, allocatedCompartment.getAllocatedItems().size());
        assertEquals(1, allocatedCompartment.getConnections().size());
    }

    @Test
    public void testSingleVirtualRoom() throws Exception
    {
        ResourceDatabase resourceDatabase = ResourceDatabase.createTestingResourceDatabase();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAddress(Address.LOCALHOST);
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new VirtualRoomsCapability(100));
        resourceDatabase.addResource(deviceResource);

        Task task = new Task(Interval.parse("2012/2013"), resourceDatabase);
        AllocatedCompartment allocatedCompartment;

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(false, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323}));
        allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(3, allocatedCompartment.getAllocatedItems().size());
        assertEquals(2, allocatedCompartment.getConnections().size());

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.SIP}));
        allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(3, allocatedCompartment.getAllocatedItems().size());
        assertEquals(2, allocatedCompartment.getConnections().size());
    }

    @Test
    public void testAliasAllocation() throws Exception
    {
        ResourceDatabase resourceDatabase = ResourceDatabase.createTestingResourceDatabase();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addCapability(new VirtualRoomsCapability(100));
        deviceResource.addCapability(new AliasProviderCapability(Technology.SIP, AliasType.URI, "XXX@cesnet.cz"));
        resourceDatabase.addResource(deviceResource);

        Resource resource = new Resource();
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "95XXX"));
        resourceDatabase.addResource(resource);

        Task task = new Task(Interval.parse("2012/2013"), resourceDatabase);
        AllocatedCompartment allocatedCompartment;

        task.clear();
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(new Technology[]{Technology.H323}));
        allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(3, allocatedCompartment.getAllocatedItems().size());
        assertEquals(2, allocatedCompartment.getConnections().size());
    }

    @Test
    public void testDependentResource() throws Exception
    {
        ResourceDatabase resourceDatabase = ResourceDatabase.createTestingResourceDatabase();

        Resource room = new Resource();
        room.setAllocatable(true);
        resourceDatabase.addResource(room);

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.setParentResource(room);
        terminal1.setAllocatable(true);
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new StandaloneTerminalCapability());
        resourceDatabase.addResource(terminal1);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.setParentResource(room);
        terminal2.setAllocatable(true);
        terminal2.addTechnology(Technology.H323);
        terminal2.addCapability(new StandaloneTerminalCapability());
        resourceDatabase.addResource(terminal2);

        Task task = new Task(Interval.parse("2012/2013"), resourceDatabase);
        AllocatedCompartment allocatedCompartment;

        task.clear();
        task.addResource(new ExistingResourceSpecification(terminal1));
        allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(2, allocatedCompartment.getAllocatedItems().size());
        assertEquals(0, allocatedCompartment.getConnections().size());

        task.clear();
        task.addResource(new ExistingResourceSpecification(terminal1));
        task.addResource(new ExistingResourceSpecification(terminal2));
        allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(3, allocatedCompartment.getAllocatedItems().size());
        assertEquals(1, allocatedCompartment.getConnections().size());
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
            throw new RuntimeException("TODO: Implement SimpleAllocatedEndpoint.assignAlias");
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
