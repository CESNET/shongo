package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests for allocation of migrations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MigrationTest extends AbstractExecutorTest
{
    @Test
    public void testRoomMigration() throws Exception
    {
        DeviceResource mcu1 = new DeviceResource();
        mcu1.setName("mcu1");
        mcu1.setAllocatable(true);
        mcu1.addTechnology(Technology.H323);
        mcu1.addCapability(new RoomProviderCapability(5));
        String mcu1Id = createResource(mcu1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu2");
        mcu2.setAllocatable(true);
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new RoomProviderCapability(10));
        String mcu2Id = createResource(mcu2);

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T12:00", "2012-01-01T14:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.setResourceId(mcu1Id);
        roomEstablishment.addTechnology(Technology.H323);
        reservationRequest.setSpecification(roomSpecification);
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01T12:00/2012-02-01T12:00"));
        Reservation reservation = checkAllocated(requestId);

        // Set the allocated room as started, because a migration is allocated only for started rooms
        EntityManager entityManager = createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        entityManager.getTransaction().begin();
        cz.cesnet.shongo.controller.booking.executable.Executable executable = executableManager.get(
                ObjectIdentifier.parseLocalId(reservation.getExecutable().getId(), ObjectType.EXECUTABLE));
        executable.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTED);
        executableManager.update(executable);
        entityManager.getTransaction().commit();
        entityManager.close();

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        roomSpecification = ((RoomSpecification) reservationRequest.getSpecification());
        roomSpecification.getEstablishment().setResourceId(null);
        roomSpecification.getAvailability().setParticipantCount(10);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01T13:00/2012-02-01T13:00"));
        checkAllocated(requestId);

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(2, reservationIds.size());
        RoomReservation reservation1 = (RoomReservation) service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01T12:00/2012-01-01T13:00"), reservation1.getSlot());
        Assert.assertEquals(mcu1Id, reservation1.getResourceId());
        RoomReservation reservation2 = (RoomReservation) service.getReservation(SECURITY_TOKEN, reservationIds.get(1));
        Assert.assertEquals(Interval.parse("2012-01-01T13:00/2012-01-01T14:00"), reservation2.getSlot());
        Assert.assertEquals(mcu2Id, reservation2.getResourceId());
        RoomExecutable roomExecutable1 = (RoomExecutable) reservation1.getExecutable();
        Assert.assertEquals(Interval.parse("2012-01-01T12:00/2012-01-01T13:00"), roomExecutable1.getSlot());
        Assert.assertNotNull(mcu1Id, roomExecutable1.getResourceId());
        RoomExecutable roomExecutable2 = (RoomExecutable) reservation2.getExecutable();
        Assert.assertEquals(Interval.parse("2012-01-01T13:00/2012-01-01T14:00"), roomExecutable2.getSlot());
        Assert.assertNotNull(mcu2Id, roomExecutable2.getResourceId());
        Assert.assertEquals(roomExecutable1.getId(), roomExecutable2.getMigratedExecutable().getId());
    }

    @Test
    public void testRoomMigrationSameDevice() throws Exception
    {
        ConnectTestAgent mcuAgent = getController().addJadeAgent("connect", new ConnectTestAgent());

        DateTime dateTimeStart = DateTime.parse("2012-01-01T12:00");
        DateTime dateTimeEnd = DateTime.parse("2012-01-01T14:00");
        DateTime dateTimeMiddle = dateTimeStart.plus((dateTimeEnd.getMillis() - dateTimeStart.getMillis()) / 2);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.setAllocatable(true);
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connect.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI));
        connect.addCapability(new RecordingCapability());
        connect.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(connect);
        Long mcuPersistenceId = ObjectIdentifier.parse(mcuId).getPersistenceId();

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTimeStart, dateTimeEnd);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.ADOBE_CONNECT));
        String reservationRequestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeStart, Period.months(1)));
        Reservation reservation = checkAllocated(reservationRequestId);
        cz.cesnet.shongo.controller.api.Executable executable = reservation.getExecutable();
        String executableId = executable.getId();
        RecordingService recordingService = getExecutableService(executableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        String recordingServiceId = recordingService.getId();

        // Execute room
        ExecutionResult result = runExecutor(dateTimeStart);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room1 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcuPersistenceId, room1.getResource().getId());
        Assert.assertEquals(5, room1.getLicenseCount());

        // Start recording
        getExecutableService().activateExecutableService(SECURITY_TOKEN, executableId, recordingServiceId);

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, reservationRequestId);
        ((RoomSpecification) reservationRequest.getSpecification()).getAvailability().setParticipantCount(7);
        reservationRequestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(new Interval(dateTimeMiddle, Period.months(1)));
        reservation = checkAllocated(reservationRequestId);
        executable = reservation.getExecutable();
        executableId = executable.getId();
        recordingService = getExecutableService(executableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        recordingServiceId = recordingService.getId();

        // Execute room migration
        result = runExecutor(dateTimeMiddle);
        Assert.assertEquals(1, result.getStoppedExecutables().size());
        Assert.assertEquals(room1.getId(), result.getStoppedExecutables().get(0).getId());
        Assert.assertEquals(1, result.getStartedExecutables().size());
        ResourceRoomEndpoint room2 = (ResourceRoomEndpoint) result.getStartedExecutables().get(0);
        Assert.assertEquals(mcuPersistenceId, room2.getResource().getId());
        Assert.assertEquals(7, room2.getLicenseCount());

        // Start recording (it is already started)
        getExecutableService().activateExecutableService(SECURITY_TOKEN, executableId, recordingServiceId);

        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.ModifyRecordingFolder.class);
                add(ModifyRoom.class);
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
        String mcu1Id = createResource(mcu1);
        Long mcu1PersistenceId = ObjectIdentifier.parse(mcu1Id).getPersistenceId();

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu1");
        mcu2.setAllocatable(true);
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new RoomProviderCapability(10));
        mcu2.setMode(new ManagedMode(mcuAgent2.getName()));
        String mcu2Id = createResource(mcu2);
        Long mcu2PersistenceId = ObjectIdentifier.parse(mcu2Id).getPersistenceId();

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTimeStart, dateTimeEnd);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.setResourceId(mcu1Id);
        roomEstablishment.addTechnology(Technology.H323);
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
                add(CreateRoom.class);
            }}, mcuAgent1.getPerformedCommandClasses());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>(), mcuAgent2.getPerformedCommandClasses());

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        roomSpecification = ((RoomSpecification) reservationRequest.getSpecification());
        roomSpecification.getEstablishment().setResourceId(null);
        roomSpecification.getAvailability().setParticipantCount(7);
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
                add(DeleteRoom.class);
            }}, mcuAgent1.getPerformedCommandClasses());
        Assert.assertEquals(new LinkedList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
            }}, mcuAgent2.getPerformedCommandClasses());
    }
}
