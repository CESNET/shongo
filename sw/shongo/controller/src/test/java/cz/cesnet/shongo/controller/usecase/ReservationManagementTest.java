package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

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
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        Collection<ReservationRequestSummary> reservationRequests;

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id1 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals("request", reservationRequest.getDescription());
        Assert.assertEquals(ReservationRequestState.NOT_ALLOCATED, reservationRequest.getState());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        String id2 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        // Check already modified reservation request
        try {
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception that reservation request has already been modified should be thrown.");
        }
        catch (ControllerReportSet.ReservationRequestAlreadyModifiedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }

        // Check modified
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id2 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequest();
        reservationRequest.setId(id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id3 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id1);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("No reservation request should exist.", 0 , reservationRequests.size());
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
            Assert.fail("Reservation request should not exist.");
        }
        catch (ControllerReportSet.ReservationRequestDeletedException exception) {
            Assert.assertEquals(id1, exception.getId());
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
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        Collection<ReservationRequestSummary> reservationRequests;

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id1 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals("request", reservationRequest.getDescription());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequest.setDescription("requestModified");
        String id2 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        // Check already modified reservation request
        try {
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception that reservation request has already been modified should be thrown.");
        }
        catch (ControllerReportSet.ReservationRequestAlreadyModifiedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }

        // Check modified
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id2 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request by new instance of reservation request
        reservationRequest = new ReservationRequestSet();
        reservationRequest.setId(id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("One reservation request should exist.", 1 , reservationRequests.size());
        Assert.assertEquals(id3 , reservationRequests.iterator().next().getId());
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id1);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(SECURITY_TOKEN, null);
        Assert.assertEquals("No reservation request should exist.", 0 , reservationRequests.size());
        try {
            getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
            Assert.fail("Reservation request should not exist.");
        }
        catch (ControllerReportSet.ReservationRequestDeletedException exception) {
            Assert.assertEquals(id1, exception.getId());
        }
    }

    /**
     * Test listing reservation requests based on {@link Technology} of
     * {@link cz.cesnet.shongo.controller.api.AliasSpecification},
     * {@link cz.cesnet.shongo.controller.api.AliasSetSpecification},
     * {@link cz.cesnet.shongo.controller.api.RoomSpecification} or
     * {@link cz.cesnet.shongo.controller.api.CompartmentSpecification}.
     *
     * @throws Exception
     */
    @Test
    public void testListReservationRequestsByTechnology() throws Exception
    {
        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new AliasSpecification(AliasType.H323_E164).withValue("001"));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new AliasSetSpecification(AliasType.SIP_URI));
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
                buildTechnologyFilter(new Technology[]{Technology.H323})).size());
        Assert.assertEquals(3, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.SIP})).size());
        Assert.assertEquals(5, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.H323, Technology.SIP})).size());
        Assert.assertEquals(1, getReservationService().listReservationRequests(SECURITY_TOKEN,
                buildTechnologyFilter(new Technology[]{Technology.ADOBE_CONNECT})).size());
    }

    /**
     * @param technologies
     * @return built filter for {@link ReservationService#listReservationRequests(SecurityToken, java.util.Map)}
     */
    private static Map<String, Object> buildTechnologyFilter(Technology[] technologies)
    {
        Map<String, Object> filter = new HashMap<String, Object>();
        Set<Technology> filterTechnologies = null;
        if (technologies != null) {
            filterTechnologies = new HashSet<Technology>();
            for (Technology technology : technologies) {
                filterTechnologies.add(technology);
            }
        }
        filter.put("technology", filterTechnologies);
        return filter;
    }

    /**
     * Test reservation request for infinite start/end/whole interval
     *
     * @throws Exception
     */
    @Test
    public void testSlotDuration() throws Exception
    {
        try {
            ReservationRequest reservationRequest = new ReservationRequest();
            reservationRequest.setDescription("request");
            reservationRequest.setSlot("2012-01-01T12:00", "PT0S");
            reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
            getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
            Assert.fail("Exception of empty duration should has been thrown.");
        }
        catch (ControllerReportSet.ReservationRequestEmptyDurationException exception) {
        }
    }

    /**
     * Test reservation request for infinite start/end/whole interval
     *
     * @throws Exception
     */
    @Test
    public void testInfiniteReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot(Temporal.INTERVAL_INFINITE);
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot(Temporal.DATETIME_INFINITY_START, DateTime.parse("2012-01-01"));
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        ReservationRequest reservationRequest3 = new ReservationRequest();
        reservationRequest3.setSlot(DateTime.parse("2012-01-01"), Temporal.DATETIME_INFINITY_END);
        reservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest3.setSpecification(new ResourceSpecification(resourceId));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest3);

        List<Object> params = new ArrayList<Object>();
        params.add(SECURITY_TOKEN.getAccessToken());
        params.add(null);

        Object[] result = (Object[]) getControllerClient().execute("Reservation.listReservationRequests", params);
        Interval slot1 = ((ReservationRequestSummary) result[0]).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot1.getStart());
        Assert.assertEquals(Temporal.DATETIME_INFINITY_END, slot1.getEnd());
        Interval slot2 = ((ReservationRequestSummary) result[1]).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot2.getStart());
        Assert.assertThat(Temporal.DATETIME_INFINITY_END, is(not(slot2.getEnd())));
        Interval slot3 = ((ReservationRequestSummary) result[2]).getEarliestSlot();
        Assert.assertThat(Temporal.DATETIME_INFINITY_START, is(not(slot3.getStart())));
        Assert.assertEquals(Temporal.DATETIME_INFINITY_END, slot3.getEnd());
    }

    @Test
    public void testCheckSpecificationAvailability() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        resource.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, resource);

        Interval interval = Interval.parse("2012-01-01/2012-12-31");
        Object result;

        AliasSpecification aliasSpecification = new AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.setValue("test");

        result = getReservationService().checkSpecificationAvailability(SECURITY_TOKEN, aliasSpecification, interval);
        Assert.assertEquals(Boolean.TRUE, result);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(interval);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(aliasSpecification);
        allocateAndCheck(reservationRequest);

        result = getReservationService().checkSpecificationAvailability(SECURITY_TOKEN, aliasSpecification, interval);
        Assert.assertEquals(String.class, result.getClass());

        try {
            getReservationService().checkSpecificationAvailability(SECURITY_TOKEN,
                    new RoomSpecification(1, Technology.H323), interval);
            Assert.fail("Room specification should not be able to be checked for availability for now.");
        }
        catch (RuntimeException exception) {
        }
    }
}
