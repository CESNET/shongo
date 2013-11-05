package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for booking recording and streaming services for rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerRoomServicesTest extends AbstractControllerTest
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
        mcu.addCapability(new RecordingCapability());
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
        tcs.addCapability(new RecordingCapability(5));
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
        roomSpecification.addServiceSpecification(EndpointServiceSpecification.createRecording());
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
        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(5));
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
        roomReservationRequest.setSpecification(roomSpecification);
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();

        ReservationRequest recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(EndpointServiceSpecification.createRecording(roomExecutableId));
        allocateAndCheck(recordingReservationRequest);
    }
}
