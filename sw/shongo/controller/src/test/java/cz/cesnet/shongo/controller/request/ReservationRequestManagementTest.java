package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.CompartmentSpecification;
import cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSet;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.PermissionListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Tests for creating, updating and deleting {@link AbstractReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestManagementTest extends AbstractControllerTest
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

        ListResponse<ReservationRequestSummary> reservationRequests;
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest(SECURITY_TOKEN);

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id1, reservationRequests.getItem(0).getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals("request", reservationRequest.getDescription());
        Assert.assertEquals(AllocationState.NOT_ALLOCATED, reservationRequest.getAllocationState());

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
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id2, reservationRequests.getItem(0).getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request again
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id3, reservationRequests.getItem(0).getId());
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id3);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("No reservation request should exist.", 0, reservationRequests.getItemCount());
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

        ListResponse<ReservationRequestSummary> reservationRequests;
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest(SECURITY_TOKEN);

        // Check created reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id1, reservationRequests.getItem(0).getId());
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
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id2, reservationRequests.getItem(0).getId());
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request again
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id2);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id3, reservationRequests.getItem(0).getId());
        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id3);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id3);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("No reservation request should exist.", 0, reservationRequests.getItemCount());
    }

    /**
     * Test modify {@link CompartmentSpecification} to {@link RoomSpecification} and delete the request.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSetModification() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSlot(new PeriodicDateTimeSlot("2012-01-01T00:00", "PT1H", "P1W", "2012-01-01"));
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();
        runScheduler();
        checkAllocated(id);

        reservationRequest = (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, id);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);
        id = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id);
    }

    /**
     * Test modify {@link ReservationRequest} to {@link ReservationRequestSet} and delete the request.
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestModification() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationService service = getReservationService();

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setDescription("description1");
        reservationRequest.setSlot("2012-01-01T12:00/2012-01-01T13:00");
        reservationRequest.setSpecification(new RoomSpecification(9, Technology.H323));
        String id1 = allocate(SECURITY_TOKEN, reservationRequest);
        checkAllocated(id1);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setId(id1);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequestSet.setDescription("description2");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-01-01T12:00", "PT1H", "P1W", "2012-01-01"));
        reservationRequestSet.setSpecification(new RoomSpecification(10, Technology.H323));
        String id2 = allocate(SECURITY_TOKEN, reservationRequestSet);
        checkAllocated(id2);

        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, id1);
        Assert.assertEquals(ReservationRequestPurpose.SCIENCE, reservationRequest.getPurpose());
        Assert.assertEquals("description1", reservationRequest.getDescription());
        RoomSpecification roomSpecification = ((RoomSpecification) reservationRequest.getSpecification());
        Assert.assertEquals(Integer.valueOf(9), roomSpecification.getParticipantCount());
        Assert.assertEquals(1, roomSpecification.getTechnologies().size());

        reservationRequestSet = (ReservationRequestSet) service.getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequestSet.getPurpose());
        Assert.assertEquals("description2", reservationRequestSet.getDescription());
        Assert.assertEquals(RoomSpecification.class, reservationRequestSet.getSpecification().getClass());
        roomSpecification = ((RoomSpecification) reservationRequestSet.getSpecification());
        Assert.assertEquals(Integer.valueOf(10), roomSpecification.getParticipantCount());
        Assert.assertEquals(1, roomSpecification.getTechnologies().size());

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id2);

        ListResponse<ReservationRequestSummary> reservationRequests =
                getReservationService().listReservationRequests(new ReservationRequestListRequest(SECURITY_TOKEN));
        Assert.assertEquals("No reservation request should exist.", 0, reservationRequests.getItemCount());
    }

    /**
     * Test listing reservation requests.
     *
     * @throws Exception
     */
    @Test
    public void testListReservationRequests() throws Exception
    {
        for (int index = 0; index < 9; index++) {
            String number = String.valueOf(index + 1);
            ReservationRequest request = new ReservationRequest();
            request.setDescription("request " + number);
            request.setSlot("2012-01-01T12:00", "PT2H");
            request.setPurpose(ReservationRequestPurpose.SCIENCE);
            switch (index % 3) {
                case 0:
                    request.setSpecification(new RoomSpecification(5, Technology.H323));
                    break;
                case 1:
                    request.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME)
                            .withValue("room " + number));
                    break;
                case 2:
                    AliasSetSpecification specification = new AliasSetSpecification();
                    specification.addAlias(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.H323_E164)
                            .withValue(number));
                    specification.addAlias(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME)
                            .withValue("room " + number));
                    request.setSpecification(specification);
                    break;
            }
            getReservationService().createReservationRequest(SECURITY_TOKEN, request);
        }

        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(SECURITY_TOKEN);
        request.setStart(1);
        request.setCount(5);

        ListResponse<ReservationRequestSummary> response;

        response = getReservationService().listReservationRequests(request);
        Assert.assertEquals(5, response.getItemCount());
        Assert.assertEquals("request 2", response.getItem(0).getDescription());

        request.setStart(0);
        request.setCount(null);
        response = getReservationService().listReservationRequests(request);
        Assert.assertEquals(9, response.getItemCount());
        Assert.assertEquals(ReservationRequestSummary.RoomSpecification.class,
                response.getItem(0).getSpecification().getClass());
        Assert.assertEquals(ReservationRequestSummary.AliasSpecification.class,
                response.getItem(1).getSpecification().getClass());
        Assert.assertEquals(ReservationRequestSummary.AliasSpecification.class,
                response.getItem(2).getSpecification().getClass());
        ReservationRequestSummary.AliasSpecification a1 =
                (ReservationRequestSummary.AliasSpecification) response.getItem(1).getSpecification();
        ReservationRequestSummary.AliasSpecification a2 =
                (ReservationRequestSummary.AliasSpecification) response.getItem(1).getSpecification();
        Assert.assertEquals(AliasType.ROOM_NAME, a1.getAliasType());
        Assert.assertEquals(AliasType.ROOM_NAME, a2.getAliasType());
    }

    /**
     * Test listing reservation requests based on {@link Technology} of
     * {@link cz.cesnet.shongo.controller.api.AliasSpecification},
     * {@link AliasSetSpecification},
     * {@link RoomSpecification} or
     * {@link CompartmentSpecification}.
     *
     * @throws Exception
     */
    @Test
    public void testListReservationRequestsByTechnology() throws Exception
    {
        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(
                new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.H323_E164).withValue("001"));
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
                new RoomSpecification(5,
                        new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest5);

        ReservationRequestSet reservationRequest6 = new ReservationRequestSet();
        reservationRequest6.addSlot("2012-01-01T12:00", "PT2H");
        reservationRequest6.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest6.setSpecification(
                new RoomSpecification(5,
                        new Technology[]{Technology.H323, Technology.SIP}));
        getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest6);

        ListResponse<ReservationRequestSummary> reservationRequests =
                getReservationService().listReservationRequests(new ReservationRequestListRequest(SECURITY_TOKEN));
        Assert.assertEquals(6, reservationRequests.getItemCount());

        Assert.assertEquals(4, getReservationService().listReservationRequests(new ReservationRequestListRequest(
                SECURITY_TOKEN, new Technology[]{Technology.H323})).getItemCount());
        Assert.assertEquals(3, getReservationService().listReservationRequests(new ReservationRequestListRequest(
                SECURITY_TOKEN, new Technology[]{Technology.SIP})).getItemCount());
        Assert.assertEquals(5, getReservationService().listReservationRequests(new ReservationRequestListRequest(
                SECURITY_TOKEN, new Technology[]{Technology.H323, Technology.SIP})).getItemCount());
        Assert.assertEquals(1, getReservationService().listReservationRequests(new ReservationRequestListRequest(
                SECURITY_TOKEN, new Technology[]{Technology.ADOBE_CONNECT})).getItemCount());
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
            reservationRequest.setSpecification(
                    new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
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
        params.add(new HashMap<String, Object>()
        {{
                put("securityToken", SECURITY_TOKEN.getAccessToken());
            }});
        @SuppressWarnings("unchecked")
        ListResponse<ReservationRequestSummary> result = (ListResponse<ReservationRequestSummary>)
                getControllerClient().execute("Reservation.listReservationRequests", params);
        Interval slot1 = result.getItem(0).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot1.getStart());
        Assert.assertEquals(Temporal.DATETIME_INFINITY_END, slot1.getEnd());
        Interval slot2 = result.getItem(1).getEarliestSlot();
        Assert.assertEquals(Temporal.DATETIME_INFINITY_START, slot2.getStart());
        Assert.assertThat(Temporal.DATETIME_INFINITY_END, is(not(slot2.getEnd())));
        Interval slot3 = result.getItem(2).getEarliestSlot();
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

        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecification = new cz.cesnet.shongo.controller.api.AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.setValue("test");

        AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest(SECURITY_TOKEN);
        availabilityCheckRequest.setSlot(interval);
        availabilityCheckRequest.setSpecification(aliasSpecification);

        result = getReservationService().checkAvailability(availabilityCheckRequest);
        Assert.assertEquals(Boolean.TRUE, result);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(interval);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(aliasSpecification);
        allocateAndCheck(reservationRequest);

        result = getReservationService().checkAvailability(availabilityCheckRequest);
        Assert.assertEquals(AllocationStateReport.class, result.getClass());

        try {
            availabilityCheckRequest.setSpecification(new RoomSpecification(1, Technology.H323));
            getReservationService().checkAvailability(availabilityCheckRequest);
            Assert.fail("Room specification should not be able to be checked for availability for now.");
        }
        catch (RuntimeException exception) {
            Assert.assertTrue(exception.getMessage().contains(
                    "Specification 'RoomSpecification' cannot be checked for availability"));
        }
    }

    @Test
    public void testReusementAclRecordPropagation() throws Exception
    {
        ReservationService service = getReservationService();

        String user2Id = getUserId(SECURITY_TOKEN_USER2);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest1.setReusement(ReservationRequestReusement.ARBITRARY);
        String reservationRequest1Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest2.setReusedReservationRequestId(reservationRequest1Id);
        String reservationRequest2Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest2);

        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequest1Id, Role.READER);
        Assert.assertEquals("For ReservationRequestReusement.ARBITRARY the AclRecords should not be propagated",
                new HashSet<Permission>()
                {{
                    }}, getAuthorizationService().listPermissions(new PermissionListRequest(
                SECURITY_TOKEN_USER2, reservationRequest2Id)).get(reservationRequest2Id).getPermissions());

        ReservationRequest reservationRequest3 = new ReservationRequest();
        reservationRequest3.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest3.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest3.setReusement(ReservationRequestReusement.OWNED);
        String reservationRequest3Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest3);

        ReservationRequest reservationRequest4 = new ReservationRequest();
        reservationRequest4.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest4.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest4.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest4.setReusedReservationRequestId(reservationRequest3Id);
        String reservationRequest4Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest4);

        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequest3Id, Role.READER);
        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclRecords should be propagated",
                new HashSet<Permission>()
                {{
                        add(Permission.READ);
                    }}, getAuthorizationService().listPermissions(new PermissionListRequest(
                SECURITY_TOKEN_USER2, reservationRequest4Id)).get(reservationRequest4Id).getPermissions());

        ReservationRequest reservationRequest5 = new ReservationRequest();
        reservationRequest5.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest5.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest5.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest5.setReusedReservationRequestId(reservationRequest3Id);
        String reservationRequest5Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest5);

        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclRecords should be propagated to new request",
                new HashSet<Permission>()
                {{
                        add(Permission.READ);
                    }}, getAuthorizationService().listPermissions(new PermissionListRequest(
                SECURITY_TOKEN_USER2, reservationRequest5Id)).get(reservationRequest5Id).getPermissions());
    }
}
