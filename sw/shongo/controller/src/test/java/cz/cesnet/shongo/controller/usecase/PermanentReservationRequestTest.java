package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link PermanentReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermanentReservationRequestTest extends AbstractControllerTest
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

        PermanentReservationRequest reservationRequest = new PermanentReservationRequest();
        reservationRequest.addSlot(new DateTimeSlot(
                new PeriodicDateTime("2012-01-01T00:00", "PT2H", "2012-01-01"), Period.parse("PT1H")));
        reservationRequest.setResourceId(resourceId);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Map<String, Object> reservationFilter = new HashMap<String, Object>();
        reservationFilter.put("reservationRequestId", id);

        runPreprocessor(Interval.parse("2012-01-01T00:00/2012-01-01T08:00"));
        assertEquals(4, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runPreprocessor(Interval.parse("2012-01-01T08:00/2012-01-01T16:00"));
        assertEquals(8, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runPreprocessor(Interval.parse("2012-01-01T16:00/2012-01-01T23:59"));
        assertEquals(12, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        runPreprocessor();
        assertEquals(12, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

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
            assertEquals(resourceId, resourceReservation.getResourceId());
            String slot = String.format("2012-01-01T%02d:00/2012-01-01T%02d:00", index * 2, index * 2 + 1);
            assertEquals(Interval.parse(slot).toString(), resourceReservation.getSlot().toString());
        }
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

        PermanentReservationRequest reservationRequest = new PermanentReservationRequest();
        reservationRequest.addSlot("2012-01-01T12:00", "PT1H");
        reservationRequest.addSlot("2012-01-02T12:00", "PT1H");
        reservationRequest.setResourceId(firstResourceId);

        // Create permanent reservation request
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        Map<String, Object> reservationFilter = new HashMap<String, Object>();
        reservationFilter.put("reservationRequestId", id);

        // Check created reservations
        runPreprocessor();
        assertEquals(2, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        // Remove slot from the request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, id);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check deleted reservation
        runPreprocessor();
        assertEquals(1, getReservationService().listReservations(SECURITY_TOKEN, reservationFilter).size());

        // Change resource in the request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, id);
        reservationRequest.setResourceId(secondResourceId);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check modified reservation
        runPreprocessor();
        Collection<Reservation> reservations = getReservationService().listReservations(SECURITY_TOKEN, reservationFilter);
        assertEquals(1, reservations.size());
        ResourceReservation resourceReservation = (ResourceReservation) reservations.iterator().next();
        assertEquals(secondResourceId, resourceReservation.getResourceId());

        // Delete the request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

        // Check deleted reservation
        runScheduler();
        assertEquals(0, getReservationService().listReservations(SECURITY_TOKEN, null).size());
    }

    /**
     * Test disabling whole MCU by reservation request of the MCU resource directly (not though virtual room).
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
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
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

        PermanentReservationRequest secondReservationRequest = new PermanentReservationRequest();
        secondReservationRequest.addSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setResourceId(mcuId);

        String secondReservationRequestId = allocate(secondReservationRequest);
        runPreprocessor();
        checkAllocationFailed(secondReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstReservationRequestId);
        runScheduler();

        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }
}
