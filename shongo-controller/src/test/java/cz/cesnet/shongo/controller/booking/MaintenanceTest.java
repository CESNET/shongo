package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.RoomReservation;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot;
import cz.cesnet.shongo.controller.booking.reservation.*;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.room.*;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for {@link AbstractReservationRequest} of {@link ReservationRequestPurpose#MAINTENANCE} type.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MaintenanceTest extends AbstractControllerTest
{
    /**
     * Test create permanent {@link ResourceReservation}s.
     *
     * @throws Exception
     */
    @Test
    public void testCreation() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(SECURITY_TOKEN, resource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot("2012-01-01T00:00", "PT1H", "PT2H", "2012-01-01");
        reservationRequest.addSlot(periodicDateTimeSlot);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runWorker(Interval.parse("2012-01-01T00:00/2012-01-01T08:00"));
        Assert.assertEquals(4, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        runWorker(Interval.parse("2012-01-01T08:00/2012-01-01T16:00"));
        Assert.assertEquals(8, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        runWorker(Interval.parse("2012-01-01T16:00/2012-01-01T23:59"));
        Assert.assertEquals(12, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        runPreprocessorAndScheduler();
        Assert.assertEquals(12, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        List<Reservation> reservations = getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id);
        for (int index = 0; index < 12; index++) {
            ResourceReservation resourceReservation = (ResourceReservation) reservations.get(index);
            Assert.assertEquals(resourceId, resourceReservation.getResourceId());
            String slot = String.format("2012-01-01T%02d:00/2012-01-01T%02d:00", index * 2, index * 2 + 1);
            Assert.assertEquals(Interval.parse(slot).toString(), resourceReservation.getSlot().toString());
        }
    }

    @Test
    public void testPreferenceAndExecutable() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("resource");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(SECURITY_TOKEN, mcu);

        ReservationRequest userRequest = new ReservationRequest();
        userRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        userRequest.setSlot("2012-01-01T00:00", "PT1H");
        userRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        String userRequestId = getReservationService().createReservationRequest(SECURITY_TOKEN, userRequest);

        ReservationRequest ownerRequest = new ReservationRequest();
        ownerRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        ownerRequest.setSlot("2012-01-01T00:00", "PT1H");
        ownerRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        String ownerRequestId = getReservationService().createReservationRequest(SECURITY_TOKEN, ownerRequest);

        runPreprocessorAndScheduler();

        checkAllocated(ownerRequestId);
        checkAllocationFailed(userRequestId);

        List<Reservation> reservations = getReservationService().getReservationRequestReservations(
                SECURITY_TOKEN, ownerRequestId);
        Assert.assertEquals(1, reservations.size());
        RoomReservation roomReservation = (RoomReservation) reservations.get(0);
        Assert.assertEquals(10, roomReservation.getLicenseCount());
        Assert.assertNull(roomReservation.getExecutable());
    }

    /**
     * Test create, update and delete of {@link cz.cesnet.shongo.controller.api.ResourceReservation}s.
     *
     * @throws Exception
     */
    @Test
    public void testModification() throws Exception
    {
        Resource firstResource = new Resource();
        firstResource.setName("firstResource");
        firstResource.setAllocatable(true);
        String firstResourceId = createResource(SECURITY_TOKEN, firstResource);

        Resource secondResource = new Resource();
        secondResource.setName("secondResource");
        secondResource.setAllocatable(true);
        String secondResourceId = createResource(SECURITY_TOKEN, secondResource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.addSlot("2012-01-01T12:00", "PT1H");
        reservationRequest.addSlot("2012-01-02T12:00", "PT1H");
        reservationRequest.setSpecification(new ResourceSpecification(firstResourceId));

        // Create permanent reservation request
        String id = allocate(reservationRequest);

        // Check created reservations
        Assert.assertEquals(2, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        // Remove slot from the request
        reservationRequest = getReservationRequest(id, ReservationRequestSet.class);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        id = allocate(reservationRequest);

        // Check deleted reservation
        Assert.assertEquals(1, getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id).size());

        // Change resource in the request
        reservationRequest = getReservationRequest(id, ReservationRequestSet.class);
        ((ResourceSpecification) reservationRequest.getSpecification()).setResourceId(secondResourceId);
        id = allocate(reservationRequest);

        // Check modified reservation
        List<Reservation> reservations = getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id);
        Assert.assertEquals(1, reservations.size());
        ResourceReservation resourceReservation = (ResourceReservation) reservations.get(0);
        Assert.assertEquals(secondResourceId, resourceReservation.getResourceId());

        // Delete the request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

        // Check deleted reservation
        runScheduler();
        Assert.assertEquals(0, listReservations().size());
    }

    /**
     * Test disabling whole MCU by reservation request of the MCU resource directly (not through virtual room).
     *
     * @throws Exception
     */
    @Test
    public void testRoomProviderDevice() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 2));
        firstReservationRequest.setSpecification(compartmentSpecification);

        String firstReservationRequestId = allocate(firstReservationRequest);
        checkAllocated(firstReservationRequestId);

        ReservationRequestSet secondReservationRequest = new ReservationRequestSet();
        secondReservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        secondReservationRequest.addSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setSpecification(new ResourceSpecification(mcuId));

        String secondReservationRequestId = allocate(secondReservationRequest);
        checkAllocationFailed(secondReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstReservationRequestId);
        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }

    @Test
    public void testPermanentRoomOverMaintenance() throws Exception
    {       /*
        DeviceResource mcu = new DeviceResource();
        mcu.setName("MCU-test");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(50));
        mcu.setAllocatable(true);
        String resourceId = createResource(SECURITY_TOKEN,mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.setSlot("2013-01-02T00:00", "P1D");
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runWorker(Interval.parse("2013-01-01T00:00/2014-01-04T08:00"));
        List<Reservation> reservations = getReservationService().getReservationRequestReservations(SECURITY_TOKEN, id);
        Assert.assertEquals(1, reservations.size());

        // -----------------------------------------------------

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.USER);
        permanentRoomReservationRequest.setSpecification(
                new RoomSpecification(new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        Assert.assertEquals("001", permanentRoomExecutable.getAliasByType(AliasType.H323_E164).getValue());

        // Create one MCU and allocate some virtual rooms on it
        // -----------------------------------------------------
        cz.cesnet.shongo.controller.booking.resource.DeviceResource mcu = new cz.cesnet.shongo.controller.booking.resource.DeviceResource();
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.ADOBE_CONNECT);
        mcu.addCapability(new cz.cesnet.shongo.controller.booking.room.RoomProviderCapability(50));
        mcu.setAllocatable(true);
        createResource(mcu.toApi(createEntityManager()));

        cz.cesnet.shongo.controller.booking.room.RoomReservation room1 = new cz.cesnet.shongo.controller.booking.room.RoomReservation();
        room1.setRoomProviderCapability(mcu.getCapability(cz.cesnet.shongo.controller.api.RoomProviderCapability.class));
        room1.setSlot(DateTime.parse("1"), DateTime.parse("100"));
        room1.setLicenseCount(10);
        createReservation(room1);
        */
    }

}
