package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for common allocations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerCommonTest extends AbstractControllerTest
{
    @Test
    public void testAllocationOnlyToFuture() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new ResourceSpecification(resourceId));
        String request1Id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2011-01-01T00:00", "P1Y");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new ResourceSpecification(resourceId));
        String request2Id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        runScheduler(Interval.parse("2012-07-01/2012-08-01"));

        ResourceReservation resourceReservation = (ResourceReservation) checkAllocated(request1Id);
        Assert.assertEquals("Allocated time slot should be only in future.",
                Interval.parse("2012-07-01/2013-01-01"), resourceReservation.getSlot());

        checkNotAllocated(request2Id);
    }
}
