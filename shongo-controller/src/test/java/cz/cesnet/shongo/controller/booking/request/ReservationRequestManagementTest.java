package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.CompartmentSpecification;
import cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSet;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest}s.
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
        String resourceId = createResource(resource);

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
        reservationRequest = getReservationRequest(id1, ReservationRequest.class);
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
        reservationRequest = getReservationRequest(id2, ReservationRequest.class);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request again
        reservationRequest = getReservationRequest(id2, ReservationRequest.class);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id3, reservationRequests.getItem(0).getId());
        reservationRequest = getReservationRequest(id3, ReservationRequest.class);
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
        String resourceId = createResource(resource);

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
        reservationRequest = getReservationRequest(id1, ReservationRequestSet.class);
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
        reservationRequest = getReservationRequest(id2, ReservationRequestSet.class);
        Assert.assertEquals("requestModified", reservationRequest.getDescription());

        // Modify reservation request again
        reservationRequest = getReservationRequest(id2, ReservationRequestSet.class);
        reservationRequest.setPurpose(ReservationRequestPurpose.EDUCATION);
        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        // Check modified reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        Assert.assertEquals(id3, reservationRequests.getItem(0).getId());
        reservationRequest = getReservationRequest(id3, ReservationRequestSet.class);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequest.getPurpose());

        // Delete reservation request
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, id3);

        // Check deleted reservation request
        reservationRequests = getReservationService().listReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("No reservation request should exist.", 0, reservationRequests.getItemCount());
    }

    /**
     * Test set of reservation requests as it was in runtime (run the preprocessor each hour).
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSetRuntime() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);


        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("request");
        reservationRequest.addSlot(new PeriodicDateTimeSlot(
                DateTime.parse("2012-01-02T14:30"), Period.parse("PT4H"),
                Period.parse("P1W"), LocalDate.parse("2012-01-29")));
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        reservationRequest.setReusement(ReservationRequestReusement.OWNED);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        Preprocessor.Result preprocessorResult = runPreprocessor(new Interval(
                DateTime.parse("2012-01-01T00:00:00"),
                DateTime.parse("2012-01-02T15:00:00")));
        Scheduler.Result schedulerResult = runScheduler();
        Assert.assertEquals(1, preprocessorResult.getCreatedReservationRequests());
        Assert.assertEquals(1, schedulerResult.getAllocatedReservationRequests());

        preprocessorResult = runPreprocessor(new Interval(
                DateTime.parse("2012-01-02T15:00:00"),
                DateTime.parse("2012-01-02T16:00:00")));
        schedulerResult = runScheduler();
        Assert.assertTrue(preprocessorResult.isEmpty());
        Assert.assertTrue(schedulerResult.isEmpty());

        for (int hour = 16; hour <= 20; hour++) {
            preprocessorResult = runPreprocessor(new Interval(
                    DateTime.parse("2012-01-01T00:00:00"),
                    DateTime.parse("2012-01-02T" + hour + ":00:00")));
            schedulerResult = runScheduler();
            Assert.assertTrue(preprocessorResult.isEmpty());
            Assert.assertTrue(schedulerResult.isEmpty());
        }
    }

    /**
     * Test set of reservation requests for permanent room capacity as it was in runtime (run the preprocessor each hour).
     *
     * @throws Exception
     */
    @Test
    public void testReservationRequestSetRuntimePermanentRoomCapacity() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("request");
        reservationRequest.addSlot(new PeriodicDateTimeSlot(
                DateTime.parse("2012-01-02T14:30"), Period.parse("PT4H"),
                Period.parse("P1W"), LocalDate.parse("2012-01-29")));
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5, Technology.H323);
        roomSpecification.addParticipant(new PersonParticipant("test", "martin.srom@cesnet.cz"));
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        reservationRequest.setReusement(ReservationRequestReusement.OWNED);
        reservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId);
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);

        Preprocessor.Result preprocessorResult = runPreprocessor(new Interval(
                DateTime.parse("2012-01-01T00:00:00"),
                DateTime.parse("2012-01-02T15:00:00")));
        Scheduler.Result schedulerResult = runScheduler();
        Assert.assertEquals(1, preprocessorResult.getCreatedReservationRequests());
        Assert.assertEquals(1, schedulerResult.getAllocatedReservationRequests());

        preprocessorResult = runPreprocessor(new Interval(
                DateTime.parse("2012-01-02T15:00:00"),
                DateTime.parse("2012-01-02T16:00:00")));
        schedulerResult = runScheduler();
        Assert.assertTrue(preprocessorResult.isEmpty());
        Assert.assertTrue(schedulerResult.isEmpty());

        for (int hour = 16; hour <= 20; hour++) {
            preprocessorResult = runPreprocessor(new Interval(
                    DateTime.parse("2012-01-01T00:00:00"),
                    DateTime.parse("2012-01-02T" + hour + ":00:00")));
            schedulerResult = runScheduler();
            Assert.assertTrue(preprocessorResult.isEmpty());
            Assert.assertTrue(schedulerResult.isEmpty());
        }
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
        String mcuId = createResource(mcu);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSlot(new PeriodicDateTimeSlot("2012-01-01T00:00", "PT1H", "P1W", "2012-01-01"));
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 3));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = allocate(reservationRequest);
        checkAllocated(id);

        reservationRequest = getReservationRequest(id, ReservationRequestSet.class);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
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
        createResource(mcu);

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
        Assert.assertEquals(9, roomSpecification.getAvailability().getParticipantCount());
        Assert.assertEquals(1, roomSpecification.getEstablishment().getTechnologies().size());

        reservationRequestSet = (ReservationRequestSet) service.getReservationRequest(SECURITY_TOKEN, id2);
        Assert.assertEquals(ReservationRequestPurpose.EDUCATION, reservationRequestSet.getPurpose());
        Assert.assertEquals("description2", reservationRequestSet.getDescription());
        Assert.assertEquals(RoomSpecification.class, reservationRequestSet.getSpecification().getClass());
        roomSpecification = ((RoomSpecification) reservationRequestSet.getSpecification());
        Assert.assertEquals(10, roomSpecification.getAvailability().getParticipantCount());
        Assert.assertEquals(1, roomSpecification.getEstablishment().getTechnologies().size());

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
        Assert.assertEquals(ReservationRequestSummary.SpecificationType.ROOM,
                response.getItem(0).getSpecificationType());
        Assert.assertEquals(ReservationRequestSummary.SpecificationType.ALIAS,
                response.getItem(1).getSpecificationType());
        Assert.assertEquals(ReservationRequestSummary.SpecificationType.ALIAS,
                response.getItem(2).getSpecificationType());
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
        compartmentSpecification3.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 5));
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
        String resourceId = createResource(resource);

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
        createResource(resource);

        Interval interval = new Interval(DateTime.now(), Period.years(1));
        Object result;

        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecification = new cz.cesnet.shongo.controller.api.AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.setValue("test");

        AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest(SECURITY_TOKEN);
        availabilityCheckRequest.addSlot(interval);
        availabilityCheckRequest.setSpecification(aliasSpecification);

        result = getReservationService().checkPeriodicAvailability(availabilityCheckRequest);
        Assert.assertEquals(Boolean.TRUE, result);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(interval);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(aliasSpecification);
        allocateAndCheck(reservationRequest);

        result = getReservationService().checkPeriodicAvailability(availabilityCheckRequest);
        Assert.assertEquals(AllocationStateReport.class, result.getClass());
    }

    @Test
    public void testReservationRequestReusementAclEntryPropagation() throws Exception
    {
        ReservationService service = getReservationService();

        String user2Id = getUserId(SECURITY_TOKEN_USER2);

        Set<ObjectPermission> NONE = new HashSet<ObjectPermission>();
        Set<ObjectPermission> READ = new HashSet<ObjectPermission>()
        {{
                add(ObjectPermission.READ);
            }};

        // Check ReservationRequestReusement.ARBITRARY
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

        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1, new AclEntry(user2Id, reservationRequest1Id, ObjectRole.READER));
        Assert.assertEquals("For ReservationRequestReusement.ARBITRARY the AclEntries should not be propagated",
                NONE, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest2Id));

        // Check ReservationRequestReusement.OWNED
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

        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1, new AclEntry(user2Id, reservationRequest3Id,
                ObjectRole.READER));
        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclEntries should be propagated",
                READ, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest4Id));

        ReservationRequest reservationRequest5 = new ReservationRequest();
        reservationRequest5.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest5.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest5.setSpecification(new RoomSpecification(1, Technology.H323));
        reservationRequest5.setReusedReservationRequestId(reservationRequest3Id);
        String reservationRequest5Id = service.createReservationRequest(SECURITY_TOKEN_USER1, reservationRequest5);

        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclEntries should be propagated to new request",
                READ, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5Id));

        deleteAclEntry(user2Id, reservationRequest3Id, ObjectRole.READER);
        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclEntry deletion should be also propagated",
                NONE, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest4Id));
        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclEntry deletion should be also propagated",
                NONE, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5Id));

        // Check ReservationRequestReusement.OWNED for modified request
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1, new AclEntry(user2Id, reservationRequest3Id,
                ObjectRole.READER));
        Assert.assertEquals(READ, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5Id));

        String reservationRequest5IdOld = reservationRequest5Id;
        reservationRequest5 = (ReservationRequest) service.getReservationRequest(
                SECURITY_TOKEN_USER1, reservationRequest5Id);
        reservationRequest5Id = service.modifyReservationRequest(SECURITY_TOKEN_USER1, reservationRequest5);
        Assert.assertEquals(READ, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5Id));
        Assert.assertEquals(READ, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5IdOld));

        deleteAclEntry(user2Id, reservationRequest3Id, ObjectRole.READER);
        Assert.assertEquals(
                "For ReservationRequestReusement.OWNED the deletion should be also propagated for modified request",
                NONE, listObjectPermissions(SECURITY_TOKEN_USER2, reservationRequest5Id));
    }

    @Test
    public void testExecutableReusementAclEntryPropagation() throws Exception
    {
        ReservationService service = getReservationService();

        String user2Id = getUserId(SECURITY_TOKEN_USER2);

        Set<ObjectPermission> NONE = new HashSet<ObjectPermission>();
        Set<ObjectPermission> READ = new HashSet<ObjectPermission>()
        {{
                add(ObjectPermission.READ);
            }};

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        // Check ReservationRequestReusement.ARBITRARY
        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(SECURITY_TOKEN_USER1, permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId = service.createReservationRequest(
                SECURITY_TOKEN_USER1, capacityReservationRequest);

        getAuthorizationService().createAclEntry(
                SECURITY_TOKEN_USER1, new AclEntry(user2Id, permanentRoomReservationRequestId, ObjectRole.READER));
        Assert.assertEquals("For ReservationRequestReusement.ARBITRARY the AclEntries should not be propagated",
                NONE, listObjectPermissions(SECURITY_TOKEN_USER2, capacityReservationRequestId));

        // Check ReservationRequestReusement.OWNED
        permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(1, Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        permanentRoomReservationRequestId = allocate(SECURITY_TOKEN_USER1, permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        capacityReservationRequestId = service.createReservationRequest(SECURITY_TOKEN_USER1, capacityReservationRequest);

        getAuthorizationService().createAclEntry(
                SECURITY_TOKEN_USER1, new AclEntry(user2Id, permanentRoomReservationRequestId, ObjectRole.READER));
        Assert.assertEquals("For ReservationRequestReusement.OWNED the AclEntries should be propagated",
                READ, listObjectPermissions(SECURITY_TOKEN_USER2, capacityReservationRequestId));
    }

    @Test
    public void testListOwnedResourcesReservationRequests()
    {
        Resource ownedResource = new Resource();
        ownedResource.setName("resource");
        ownedResource.setAllocatable(true);
        ownedResource.setConfirmByOwner(true);
        String ownedResourceId = createResource(ownedResource);

        AclEntry aclEntry = new AclEntry();
        aclEntry.setRole(ObjectRole.OWNER);
        aclEntry.setIdentityPrincipalId(getUserId(SECURITY_TOKEN));
        aclEntry.setIdentityType(AclIdentityType.USER);
        aclEntry.setObjectId(ownedResourceId);
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_ROOT, aclEntry);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(ownedResourceId));
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        ReservationRequest reservationRequestLonger = new ReservationRequest();
        reservationRequestLonger.setDescription("request");
        reservationRequestLonger.setSlot("2012-01-01T11:00", "PT3H");
        reservationRequestLonger.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestLonger.setSpecification(new ResourceSpecification(ownedResourceId));
        String id2 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestLonger);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        ReservationRequest reservationRequestAnother = new ReservationRequest();
        reservationRequestAnother.setDescription("request");
        reservationRequestAnother.setSlot("2012-01-01T01:00", "PT1H");
        reservationRequestAnother.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestAnother.setSpecification(new ResourceSpecification(ownedResourceId));
        String id3 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestAnother);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        ListResponse<ReservationRequestSummary> reservationRequests;
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest(SECURITY_TOKEN);
        reservationRequestListRequest.setAllocationState(AllocationState.CONFIRM_AWAITING);
        reservationRequestListRequest.setIntervalDateOnly(false);
        reservationRequestListRequest.setInterval(reservationRequest.getSlot());

        // Check created reservation request without existing reservation, waiting for confirmation
        reservationRequests = getReservationService().listOwnedResourcesReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("Two reservation request should exist.", 2, reservationRequests.getItemCount());
        Assert.assertEquals(id1, reservationRequests.getItem(0).getId());
        reservationRequest = getReservationRequest(id1, ReservationRequest.class);
        Assert.assertEquals("request", reservationRequest.getDescription());
        Assert.assertEquals(AllocationState.CONFIRM_AWAITING, reservationRequest.getAllocationState());
    }

    @Test
    public void testReservationRequestConfirmation() throws Exception
    {
        Resource ownedResource = new Resource();
        ownedResource.setName("resource");
        ownedResource.setAllocatable(true);
        ownedResource.setConfirmByOwner(true);
        String ownedResourceId = createResource(ownedResource);

        AclEntry aclEntry = new AclEntry();
        aclEntry.setRole(ObjectRole.OWNER);
        aclEntry.setIdentityPrincipalId(getUserId(SECURITY_TOKEN));
        aclEntry.setIdentityType(AclIdentityType.USER);
        aclEntry.setObjectId(ownedResourceId);
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_ROOT, aclEntry);

        // Create reservation request requires confirmation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(ownedResourceId));
        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);

        ReservationRequest reservationRequestLonger = new ReservationRequest();
        reservationRequestLonger.setDescription("request");
        reservationRequestLonger.setSlot("2012-01-01T11:00", "PT3H");
        reservationRequestLonger.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestLonger.setSpecification(new ResourceSpecification(ownedResourceId));
        String id2 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestLonger);
        Assert.assertEquals("shongo:cz.cesnet:req:2", id2);

        ReservationRequest reservationRequestAnother = new ReservationRequest();
        reservationRequestAnother.setDescription("request");
        reservationRequestAnother.setSlot("2012-01-01T01:00", "PT1H");
        reservationRequestAnother.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestAnother.setSpecification(new ResourceSpecification(ownedResourceId));
        String id3 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequestAnother);
        Assert.assertEquals("shongo:cz.cesnet:req:3", id3);

        ListResponse<ReservationRequestSummary> reservationRequests;
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest(SECURITY_TOKEN);
        reservationRequestListRequest.setAllocationState(AllocationState.CONFIRM_AWAITING);

        // Check created reservation request without existing reservation, waiting for confirmation
        reservationRequests = getReservationService().listOwnedResourcesReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("Tree reservation requests should exist.", 3, reservationRequests.getItemCount());
        Assert.assertEquals(id1, reservationRequests.getItem(0).getId());
        reservationRequest = getReservationRequest(id1, ReservationRequest.class);
        Assert.assertEquals("request", reservationRequest.getDescription());
        Assert.assertEquals(AllocationState.CONFIRM_AWAITING, reservationRequest.getAllocationState());

        // Confirm reservation request and check if allocated
        getReservationService().confirmReservationRequest(SECURITY_TOKEN, id1, true);

        reservationRequests = getReservationService().listOwnedResourcesReservationRequests(reservationRequestListRequest);
        Assert.assertEquals("One reservation request should exist.", 1, reservationRequests.getItemCount());
        runScheduler();
        checkAllocated(id1);
    }

