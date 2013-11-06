package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

/**
 * Tests for reallocation of reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorMigrationTest extends AbstractExecutorTest
{
    @Test
    public void testRoomMigrationSameDevice() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTimeStart = DateTime.parse("2012-01-01T12:00");
        DateTime dateTimeEnd = DateTime.parse("2012-01-01T14:00");
        DateTime dateTimeMiddle = dateTimeStart.plus((dateTimeEnd.getMillis() - dateTimeStart.getMillis()) / 2);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);
        Long mcuPersistenceId = EntityIdentifier.parse(mcuId).getPersistenceId();

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTimeStart, dateTimeEnd);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeStart, Period.months(1)));
        checkAllocated(requestId);

        // Execute room
        ExecutionResult result = runExecutor(dateTimeStart);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room1 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcuPersistenceId, room1.getResource().getId());
        Assert.assertEquals(5, room1.getLicenseCount());

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        ((RoomSpecification) reservationRequest.getSpecification()).setParticipantCount(7);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeMiddle, Period.months(1)));
        checkAllocated(requestId);

        // Execute room migration
        result = runExecutor(dateTimeMiddle);
        Assert.assertEquals(1, result.getStoppedExecutables().size());
        Assert.assertEquals(room1.getId(), result.getStoppedExecutables().get(0).getId());
        Assert.assertEquals(1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room2 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcuPersistenceId, room2.getResource().getId());
        Assert.assertEquals(7, room2.getLicenseCount());

        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    @Test
    public void testRoomMigrationAnotherDevice() throws Exception
    {
        McuTestAgent mcuAgent1 = getController().addJadeAgent("mcu1", new McuTestAgent());
        McuTestAgent mcuAgent2 = getController().addJadeAgent("mcu2", new McuTestAgent());

        DateTime dateTimeStart = DateTime.parse("2012-01-01T12:00");
        DateTime dateTimeEnd = DateTime.parse("2012-01-01T14:00");
        DateTime dateTimeMiddle = dateTimeStart.plus((dateTimeEnd.getMillis() - dateTimeStart.getMillis()) / 2);

        DeviceResource mcu1 = new DeviceResource();
        mcu1.setName("mcu1");
        mcu1.setAllocatable(true);
        mcu1.addTechnology(Technology.H323);
        mcu1.addCapability(new RoomProviderCapability(5));
        mcu1.setMode(new ManagedMode(mcuAgent1.getName()));
        String mcu1Id = getResourceService().createResource(SECURITY_TOKEN, mcu1);
        Long mcu1PersistenceId = EntityIdentifier.parse(mcu1Id).getPersistenceId();

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu1");
        mcu2.setAllocatable(true);
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new RoomProviderCapability(10));
        mcu2.setMode(new ManagedMode(mcuAgent2.getName()));
        String mcu2Id = getResourceService().createResource(SECURITY_TOKEN, mcu2);
        Long mcu2PersistenceId = EntityIdentifier.parse(mcu2Id).getPersistenceId();

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTimeStart, dateTimeEnd);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.setParticipantCount(5);
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setResourceId(mcu1Id);
        reservationRequest.setSpecification(roomSpecification);
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeStart, Period.months(1)));
        checkAllocated(requestId);

        // Execute room
        mcuAgent1.clearPerformedCommands();
        mcuAgent2.clearPerformedCommands();
        ExecutionResult result = runExecutor(dateTimeStart);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room1 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcu1PersistenceId, room1.getResource().getId());
        Assert.assertEquals(5, room1.getLicenseCount());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
            }}, mcuAgent1.getPerformedCommandClasses());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>(), mcuAgent2.getPerformedCommandClasses());

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        roomSpecification = ((RoomSpecification) reservationRequest.getSpecification());
        roomSpecification.setParticipantCount(7);
        roomSpecification.setResourceId(null);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeMiddle, Period.months(1)));
        checkAllocated(requestId);

        // Execute room migration
        mcuAgent1.clearPerformedCommands();
        mcuAgent2.clearPerformedCommands();
        result = runExecutor(dateTimeMiddle);
        Assert.assertEquals(1, result.getStoppedExecutables().size());
        Assert.assertEquals(room1.getId(), result.getStoppedExecutables().get(0).getId());
        Assert.assertEquals(1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room2 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcu2PersistenceId, room2.getResource().getId());
        Assert.assertEquals(7, room2.getLicenseCount());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent1.getPerformedCommandClasses());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
            }}, mcuAgent2.getPerformedCommandClasses());
    }
}
