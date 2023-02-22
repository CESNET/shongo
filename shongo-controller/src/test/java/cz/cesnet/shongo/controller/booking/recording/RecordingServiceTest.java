package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AdobeConnectRoomSetting;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.RecordingService;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.api.RecordingServiceSpecification;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import jade.core.AID;
import org.junit.Assert;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import java.util.*;

/**
 * Tests for booking recording and streaming services for rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingServiceTest extends AbstractExecutorTest
{
    /**
     * Booking the recording separately from the room.
     *
     * @throws Exception
     */
    @Test
    public void testAlwaysRecordableRoom() throws Exception
    {
        ConnectTestAgent connectAgent = getController().addJadeAgent("connect", new ConnectTestAgent());

        DateTime dateTime = DateTime.now().minusMinutes(30);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connect.addCapability(new AliasProviderCapability("{hash}@cesnet.cz", AliasType.ADOBE_CONNECT_URI));
        connect.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability());
        connect.setAllocatable(true);
        connect.setMode(new ManagedMode(connectAgent.getName()));
        String connectId = createResource(connect);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5, Technology.ADOBE_CONNECT);
        AdobeConnectRoomSetting roomSetting = new AdobeConnectRoomSetting();
        roomSetting.setPin("1234");
        roomSpecification.addRoomSetting(roomSetting);
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        RecordingService recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull("Recording service should be allocated.", recordingService);
        Assert.assertEquals("Connect should be allocated as recording device", connectId, recordingService.getResourceId());
        Assert.assertFalse("Recording should not be active", recordingService.isActive());
        Assert.assertNull("Recording should not be recorded", recordingService.getRecordingId());

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("None executable service should be activated.",
                0, result.getActivatedExecutableServices().size());

        // Check executable after execution
        recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(connectId, recordingService.getResourceId());
        Assert.assertNull(recordingService.getRecordingId());

        // Request starting of the service
        roomReservationRequest = getReservationRequest(roomReservationRequestId, ReservationRequest.class);
        roomSpecification = (RoomSpecification) roomReservationRequest.getSpecification();
        roomSpecification.getAvailability().addServiceSpecification(new RecordingServiceSpecification(true));
        roomReservationRequestId = allocate(roomReservationRequest, dateTime.plusHours(1));
        roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        roomExecutableId = roomExecutable.getId();

        // Check execution
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Check executable after execution
        recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(connectId, recordingService.getResourceId());
        Assert.assertTrue(recordingService.isActive());
        Assert.assertNotNull(recordingService.getRecordingId());

        // Check execution
        result = runExecutor(dateTime.plusHours(2));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be deactivated.",
                1, result.getDeactivatedExecutableServices().size());

        // Create new ACL for request
        getAuthorizationService().createAclEntry(SECURITY_TOKEN,
                new AclEntry(getUserId(SECURITY_TOKEN_USER2), roomReservationRequestId, ObjectRole.READER));
        runExecutor(dateTime.plusHours(3));

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        runScheduler();
        runExecutor(dateTime.plusHours(3));

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(DeleteRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.ModifyRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.DeleteRecordingFolder.class);
            }}, connectAgent.getPerformedCommandClasses());

        cz.cesnet.shongo.connector.api.jade.recording.StartRecording startRecording =
                connectAgent.getPerformedCommand(5, cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
        Assert.assertEquals("1234", startRecording.getRecordingSettings().getPin());
    }

    /**
     * Booking the recording separately from the room.
     *
     * @throws Exception
     */
    @Test
    public void testRoomRecordingAtOnce() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());
        TcsTestAgent tcsAgent = getController().addJadeAgent("tcs", new TcsTestAgent());

        DateTime dateTime = DateTime.now().minusMinutes(30);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.H323_E164}));
        mcu.addCapability(new AliasProviderCapability("{digit:9}", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode(tcsAgent.getName()));
        String tcsId = createResource(tcs);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        RecordingService recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull("Recording service should be allocated.", recordingService);
        Assert.assertEquals("TCS should be allocated as recording device", tcsId, recordingService.getResourceId());
        Assert.assertFalse("Recording should not be active", recordingService.isActive());
        Assert.assertNull("Recording should not be recorded", recordingService.getRecordingId());

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Check executable after execution
        roomExecutable = (RoomExecutable) getExecutableService().getExecutable(SECURITY_TOKEN, roomExecutableId);
        recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(tcsId, recordingService.getResourceId());
        Assert.assertTrue(recordingService.isActive());
        Assert.assertNotNull(recordingService.getRecordingId());

        // Check execution
        result = runExecutor(dateTime.plusHours(2));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be deactivated.",
                1, result.getDeactivatedExecutableServices().size());

        // Check performed actions on MCU
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
        Assert.assertEquals(6, mcuAgent.getPerformedCommand(1,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(5, mcuAgent.getPerformedCommand(2,
                ModifyRoom.class).getRoom().getLicenseCount());

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
            }}, tcsAgent.getPerformedCommandClasses());
    }

    /**
     * Booking the recording separately from the room.
     *
     * @throws Exception
     */
    @Test
    public void testRoomRecordingSeparately() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());
        TcsTestAgent tcsAgent = getController().addJadeAgent("tcs", new TcsTestAgent());

        DateTime dateTime = DateTime.now().minusMinutes(30);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.H323_E164}));
        mcu.addCapability(new AliasProviderCapability("{digit:9}", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(1));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode(tcsAgent.getName()));
        String tcsId = createResource(tcs);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        RecordingService recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNull(recordingService);

        ReservationRequest recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime, Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(
                RecordingServiceSpecification.forExecutable(roomExecutableId, true));
        allocateAndCheck(recordingReservationRequest);

        // Check executable before execution
        recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull("Recording service should be allocated.", recordingService);
        Assert.assertEquals("TCS should be allocated as recording device", tcsId, recordingService.getResourceId());
        Assert.assertFalse("Recording should not be active", recordingService.isActive());
        Assert.assertNull("Recording should not be recorded", recordingService.getRecordingId());

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Check executable after execution
        recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(tcsId, recordingService.getResourceId());
        Assert.assertTrue(recordingService.isActive());
        Assert.assertNotNull(recordingService.getRecordingId());

        // Another recording
        recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime.plusHours(1), Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(
                RecordingServiceSpecification.forExecutable(roomExecutableId, true));
        allocateAndCheck(recordingReservationRequest);

        // Check execution
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable service should be deactivated.",
                1, result.getDeactivatedExecutableServices().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Check execution
        result = runExecutor(dateTime.plusHours(2));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be deactivated.",
                1, result.getDeactivatedExecutableServices().size());

        // Check performed actions on MCU
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
        Assert.assertEquals(6, mcuAgent.getPerformedCommand(1,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(5, mcuAgent.getPerformedCommand(2,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(6, mcuAgent.getPerformedCommand(3,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(5, mcuAgent.getPerformedCommand(4,
                ModifyRoom.class).getRoom().getLicenseCount());

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
            }}, tcsAgent.getPerformedCommandClasses());

        // Second recording in the same time should fail
        recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime, Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(
                RecordingServiceSpecification.forExecutable(roomExecutableId, true));
        allocateAndCheckFailed(recordingReservationRequest);

        // Check recordings
        ExecutableRecordingListRequest request = new ExecutableRecordingListRequest();
        request.setSecurityToken(SECURITY_TOKEN);
        request.setExecutableId(roomExecutableId);
        ListResponse<ResourceRecording> recordings = getExecutableService().listExecutableRecordings(request);
        Assert.assertEquals(2, recordings.getItemCount());
    }

    /**
     * Test modification of room when it has started allocated recording.
     *
     * @throws Exception
     */
    @Test
    public void testModifyRoomWithActiveAllocatedRecording() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());
        TcsTestAgent tcsAgent = getController().addJadeAgent("tcs", new TcsTestAgent());

        DateTime dateTime = DateTime.now();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.H323_E164}));
        mcu.addCapability(new AliasProviderCapability("{digit:9}", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode(tcsAgent.getName()));
        String tcsId = createResource(tcs);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Modify reservation request
        roomReservationRequest = getReservationRequest(roomReservationRequestId, ReservationRequest.class);
        roomReservationRequestId = allocate(roomReservationRequest, dateTime.plusHours(1));
        checkAllocated(roomReservationRequestId);

        // Check execution
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("No executable service should be activated.",
                0, result.getActivatedExecutableServices().size());
        Assert.assertEquals("No executable service should be deactivated.",
                0, result.getDeactivatedExecutableServices().size());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        runScheduler(dateTime.plusHours(2));

        // Stop recording and delete room
        result = runExecutor(dateTime.plusHours(2));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be stopped.",
                1, result.getDeactivatedExecutableServices().size());
        // Finalize room
        runExecutor(dateTime.plusHours(3));

        // Check performed actions on MCU
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
        Assert.assertEquals(5, mcuAgent.getPerformedCommand(0,
                CreateRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(6, mcuAgent.getPerformedCommand(1,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(6, mcuAgent.getPerformedCommand(2,
                ModifyRoom.class).getRoom().getLicenseCount());
        Assert.assertEquals(5, mcuAgent.getPerformedCommand(3,
                ModifyRoom.class).getRoom().getLicenseCount());

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.ModifyRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.DeleteRecordingFolder.class);
            }}, tcsAgent.getPerformedCommandClasses());
    }

    /**
     * Test stopping of automatic {@link RecordingService} when {@link ReservationRequest} for {@link RoomExecutable}
     * is deleted before end of time slot.
     *
     * @throws Exception
     */
    @Test
    public void testStoppingRecordingWhenReservationRequestDeleted() throws Exception
    {
        ConnectTestAgent connectAgent = getController().addJadeAgent("connect", new ConnectTestAgent());

        DateTime dateTime = DateTime.now().minusMinutes(30);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connect.addCapability(new AliasProviderCapability("{hash}@cesnet.cz", AliasType.ADOBE_CONNECT_URI));
        connect.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability());
        connect.setAllocatable(true);
        connect.setMode(new ManagedMode(connectAgent.getName()));
        createResource(connect);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.minutes(120));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(5, Technology.ADOBE_CONNECT));
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        String recordingServiceId = getExecutableService(roomExecutableId, RecordingService.class).getId();

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("None executable service should be activated.",
                0, result.getActivatedExecutableServices().size());

        // Start recording
        getExecutableService().activateExecutableService(SECURITY_TOKEN, roomExecutableId, recordingServiceId);

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        runScheduler(dateTime.plusHours(1));

        // Stop recording and delete room
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be stopped.",
                1, result.getDeactivatedExecutableServices().size());

        // Finalize
        runExecutor(dateTime.plusHours(2));

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(DeleteRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.DeleteRecordingFolder.class);
            }}, connectAgent.getPerformedCommandClasses());
    }

    /**
     * Test stopping of allocated {@link RecordingService} when {@link ReservationRequest} for {@link RoomExecutable}
     * is deleted before end of time slot.
     *
     * @throws Exception
     */
    @Test
    public void testStoppingAllocatedRecordingWhenReservationRequestDeleted() throws Exception
    {
        ConnectTestAgent connectAgent = getController().addJadeAgent("connect", new ConnectTestAgent());

        DateTime dateTime = DateTime.now().minusMinutes(30);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connect.addCapability(new AliasProviderCapability("{hash}@cesnet.cz", AliasType.ADOBE_CONNECT_URI));
        connect.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability(1));
        connect.setAllocatable(true);
        connect.setMode(new ManagedMode(connectAgent.getName()));
        createResource(connect);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.minutes(120));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.ADOBE_CONNECT);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        String recordingServiceId = getExecutableService(roomExecutableId, RecordingService.class).getId();

        // Check execution
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.",
                1, result.getStartedExecutables().size());
        Assert.assertEquals("None executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        runScheduler(dateTime.plusHours(1));

        // Stop recording and delete room
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be stopped.",
                1, result.getStoppedExecutables().size());
        Assert.assertEquals("One executable service should be stopped.",
                1, result.getDeactivatedExecutableServices().size());

        // Finalize
        runExecutor(dateTime.plusHours(2));

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.GetActiveRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.IsRecordingActive.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.DeleteRecordingFolder.class);
            }}, connectAgent.getPerformedCommandClasses());
    }

    /**
     * Testing MCU agent.
     */
    public class TcsTestAgent extends RecordableTestAgent
    {
        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException
        {
            return super.handleCommand(command, sender);
        }
    }
}