//    @Test
//    public void testReservationRequestSetConfirmation() throws Exception
//    {
//        Resource ownedResource = new Resource();
//        ownedResource.setName("resource");
//        ownedResource.setAllocatable(true);
//        ownedResource.setConfirmByOwner(true);
//        String ownedResourceId = createResource(ownedResource);
//
//        AclEntry aclEntry = new AclEntry();
//        aclEntry.setRole(ObjectRole.OWNER);
//        aclEntry.setIdentityPrincipalId(getUserId(SECURITY_TOKEN));
//        aclEntry.setIdentityType(AclIdentityType.USER);
//        aclEntry.setObjectId(ownedResourceId);
//        getAuthorizationService().createAclEntry(SECURITY_TOKEN_ROOT, aclEntry);
//
//        // Create reservation request requires confirmation
//        ReservationRequestSet reservationRequest = new ReservationRequestSet();
//        reservationRequest.setDescription("request");
//        reservationRequest.addSlot("2012-01-01T12:00", "PT2H");
//        reservationRequest.addSlot("2012-01-02T12:00", "PT2H");
//        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
//        reservationRequest.setSpecification(new ResourceSpecification(ownedResourceId));
//        String id1 = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
//        Assert.assertEquals("shongo:cz.cesnet:req:1", id1);
//
//        runPreprocessor();
//
//        ListResponse<ReservationRequestSummary> reservationRequests;
//        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest(SECURITY_TOKEN);
//        reservationRequestListRequest.setAllocationState(AllocationState.CONFIRM_AWAITING);
//
//        // Check created reservation request without existing reservation, waiting for confirmation
//        reservationRequests = getReservationService().listOwnedResourcesReservationRequests(reservationRequestListRequest);
//        Assert.assertEquals("Two reservation requests should exist.", 2, reservationRequests.getItemCount());
//        String reqId1 = reservationRequests.getItem(0).getId();
//        String reqId2 = reservationRequests.getItem(1).getId();
//
//        // Confirm reservation request and check if allocated
//        getReservationService().confirmReservationRequest(SECURITY_TOKEN, reqId1, true);
//        getReservationService().denyReservationRequest(SECURITY_TOKEN, reqId2);
//
//        runScheduler();
//
//        reservationRequests = getReservationService().listOwnedResourcesReservationRequests(reservationRequestListRequest);
//        Assert.assertEquals("No reservation request should exist.", 0, reservationRequests.getItemCount());
//        checkAllocated(reqId1);
//        checkAllocationFailed(reqId2);
//    }

    private Set<ObjectPermission> listObjectPermissions(SecurityToken securityToken, String objectId)
    {
        return getAuthorizationService().listObjectPermissions(new ObjectPermissionListRequest(
                securityToken, objectId)).get(objectId).getObjectPermissions();
    }
}
