package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.resource.StandaloneTerminalCapability;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointProvider;
import cz.cesnet.shongo.controller.booking.participant.ExistingEndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.scheduler.*;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link cz.cesnet.shongo.controller.booking.compartment.CompartmentReservationTask}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservationTaskTest extends AbstractSchedulerTest
{
    @Test
    public void testFailures() throws Exception
    {
        Interval slot = Temporal.INTERVAL_INFINITE;
        SchedulerContext schedulerContext = createSchedulerContext();
        CompartmentReservationTask compartmentReservationTask;

        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        try {
            compartmentReservationTask.perform();
            Assert.fail("Exception about not enough requested ports should be thrown.");
        }
        catch (SchedulerException exception) {
        }

        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(new Technology[]{Technology.SIP}));
        try {
            compartmentReservationTask.perform();
            Assert.fail("Exception about no available virtual room should be thrown.");
        }
        catch (SchedulerException exception) {
        }

        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.H323}));
        try {
            compartmentReservationTask.perform();
            Assert.fail("Exception about no alias available should be thrown.");
        }
        catch (SchedulerException exception) {
        }

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new RoomProviderCapability(100));
        createResource(deviceResource);

        compartmentReservationTask = new CompartmentReservationTask(
                schedulerContext, slot, CallInitiation.VIRTUAL_ROOM);
        compartmentReservationTask.addParticipant(
                new ExternalEndpointSetParticipant(Technology.H323, 3));
        try {
            compartmentReservationTask.perform();
            Assert.fail("Exception about cannot create.");
        }
        catch (SchedulerException exception) {
        }
    }

    @Test
    public void testNoRoom() throws Exception
    {
        Interval slot = Temporal.INTERVAL_INFINITE;
        SchedulerContext schedulerContext = createSchedulerContext();
        CompartmentReservationTask compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(new SimpleEndpointParticipant(
                new Alias(AliasType.H323_E164, "950000001"), true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.H323}));
        Reservation reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(2, reservation.getChildReservations().size());
        Assert.assertEquals(1, ((Compartment) reservation.getExecutable()).getConnections().size());
    }

    @Test
    public void testSingleRoom() throws Exception
    {
        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new RoomProviderCapability(100));
        deviceResource.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164, true));
        deviceResource.addCapability(new AliasProviderCapability("950000001@cesnet.cz", AliasType.SIP_URI, true));
        createResource(deviceResource);

        SchedulerContext schedulerContext;
        CompartmentReservationTask compartmentReservationTask;
        Reservation reservation;

        Interval slot = Temporal.INTERVAL_INFINITE;
        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(false, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.H323}));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(4, reservation.getChildReservations().size());
        Assert.assertEquals(2, ((Compartment) reservation.getExecutable()).getConnections().size());

        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.H323}));
        compartmentReservationTask.addChildReservation(
                new SimpleEndpointParticipant(true, new Technology[]{Technology.SIP}));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(5, reservation.getChildReservations().size());
        Assert.assertEquals(2, ((Compartment) reservation.getExecutable()).getConnections().size());
    }

    @Test
    public void testSingleRoomFromMultipleEndpoints() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(100));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        createResource(mcu);

        DeviceResource terminal = new DeviceResource();
        terminal.setAllocatable(true);
        terminal.setTechnology(Technology.H323);
        terminal.addCapability(new StandaloneTerminalCapability());
        createResource(terminal);

        Interval slot = Temporal.INTERVAL_INFINITE;
        SchedulerContext schedulerContext = createSchedulerContext();

        CompartmentReservationTask compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 50));
        compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(terminal));
        Reservation reservation = compartmentReservationTask.perform();
        Assert.assertEquals(3, reservation.getChildReservations().size());
        Assert.assertEquals(2, ((Compartment) reservation.getExecutable()).getEndpoints().size());
        Assert.assertEquals(1, ((Compartment) reservation.getExecutable()).getRoomEndpoints().size());
    }

    @Test
    public void testAliasAllocation() throws Exception
    {
        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addTechnology(Technology.SIP);
        deviceResource.addCapability(new RoomProviderCapability(100));
        createResource(deviceResource);

        Resource resource = new Resource();
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        resource.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        createResource(resource);

        Interval slot = Temporal.INTERVAL_INFINITE;
        SchedulerContext schedulerContext;
        CompartmentReservationTask compartmentReservationTask;
        Reservation reservation;

        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot, CallInitiation.TERMINAL);
        compartmentReservationTask.addParticipant(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        compartmentReservationTask.addParticipant(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(4, reservation.getChildReservations().size());
        Assert.assertEquals(2, ((Compartment) reservation.getExecutable()).getConnections().size());

        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot, CallInitiation.VIRTUAL_ROOM);
        compartmentReservationTask.addParticipant(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        compartmentReservationTask.addParticipant(
                new SimpleEndpointParticipant(new Technology[]{Technology.H323}));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(5, reservation.getChildReservations().size());
        Assert.assertEquals(2, ((Compartment) reservation.getExecutable()).getConnections().size());

        try {
            schedulerContext = createSchedulerContext();
            compartmentReservationTask = new CompartmentReservationTask(
                    schedulerContext, slot, CallInitiation.VIRTUAL_ROOM);
            compartmentReservationTask.addParticipant(
                    new SimpleEndpointParticipant(new Technology[]{Technology.SIP}));
            compartmentReservationTask.addParticipant(
                    new SimpleEndpointParticipant(new Technology[]{Technology.SIP}));
            compartmentReservationTask.perform();
            Assert.fail("Only one SIP alias should be possible to allocate.");
        }
        catch (SchedulerException exception) {
        }
    }

    @Test
    public void testDependentResource() throws Exception
    {
        Resource room = new Resource();
        room.setAllocatable(true);
        createResource(room);

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setParentResource(room);
        terminal1.setAllocatable(true);
        terminal1.addTechnology(Technology.H323);
        StandaloneTerminalCapability terminalCapability = new StandaloneTerminalCapability();
        terminalCapability.addAlias(new Alias(AliasType.H323_E164, "950000001"));
        terminal1.addCapability(terminalCapability);
        createResource(terminal1);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.setParentResource(room);
        terminal2.setAllocatable(true);
        terminal2.addTechnology(Technology.H323);
        terminal2.addCapability(new StandaloneTerminalCapability());
        createResource(terminal2);

        Interval slot = Temporal.INTERVAL_INFINITE;
        SchedulerContext schedulerContext;
        CompartmentReservationTask compartmentReservationTask;
        Reservation reservation;

        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(terminal1));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(2, reservation.getNestedReservations().size());
        Assert.assertEquals(0, ((Compartment) reservation.getExecutable()).getConnections().size());

        schedulerContext = createSchedulerContext();
        compartmentReservationTask = new CompartmentReservationTask(schedulerContext, slot);
        compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(terminal1));
        compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(terminal2));
        reservation = compartmentReservationTask.perform();
        Assert.assertNotNull(reservation);
        Assert.assertEquals(3, reservation.getNestedReservations().size());
        Assert.assertEquals(1, ((Compartment) reservation.getExecutable()).getConnections().size());
    }

    @Test
    public void testEndpointUniqueness() throws Exception
    {
        DeviceResource endpoint = new DeviceResource();
        endpoint.setAllocatable(true);
        endpoint.addTechnology(Technology.H323);
        endpoint.addCapability(new StandaloneTerminalCapability());
        createResource(endpoint);

        try {
            Interval slot = Temporal.INTERVAL_INFINITE;
            SchedulerContext schedulerContext = createSchedulerContext();
            CompartmentReservationTask compartmentReservationTask =
                    new CompartmentReservationTask(schedulerContext, slot);
            compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(endpoint));
            compartmentReservationTask.addParticipant(new ExistingEndpointParticipant(endpoint));
            compartmentReservationTask.perform();
            Assert.fail("Exception that resource is requested multiple times should be thrown");
        }
        catch (SchedulerException exception) {
            Assert.assertEquals(SchedulerReportSet.ResourceMultipleRequestedReport.class,
                    exception.getReport().getChildReports().get(0).getClass());
        }
    }

    private static class SimpleEndpointParticipant extends AbstractParticipant implements ReservationTaskProvider
    {
        private Alias alias = null;
        private boolean standalone = false;
        private Set<Technology> technologies = new HashSet<Technology>();

        public SimpleEndpointParticipant(Alias alias, boolean standalone, Technology[] technologies)
        {
            this.alias = alias;
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleEndpointParticipant(boolean standalone, Technology[] technologies)
        {
            this.standalone = standalone;
            for (Technology technology : technologies) {
                this.technologies.add(technology);
            }
        }

        public SimpleEndpointParticipant(Technology[] technologies)
        {
            this(false, technologies);
        }


        @Override
        public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
        {
            return new ReservationTask(schedulerContext, slot)
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
                        public void addAssignedAlias(Alias alias) throws SchedulerException
                        {
                        }

                        @Override
                        public List<Alias> getAliases()
                        {
                            List<Alias> aliases = new ArrayList<Alias>();
                            if (alias != null) {
                                aliases.add(alias);
                            }
                            return aliases;
                        }
                    };

                    @Override
                    public Endpoint getEndpoint()
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
                protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
                {
                    return new SimpleEndpointReservation();
                }
            };
        }

        @Override
        protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
        {
            throw new RuntimeException("Not implemented.");
        }
    }
}
