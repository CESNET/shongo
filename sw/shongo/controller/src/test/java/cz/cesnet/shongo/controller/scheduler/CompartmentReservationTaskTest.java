package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.compartment.Endpoint;
import cz.cesnet.shongo.controller.compartment.EndpointProvider;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.Interval;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests for {@link CompartmentReservationTask}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservationTaskTest
{
    @Test
    public void testFailures() throws Exception
    {
        ReservationTask.Context context = new ReservationTask.Context(new Cache(), Interval.parse("2012/2013"));

        CompartmentReservationTask compartmentReservationTask;

        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        try {
            compartmentReservationTask.perform();
            fail("Exception about not enough requested ports should be thrown.");
        }
        catch (ReportException exception) {
        }

        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(new Technology[]{Technology.SIP}));
        try {
            compartmentReservationTask.perform();
            fail("Exception about no available virtual room should be thrown.");
        }
        catch (ReportException exception) {
        }

        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(true, new Technology[]{Technology.H323}));
        try {
            compartmentReservationTask.perform();
            fail("Exception about no alias available should be thrown.");
        }
        catch (ReportException exception) {
        }
    }

    @Test
    public void testNoVirtualRoom() throws Exception
    {
        ReservationTask.Context context = new ReservationTask.Context(new Cache(), Interval.parse("2012/2013"));
        CompartmentReservationTask compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(Address.LOCALHOST, true, new Technology[]{Technology.H323}));
        CompartmentReservation reservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(reservation);
        assertEquals(2, reservation.getChildReservations().size());
        assertEquals(1, reservation.getCompartment().getConnections().size());
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

        ReservationTask.Context context;
        CompartmentReservationTask compartmentReservationTask;
        CompartmentReservation compartmentReservation;

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(false, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(true, new Technology[]{Technology.H323}));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(3, compartmentReservation.getChildReservations().size());
        assertEquals(2, compartmentReservation.getCompartment().getConnections().size());

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointSpecification(true, new Technology[]{Technology.SIP}));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(3, compartmentReservation.getChildReservations().size());
        assertEquals(2, compartmentReservation.getCompartment().getConnections().size());
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

        ReservationTask.Context context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        CompartmentReservationTask compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 50));
        compartmentReservationTask.addChildSpecification(new ExistingEndpointSpecification(terminal));
        CompartmentReservation compartmentReservation = compartmentReservationTask
                .perform(CompartmentReservation.class);
        assertEquals(3, compartmentReservation.getChildReservations().size());
        assertEquals(2, compartmentReservation.getCompartment().getEndpoints().size());
        assertEquals(1, compartmentReservation.getCompartment().getVirtualRooms().size());
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

        ReservationTask.Context context;
        CompartmentReservationTask compartmentReservationTask;
        CompartmentReservation compartmentReservation;

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context, CallInitiation.TERMINAL);
        compartmentReservationTask.addChildSpecification(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildSpecification(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(4, compartmentReservation.getChildReservations().size());
        assertEquals(2, compartmentReservation.getCompartment().getConnections().size());

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context, CallInitiation.VIRTUAL_ROOM);
        compartmentReservationTask.addChildSpecification(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildSpecification(
                new SimpleEndpointSpecification(new Technology[]{Technology.H323}));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(5, compartmentReservation.getChildReservations().size());
        assertEquals(2, compartmentReservation.getCompartment().getConnections().size());

        try {
            context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
            compartmentReservationTask = new CompartmentReservationTask(context, CallInitiation.VIRTUAL_ROOM);
            compartmentReservationTask.addChildSpecification(
                    new SimpleEndpointSpecification(new Technology[]{Technology.SIP}));
            compartmentReservationTask.addChildSpecification(
                    new SimpleEndpointSpecification(new Technology[]{Technology.SIP}));
            compartmentReservationTask.perform(CompartmentReservation.class);
            fail("Only one SIP alias should be possible to allocate.");
        }
        catch (ReportException exception) {
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

        ReservationTask.Context context;
        CompartmentReservationTask compartmentReservationTask;
        CompartmentReservation compartmentReservation;

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(2, compartmentReservation.getNestedReservations().size());
        assertEquals(0, compartmentReservation.getCompartment().getConnections().size());

        context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        compartmentReservationTask = new CompartmentReservationTask(context);
        compartmentReservationTask.addChildSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentReservationTask.addChildSpecification(new ExistingEndpointSpecification(terminal2));
        compartmentReservation = compartmentReservationTask.perform(CompartmentReservation.class);
        assertNotNull(compartmentReservation);
        assertEquals(3, compartmentReservation.getNestedReservations().size());
        assertEquals(1, compartmentReservation.getCompartment().getConnections().size());
    }

    private static class SimpleEndpointSpecification extends Specification implements ReservationTaskProvider
    {
        private Address address = null;
        private boolean standalone = false;
        private Set<Technology> technologies = new HashSet<Technology>();

        public SimpleEndpointSpecification(Address address, boolean standalone, Technology[] technologies)
        {
            this.address = address;
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleEndpointSpecification(boolean standalone, Technology[] technologies)
        {
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleEndpointSpecification(Technology[] technologies)
        {
            this(false, technologies);
        }


        @Override
        public ReservationTask createReservationTask(ReservationTask.Context context)
        {
            return new ReservationTask(context)
            {
                class SimpleEndpointReservation extends Reservation implements EndpointProvider
                {
                    private Endpoint endpoint = new Endpoint()
                    {
                        @Override
                        public int getCount()
                        {
                            return 1;
                        }

                        @Override
                        public Set<Technology> getTechnologies()
                        {
                            return technologies;
                        }

                        @Override
                        public boolean isStandalone()
                        {
                            return standalone;
                        }

                        @Override
                        public void addAssignedAlias(Alias alias)
                        {
                        }

                        @Override
                        public List<Alias> getAliases()
                        {
                            return new ArrayList<Alias>();
                        }

                        @Override
                        public Address getAddress()
                        {
                            return address;
                        }
                    };

                    @Override
                    public Endpoint createEndpoint()
                    {
                        return endpoint;
                    }

                    @Override
                    protected cz.cesnet.shongo.controller.api.Reservation createApi()
                    {
                        throw new TodoImplementException();
                    }
                }

                @Override
                protected Reservation createReservation()
                        throws ReportException
                {
                    return new SimpleEndpointReservation();
                }
            };
        }

        @Override
        protected cz.cesnet.shongo.controller.api.Specification createApi()
        {
            throw new IllegalStateException("Not implemented.");
        }
    }
}
