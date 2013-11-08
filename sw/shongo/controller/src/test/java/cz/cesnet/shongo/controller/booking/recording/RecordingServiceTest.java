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
        DeviceResource mcu = new DeviceResource();
        mcu.setName("connect");
        mcu.addTechnology(Technology.ADOBE_CONNECT);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability());
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(5, Technology.ADOBE_CONNECT));
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
    }

    /**
     * Booking the recording separately from the room.
     *
     * @throws Exception
     */
    @Test
    public void testRoomRecordingAtOnce() throws Exception
    {
        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability(5));
        tcs.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, tcs);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.addServiceSpecification(ExecutableServiceSpecification.createRecording());
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
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
        mcu.setMode(new ManagedMode("mcu"));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(2));
        tcs.setAllocatable(true);
        tcs.setMode(new ManagedMode("tcs"));
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
        cz.cesnet.shongo.controller.api.RecordingService recordingService = roomExecutable.getService(
                cz.cesnet.shongo.controller.api.RecordingService.class);
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
        Assert.assertNotNull(recordingService);
        Assert.assertEquals(tcsId, recordingService.getResourceId());
        Assert.assertNull(recordingService.getRecordingId());

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
            if (command instanceof StopRecording) {
            }
            return result;
        }
    }
}
