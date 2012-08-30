package cz.cesnet.shongo.controller.scheduler.plan;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.fault.FaultException;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests for {@link Task}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TaskTest
{
    private static class SimpleEndpoint extends Endpoint
    {
        private boolean standalone = false;
        private Set<Technology> technologies = new HashSet<Technology>();

        public SimpleEndpoint(boolean standalone, Technology[] technologies)
        {
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleEndpoint(Technology[] technologies)
        {

            this(false, technologies);
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
        task.addEndpoint(new SimpleEndpoint(new Technology[]{Technology.H323}));
        try {
            task.findPlan();
            fail("Exception about not enough ports should be thrown.");
        } catch (FaultException exception) {
        }

        task.clear();
        task.addEndpoint(new SimpleEndpoint(new Technology[]{Technology.H323}));
        task.addEndpoint(new SimpleEndpoint(new Technology[]{Technology.SIP}));
        try {
            task.findPlan();
            fail("Exception about no available virtual room should be thrown.");
        } catch (FaultException exception) {
        }
    }

    @Test
    public void testNoVirtualRoom() throws Exception
    {
        Task task = new Task(Interval.parse("2012/2013"), new ResourceDatabase());
        task.addEndpoint(new SimpleEndpoint(true, new Technology[]{Technology.H323}));
        task.addEndpoint(new SimpleEndpoint(true, new Technology[]{Technology.H323, Technology.SIP}));
        Plan plan = task.findPlan();
        assertNotNull(plan);
        assertEquals(2, plan.getEndpoints().size());
        assertEquals(1, plan.getConnections().size());
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
        Plan plan;

        task.clear();
        task.addEndpoint(new SimpleEndpoint(false, new Technology[]{Technology.H323}));
        task.addEndpoint(new SimpleEndpoint(true, new Technology[]{Technology.H323}));
        plan = task.findPlan();
        assertNotNull(plan);
        assertEquals(3, plan.getEndpoints().size());
        assertEquals(2, plan.getConnections().size());

        task.clear();
        task.addEndpoint(new SimpleEndpoint(true, new Technology[]{Technology.H323}));
        task.addEndpoint(new SimpleEndpoint(true, new Technology[]{Technology.SIP}));
        plan = task.findPlan();
        assertNotNull(plan);
        assertEquals(3, plan.getEndpoints().size());
        assertEquals(2, plan.getConnections().size());
    }
}
