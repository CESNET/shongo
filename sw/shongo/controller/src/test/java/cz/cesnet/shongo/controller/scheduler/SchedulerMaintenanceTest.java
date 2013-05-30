package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests for {@link AbstractReservationRequest} of {@link ReservationRequestPurpose#MAINTENANCE} type.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerMaintenanceTest extends AbstractControllerTest
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
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.addSlot(new PeriodicDateTimeSlot("2012-01-01T00:00", "PT1H", "PT2H", "2012-01-01"));
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        Map<String, Object> reservationFilter = new HashMap<String, Object>();
        reservationFilter.put("reservationRequestId", id);

        runWorker(Interval.parse("2012-01-01T00:00/2012-01-01T08:00"));
        Assert.assertEquals(4, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runWorker(Interval.parse("2012-01-01T08:00/2012-01-01T16:00"));
        Assert.assertEquals(8, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runWorker(Interval.parse("2012-01-01T16:00/2012-01-01T23:59"));
        Assert.assertEquals(12, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runPreprocessorAndScheduler();
        Assert.assertEquals(12, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        List<Reservation> reservations = new ArrayList<Reservation>(
                getReservationService().listReservations(SECURITY_TOKEN, reservationFilter));
        Collections.sort(reservations, new Comparator<Reservation>()
        {
            @Override
            public int compare(Reservation o1, Reservation o2)
            {
                return o1.getSlot().getStart().compareTo(o2.getSlot().getStart());
            }
        });
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
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSlot("2012-01-01T00:00", "PT1H");
        reservationRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        String reservationRequestId =
                getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        ReservationRequest maintenanceReservationRequest = new ReservationRequest();
        maintenanceReservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        maintenanceReservationRequest.setSlot("2012-01-01T00:00", "PT1H");
        maintenanceReservationRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        String maintenanceReservationRequestId =
                getReservationService().createReservationRequest(SECURITY_TOKEN, maintenanceReservationRequest);

        runPreprocessorAndScheduler();

        checkAllocated(maintenanceReservationRequestId);
        checkAllocationFailed(reservationRequestId);

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("reservationRequestId", maintenanceReservationRequestId);
        Collection<Reservation> reservations = getReservationService().listReservations(SECURITY_TOKEN, filter);
        Assert.assertEquals(1, reservations.size());
        RoomReservation roomReservation = (RoomReservation) reservations.iterator().next();
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
        String firstResourceId = getResourceService().createResource(SECURITY_TOKEN, firstResource);

        Resource secondResource = new Resource();
        secondResource.setName("secondResource");
        secondResource.setAllocatable(true);
        String secondResourceId = getResourceService().createResource(SECURITY_TOKEN, secondResource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.addSlot("2012-01-01T12:00", "PT1H");
        reservationRequest.addSlot("2012-01-02T12:00", "PT1H");
        reservationRequest.setSpecification(new ResourceSpecification(firstResourceId));

        // Create permanent reservation request
        String reservationRequestId = allocate(reservationRequest);

        Map<String, Object> reservationFilter = new HashMap<String, Object>();
        reservationFilter.put("reservationRequestId", reservationRequestId);

        // Check created reservations
        Assert.assertEquals(2, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        // Remove slot from the request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        reservationRequestId = allocate(reservationRequest);

        // Check deleted reservation
        Assert.assertEquals(1, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        // Change resource in the request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);
        ((ResourceSpecification) reservationRequest.getSpecification()).setResourceId(secondResourceId);
        reservationRequestId = allocate(reservationRequest);

        // Check modified reservation
        Collection<Reservation> reservations = getReservationService()
                .listReservations(SECURITY_TOKEN, reservationFilter);
        Assert.assertEquals(1, reservations.size());
        ResourceReservation resourceReservation = (ResourceReservation) reservations.iterator().next();
        Assert.assertEquals(secondResourceId, resourceReservation.getResourceId());

        // Delete the request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);

        // Check deleted reservation
        runScheduler();
        Assert.assertEquals(0, getReservationService().listReservations(SECURITY_TOKEN, null).size());
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
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
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
        runScheduler();

        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }
}
