package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests for creating, updating and deleting {@link AbstractReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationManagementTest extends AbstractControllerTest
{
    /**
     * Test single reservation request.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setName("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceIdentifier));
        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check created reservation request
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("request", reservationRequest.getName());
        assertEquals(ReservationRequest.State.COMPLETE, reservationRequest.getState());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setName("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequest();
        reservationRequest.setIdentifier(identifier);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check modified reservation request
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("requestModified", reservationRequest.getName());
        assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, identifier);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, identifier);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(Domain.getLocalIdentifier(identifier), exception.getEntityIdentifier());
        }
    }

    /**
     * Test set of reservation requests.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSet() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setName("request");
        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSpecification(new ResourceSpecification(resourceIdentifier));
        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check created reservation request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("request", reservationRequest.getName());
        assertEquals(1, reservationRequest.getReservationRequests().size());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setName("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequestSet();
        reservationRequest.setIdentifier(identifier);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check modified reservation request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("requestModified", reservationRequest.getName());
        assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, identifier);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, identifier);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(Domain.getLocalIdentifier(identifier), exception.getEntityIdentifier());
        }
    }

    /**
     * Test set of reservation requests.
     *
     * @throws Exception
     */
    @Test
    public void testPermanentReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, resource);

        PermanentReservationRequest reservationRequest = new PermanentReservationRequest();
        reservationRequest.setName("request");
        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setResourceIdentifier(resourceIdentifier);
        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check created reservation request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("request", reservationRequest.getName());
        assertEquals(1, reservationRequest.getResourceReservations().size());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setName("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new PermanentReservationRequest();
        reservationRequest.setIdentifier(identifier);
        reservationRequest.addSlot("2012-01-01T16:00", "PT2H");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check modified reservation request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                identifier);
        assertEquals("requestModified", reservationRequest.getName());
        assertEquals(2, reservationRequest.getResourceReservations().size());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, identifier);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, identifier);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(Domain.getLocalIdentifier(identifier), exception.getEntityIdentifier());
        }
    }
}
