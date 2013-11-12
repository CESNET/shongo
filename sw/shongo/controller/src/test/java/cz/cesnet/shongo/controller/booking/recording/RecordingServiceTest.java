package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.jade.recording.StartRecording;
import cz.cesnet.shongo.connector.api.jade.recording.StopRecording;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.RecordingService;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import jade.core.AID;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import java.util.ArrayList;

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

        DateTime dateTime = DateTime.parse("2012-06-22T14:00");

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10));
        connect.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability());
        connect.setAllocatable(true);
        connect.setMode(new ManagedMode(connectAgent.getName()));
        String connectId = getResourceService().createResource(SECURITY_TOKEN, connect);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(5, Technology.ADOBE_CONNECT));
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        RecordingService recordingService = roomExecutable.getService(RecordingService.class);
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
        roomExecutable = (RoomExecutable) getExecutableService().getExecutable(SECURITY_TOKEN, roomExecutableId);
        recordingService = roomExecutable.getService(RecordingService.class);
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(connectId, recordingService.getResourceId());
        Assert.assertNull(recordingService.getRecordingId());

        // Request starting of the service
        roomReservationRequest = (ReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, roomReservationRequestId);
        RoomSpecification roomSpecification = (RoomSpecification) roomReservationRequest.getSpecification();
        roomSpecification.addServiceSpecification(ExecutableServiceSpecification.createRecording());
        roomReservationRequestId = getReservationService().modifyReservationRequest(
                SECURITY_TOKEN, roomReservationRequest);

        // Check execution
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.",
                1, result.getUpdatedExecutables().size());
        Assert.assertEquals("One executable service should be activated.",
                1, result.getActivatedExecutableServices().size());

        // Check executable after execution
        roomExecutable = (RoomExecutable) getExecutableService().getExecutable(SECURITY_TOKEN, roomExecutableId);
        recordingService = roomExecutable.getService(RecordingService.class);
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

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, connectAgent.getPerformedCommandClasses());

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

        DateTime dateTime = DateTime.parse("2012-06-22T14:00");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode(tcsAgent.getName()));
        String tcsId = getResourceService().createResource(SECURITY_TOKEN, tcs);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.addServiceSpecification(ExecutableServiceSpecification.createRecording());
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();
        RecordingService recordingService = roomExecutable.getService(RecordingService.class);
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
        recordingService = roomExecutable.getService(RecordingService.class);
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

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
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

        DateTime dateTime = DateTime.parse("2012-06-22T14:00");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode(tcsAgent.getName()));
        String tcsId = getResourceService().createResource(SECURITY_TOKEN, tcs);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, Period.hours(2));
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        RecordingService recordingService = roomExecutable.getService(RecordingService.class);
        String roomExecutableId = roomExecutable.getId();
        Assert.assertNull(recordingService);

        ReservationRequest recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime, Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(ExecutableServiceSpecification.createRecording(roomExecutableId));
        allocateAndCheck(recordingReservationRequest);

        // Check executable before execution
        roomExecutable = (RoomExecutable) getExecutableService().getExecutable(SECURITY_TOKEN, roomExecutableId);
        recordingService = roomExecutable.getService(RecordingService.class);
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
        recordingService = roomExecutable.getService(RecordingService.class);
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

        // Check performed actions on TCS
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.recording.StartRecording.class);
                add(cz.cesnet.shongo.connector.api.jade.recording.StopRecording.class);
            }}, tcsAgent.getPerformedCommandClasses());

        // Second recording should pass
        recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime, Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(ExecutableServiceSpecification.createRecording(roomExecutableId));
        allocateAndCheck(recordingReservationRequest);

        // Third recording should fail
        recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot(dateTime, Period.hours(1));
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(ExecutableServiceSpecification.createRecording(roomExecutableId));
        allocateAndCheckFailed(recordingReservationRequest);
    }

    /**
     * Testing MCU agent.
     */
    public class TcsTestAgent extends TestAgent
    {
        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            Object result = super.handleCommand(command, sender);
            if (command instanceof StartRecording) {
                return "1";
            }
            else if (command instanceof StopRecording) {
            }
            return result;
        }
    }
}
