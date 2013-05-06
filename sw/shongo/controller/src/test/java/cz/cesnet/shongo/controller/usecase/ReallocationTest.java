package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReallocationTest extends AbstractControllerTest
{
    @Test
    public void testExtendAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setMaximumFuture(Period.parse("P1Y"));
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        // In 2013
        setWorkingInterval(Interval.parse("2013-01-01T00:00/2013-02-01T00:00"));
        // Allocate a new alias reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String reservationRequestId = allocate(reservationRequest);
        AliasReservation aliasReservation1 = (AliasReservation) checkAllocated(reservationRequestId);

        // In 2014
        setWorkingInterval(Interval.parse("2014-01-01T00:00/2014-02-01T00:00"));
        // Extend the validity of the alias reservation
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);
        reservationRequest.setSlot("2013-01-01T00:00", "P2Y");
        AliasReservation aliasReservation2 = (AliasReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("Value reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1.getValueReservation().getId(), aliasReservation2.getValueReservation().getId());
        Assert.assertEquals("Alias reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1.getId(), aliasReservation2.getId());
    }

    @Test
    public void testIncreaseRoomCapacity() throws Exception
    {
        DeviceResource multipoint = new DeviceResource();
        multipoint.setName("multipoint");
        multipoint.setAllocatable(true);
        multipoint.addTechnology(Technology.H323);
        multipoint.addCapability(new RoomProviderCapability(5));
        getResourceService().createResource(SECURITY_TOKEN, multipoint);

        // Allocate a new room reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(3, Technology.H323));
        String reservationRequestId = allocate(reservationRequest);
        RoomReservation roomReservation1 = (RoomReservation) checkAllocated(reservationRequestId);

        // Increase the room capacity
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);
        ((RoomSpecification)reservationRequest.getSpecification()).setParticipantCount(5);
        RoomReservation roomReservation2 = (RoomReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("Reservation identifiers should be same (only the room capacity should be increased)",
                roomReservation1.getId(), roomReservation2.getId());
    }

    @Test
    public void testMaintenanceForcesReallocation() throws Exception
    {
        DeviceResource multipoint1 = new DeviceResource();
        multipoint1.setName("multipoint1");
        multipoint1.setAllocatable(true);
        multipoint1.addTechnology(Technology.H323);
        multipoint1.addCapability(new RoomProviderCapability(10));
        String multipoint1Id = getResourceService().createResource(SECURITY_TOKEN, multipoint1);

        DeviceResource multipoint2 = new DeviceResource();
        multipoint2.setName("multipoint2");
        multipoint2.setAllocatable(true);
        multipoint2.addTechnology(Technology.H323);
        multipoint2.addCapability(new RoomProviderCapability(5));
        String multipoint2Id = getResourceService().createResource(SECURITY_TOKEN, multipoint2);

        // Allocate a new room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2013-01-01T12:00", "PT1H");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(3, Technology.H323));
        String roomReservationRequestId = allocate(roomReservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(roomReservationRequestId);

        Assert.assertEquals(multipoint1Id, roomReservation.getResourceId());

        ReservationRequest maintenanceReservationRequest = new ReservationRequest();
        maintenanceReservationRequest.setSlot("2013-01-01T00:00", "P1D");
        maintenanceReservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        maintenanceReservationRequest.setSpecification(new ResourceSpecification(multipoint1Id));
        ResourceReservation maintenanceReservation =
                (ResourceReservation) allocateAndCheck(maintenanceReservationRequest);
        roomReservation = (RoomReservation) getReservationService().getReservation(
                SECURITY_TOKEN, roomReservationRequestId);

        Assert.assertEquals(multipoint1Id, maintenanceReservation.getResourceId());
        Assert.assertEquals(multipoint2Id, roomReservation.getResourceId());
    }
}
