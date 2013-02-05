package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import junitx.framework.Assert;
import org.junit.Test;

import java.util.*;

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
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check created reservation request
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                id);
        assertEquals("request", reservationRequest.getDescription());
        assertEquals(ReservationRequestState.NOT_ALLOCATED, reservationRequest.getState());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequest();
        reservationRequest.setId(id);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check modified reservation request
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                id);
        assertEquals("requestModified", reservationRequest.getDescription());
        assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(id, exception.getEntityId());
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
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("request");
        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check created reservation request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id);
        assertEquals("request", reservationRequest.getDescription());
        assertEquals(1, reservationRequest.getReservationRequests().size());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequestSet();
        reservationRequest.setId(id);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Check modified reservation request
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id);
        assertEquals("requestModified", reservationRequest.getDescription());
        assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(id, exception.getEntityId());
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
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        PermanentReservationRequest reservationRequest = new PermanentReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setResourceId(resourceId);
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check created reservation request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                id);
        assertEquals("request", reservationRequest.getDescription());
        assertEquals(1, reservationRequest.getResourceReservations().size());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        // Modify reservation request by new instance of reservation request
        reservationRequest = new PermanentReservationRequest();
        reservationRequest.setId(id);
        reservationRequest.addSlot("2012-01-01T16:00", "PT2H");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();

        // Check modified reservation request
        reservationRequest = (PermanentReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                id);
        assertEquals("requestModified", reservationRequest.getDescription());
        assertEquals(2, reservationRequest.getResourceReservations().size());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);

        // Check deleted reservation request
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id);
            fail("Reservation request should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(AbstractReservationRequest.class, exception.getEntityType());
            assertEquals(id, exception.getEntityId());
        }
    }

    /**
     * Test listing reservation requests based on {@link Technology} of
     * {@link cz.cesnet.shongo.controller.api.Executable.ResourceRoom} or
     * {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
     *
     * @throws Exception
     */
    @Test
    public void testListReservationRequests() throws Exception
    {
        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new AliasSpecification(AliasType.H323_E164).withValue("001"));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new AliasGroupSpecification(AliasType.SIP_URI));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        ReservationRequest reservationRequest3 = new ReservationRequest();
        reservationRequest3.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest3.setSpecification(
                new RoomSpecification(5, new Technology[]{Technology.ADOBE_CONNECT}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest3);

        ReservationRequestSet reservationRequest4 = new ReservationRequestSet();
        reservationRequest4.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest4.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification3 = new CompartmentSpecification();
        compartmentSpecification3.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 5));
        reservationRequest4.setSpecification(compartmentSpecification3);
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest4);

        ReservationRequest reservationRequest5 = new ReservationRequest();
        reservationRequest5.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest5.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest5.setSpecification(
                new RoomSpecification(5, new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest5);

        ReservationRequestSet reservationRequest6 = new ReservationRequestSet();
        reservationRequest6.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest6.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest6.setSpecification(
                new RoomSpecification(5, new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest6);

        Collection<ReservationRequestSummary> reservationRequests =
                getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals(6, reservationRequests.size());

        Assert.assertEquals(4, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildFilter(new Technology[]{Technology.H323})).size());
        Assert.assertEquals(3, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildFilter(new Technology[]{Technology.SIP})).size());
        Assert.assertEquals(5, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildFilter(new Technology[]{Technology.H323, Technology.SIP})).size());
        Assert.assertEquals(1, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildFilter(new Technology[]{Technology.ADOBE_CONNECT})).size());
    }

    /**
     * @param technologies
     * @return builded filter for {@link ReservationService#listReservationRequests(SecurityToken, java.util.Map)}
     */
    private static Map<String, Object> buildFilter(Technology[] technologies)
    {
        Map<String, Object> filter = new HashMap<String, Object>();
        Set<Technology> filterTechnologies = null;
        if ( technologies != null) {
            filterTechnologies = new HashSet<Technology>();
            for (Technology technology : technologies) {
                filterTechnologies.add(technology);
            }
        }
        filter.put("technology", filterTechnologies);
        return filter;
    }
}
