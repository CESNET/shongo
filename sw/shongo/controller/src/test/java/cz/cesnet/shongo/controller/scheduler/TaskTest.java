package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.controller.scheduler.Task;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.Task}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TaskTest
{
    private static class SimpleAllocatedEndpoint extends AllocatedItem implements AllocatedEndpoint
    {
        private boolean standalone = false;
        private Set<Technology> technologies = new HashSet<Technology>();

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
    }

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
    }

    @Test
    public void testNoVirtualRoom() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new ResourceDatabase());
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323}));
        task.addAllocatedItem(new SimpleAllocatedEndpoint(true, new Technology[]{Technology.H323, Technology.SIP}));
        AllocatedCompartment allocatedCompartment = task.createAllocatedCompartment();
        assertNotNull(allocatedCompartment);
        assertEquals(2, allocatedCompartment.getAllocatedItems().size());
        assertEquals(1, allocatedCompartment.getConnections().size());
    }

    @Test
    public void testSingleVirtualRoom() throws Exception
    {
        ResourceDatabase resourceDatabase = new ResourceDatabase();
        resourceDatabase.disablePersistedRequirement();
        resourceDatabase.init();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setSchedulable(true);
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
}
