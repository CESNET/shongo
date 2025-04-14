package cz.cesnet.shongo.controller.booking.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AuxiliaryData;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for reallocation of reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModificationTest extends AbstractControllerTest
{

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testModifyAttributes() throws JsonProcessingException {
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
        ReservationRequest reservationRequestGet = getReservationRequest(id1, ReservationRequest.class);

        Assert.assertEquals(ReservationRequestType.NEW, reservationRequestGet.getType());
        Assert.assertEquals(reservationRequest.getPurpose(), reservationRequestGet.getPurpose());
        Assert.assertEquals(reservationRequest.getDescription(), reservationRequestGet.getDescription());
        Assert.assertEquals(reservationRequest.getInterDomain(), reservationRequestGet.getInterDomain());
        Assert.assertEquals(reservationRequest.getReusedReservationRequestId(), reservationRequestGet.getReusedReservationRequestId());
        Assert.assertEquals(ReservationRequestReusement.NONE, reservationRequestGet.getReusement());
        Assert.assertEquals(reservationRequest.getAuxData(), reservationRequestGet.getAuxData());
        Assert.assertEquals(reservationRequest.getParentReservationRequestId(), reservationRequestGet.getParentReservationRequestId());
        Assert.assertEquals(reservationRequest.getSlot(), reservationRequestGet.getSlot());
        Assert.assertEquals(reservationRequest.getReservationIds(), reservationRequestGet.getReservationIds());

        // Modify reservation request by retrieved instance of reservation request
        reservationRequestGet.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequestGet.setPriority(5);
        reservationRequestGet.setDescription("requestModified");
        reservationRequestGet.setSpecification(new AliasSpecification(Technology.ADOBE_CONNECT));
        reservationRequestGet.setReusement(ReservationRequestReusement.OWNED);
        List<AuxiliaryData> auxData = List.of(
                new AuxiliaryData("tag1", true, objectMapper.readTree("[\"karnis@cenet.cz\", \"filip.karnis@cesnet.cz\"]")),
                new AuxiliaryData("tag2", false, objectMapper.readTree("[\"shouldnotbe@used\"]")),
                new AuxiliaryData("tag3", true, null)
        );
        reservationRequestGet.setAuxData(auxData);
        reservationRequestGet.setSlot("2012-01-01T13:00", "PT1H");

        String id2 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequestGet);
        ReservationRequest reservationRequestGet2 = getReservationRequest(id2, ReservationRequest.class);

        Assert.assertEquals(ReservationRequestType.MODIFIED, reservationRequestGet2.getType());
        Assert.assertEquals(reservationRequestGet.getPurpose(), reservationRequestGet2.getPurpose());
        Assert.assertEquals(reservationRequestGet.getPriority(), reservationRequestGet2.getPriority());
        Assert.assertEquals(reservationRequestGet.getDescription(), reservationRequestGet2.getDescription());
        Assert.assertEquals(reservationRequestGet.getInterDomain(), reservationRequestGet2.getInterDomain());
        Assert.assertEquals(reservationRequestGet.getReusedReservationRequestId(), reservationRequestGet2.getReusedReservationRequestId());
        Assert.assertEquals(reservationRequestGet.getReusement(), reservationRequestGet2.getReusement());
        Assert.assertEquals(reservationRequestGet.getAuxData(), reservationRequestGet2.getAuxData());
        Assert.assertEquals(reservationRequestGet.getParentReservationRequestId(), reservationRequestGet2.getParentReservationRequestId());
        Assert.assertEquals(reservationRequestGet.getSlot(), reservationRequestGet2.getSlot());
        Assert.assertEquals(reservationRequestGet.getAllocationState(), reservationRequestGet2.getAllocationState());
        Assert.assertEquals(reservationRequestGet.getReservationIds(), reservationRequestGet2.getReservationIds());

        // Modify again
        reservationRequestGet2.setReusedReservationRequestId(id2);

        String id3 = getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequestGet2);
        ReservationRequest reservationRequestGet3 = getReservationRequest(id3, ReservationRequest.class);

        // Check that reused reservation request points to id3 since id2 was modified to id3
        Assert.assertEquals(id3, reservationRequestGet3.getReusedReservationRequestId());
    }

    @Test
    public void testExtension() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);

        ReservationService service = getReservationService();

        // Allocate reservation for 2012
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "2013-01-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01/2012-02-01"));

        ResourceReservation resourceReservation = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-01-01/2013-01-01"), resourceReservation.getSlot());

        // Extend reservation for 2013
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        reservationRequest.setSlot("2012-01-01T00:00", "2014-01-01T00:00");
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-07-01/2012-08-01"));

        resourceReservation = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-07-01/2014-01-01"), resourceReservation.getSlot());

        // Extend reservation for 2014
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        reservationRequest.setSlot("2012-01-01T00:00", "2015-01-01T00:00");
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2013-07-01/2013-08-01"));

        resourceReservation = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2013-07-01/2015-01-01"), resourceReservation.getSlot());

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(3, reservationIds.size());
        Reservation reservation1 = service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01/2012-07-01"), reservation1.getSlot());
        Reservation reservation2 = service.getReservation(SECURITY_TOKEN, reservationIds.get(1));
        Assert.assertEquals(Interval.parse("2012-07-01/2013-07-01"), reservation2.getSlot());
        Reservation reservation3 = service.getReservation(SECURITY_TOKEN, reservationIds.get(2));
        Assert.assertEquals(Interval.parse("2013-07-01/2015-01-01"), reservation3.getSlot());
    }

    @Test
    public void testShortening() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);

        ReservationService service = getReservationService();

        // Allocate reservation from 2012 to 2013
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "2014-01-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01/2012-02-01"));

        ResourceReservation resourceReservation = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-01-01/2014-01-01"), resourceReservation.getSlot());

        // Shorten the reservation to only 2012
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        reservationRequest.setSlot("2012-01-01T00:00", "2013-01-01T00:00");
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-07-01/2012-08-01"));

        resourceReservation = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-07-01/2013-01-01"), resourceReservation.getSlot());

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(2, reservationIds.size());
        Reservation reservation1 = service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01/2012-07-01"), reservation1.getSlot());
        Reservation reservation2 = service.getReservation(SECURITY_TOKEN, reservationIds.get(1));
        Assert.assertEquals(Interval.parse("2012-07-01/2013-01-01"), reservation2.getSlot());
    }

    @Test
    public void testModificationOfFutureReservation() throws Exception
    {
        Resource resource1 = new Resource();
        resource1.setName("resource");
        resource1.setAllocatable(true);
        String resource1Id = createResource(resource1);

        Resource resource2 = new Resource();
        resource2.setName("resource");
        resource2.setAllocatable(true);
        String resource2Id = createResource(resource2);

        ReservationService service = getReservationService();

        // Allocate reservation for 2013
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-02-01T00:00", "2012-03-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resource1Id));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01/2012-03-01"));

        ResourceReservation resourceReservation1 = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-02-01/2012-03-01"), resourceReservation1.getSlot());
        Assert.assertEquals(resource1Id, resourceReservation1.getResourceId());

        // Modify the reservation
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        ((ResourceSpecification) reservationRequest.getSpecification()).setResourceId(resource2Id);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-02/2012-03-02"));

        ResourceReservation resourceReservation2 = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-02-01/2012-03-01"), resourceReservation2.getSlot());
        Assert.assertEquals(resource2Id, resourceReservation2.getResourceId());
        Assert.assertFalse(resourceReservation1.getId().equals(resourceReservation2.getId()));

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(1, reservationIds.size());
        Reservation reservation = service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(resourceReservation2.getId(), reservation.getId());
    }

    @Test
    public void testModificationOfActiveReservation() throws Exception
    {
        Resource resource1 = new Resource();
        resource1.setName("resource");
        resource1.setAllocatable(true);
        String resource1Id = createResource(resource1);

        Resource resource2 = new Resource();
        resource2.setName("resource");
        resource2.setAllocatable(true);
        String resource2Id = createResource(resource2);

        ReservationService service = getReservationService();

        // Allocate reservation for 2013
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "2012-02-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resource1Id));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01/2012-02-01"));

        ResourceReservation resourceReservation1 = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-01-01/2012-02-01"), resourceReservation1.getSlot());
        Assert.assertEquals(resource1Id, resourceReservation1.getResourceId());

        // Modify the reservation
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        ((ResourceSpecification) reservationRequest.getSpecification()).setResourceId(resource2Id);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-15/2012-02-15"));

        ResourceReservation resourceReservation2 = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-01-15/2012-02-01"), resourceReservation2.getSlot());
        Assert.assertEquals(resource2Id, resourceReservation2.getResourceId());
        Assert.assertFalse(resourceReservation1.getId().equals(resourceReservation2.getId()));

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(2, reservationIds.size());
        Reservation reservation1 = service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01/2012-01-15"), reservation1.getSlot());
        Reservation reservation2 = service.getReservation(SECURITY_TOKEN, reservationIds.get(1));
        Assert.assertEquals(Interval.parse("2012-01-15/2012-02-01"), reservation2.getSlot());
    }

    @Test
    public void testModificationOfPastReservation() throws Exception
    {
        Resource resource1 = new Resource();
        resource1.setName("resource");
        resource1.setAllocatable(true);
        String resource1Id = createResource(resource1);

        Resource resource2 = new Resource();
        resource2.setName("resource");
        resource2.setAllocatable(true);
        String resource2Id = createResource(resource2);

        ReservationService service = getReservationService();

        // Allocate reservation for 2013
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "2012-02-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resource1Id));
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01/2012-02-01"));

        ResourceReservation resourceReservation1 = (ResourceReservation) checkAllocated(requestId);
        Assert.assertEquals(Interval.parse("2012-01-01/2012-02-01"), resourceReservation1.getSlot());
        Assert.assertEquals(resource1Id, resourceReservation1.getResourceId());

        // Modify the reservation
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        ((ResourceSpecification) reservationRequest.getSpecification()).setResourceId(resource2Id);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2013-01-01/2013-02-01"));

        checkNotAllocated(requestId);

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(1, reservationIds.size());
        Reservation reservation = service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01/2012-02-01"), reservation.getSlot());
    }

    @Test
    public void testPermanentRoomWithCapacityModification() throws Exception
    {
        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAllocatable(true);
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(
                new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME, AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}"));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ROOM_NAME));
        createResource(connectServer);

        // Allocate a new permanent room reservation
        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addAliasSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        roomEstablishment.addAliasSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        permanentRoomReservationRequest.setSpecification(roomSpecification);
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        // Allocate a permanent room capacity
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        reservationRequest.setSpecification(new RoomSpecification(5));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        // Increase room capacity
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        ((RoomSpecification) reservationRequest.getSpecification()).getAvailability().setParticipantCount(10);
        allocateAndCheck(reservationRequest);
    }

    @Test
    public void testRoomModificationSameName() throws Exception
    {
        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.setAllocatable(true);
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME}));
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, new ValueProvider.Pattern("{hash}")));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}@cesnet.cz"));
        connect.addCapability(aliasProviderCapability);
        createResource(connect);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(3, Technology.ADOBE_CONNECT));
        String reservationRequestId = allocate(reservationRequest);
        RoomReservation roomReservation = (RoomReservation) checkAllocated(reservationRequestId);
        RoomExecutable roomExecutable = (RoomExecutable) roomReservation.getExecutable();
        String roomName = roomExecutable.getAliasByType(AliasType.ROOM_NAME).getValue();

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        RoomSpecification roomSpecification = (RoomSpecification) reservationRequest.getSpecification();
        roomSpecification.getEstablishment().addAliasSpecification(
                new AliasSpecification(AliasType.ROOM_NAME, roomName));
        allocateAndCheck(reservationRequest);
    }

    /*@Test
    public void testValueModification() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("{number:0:100}").withAllowedAnyRequestedValue());
        String valueProviderId = createResource(valueProvider);

        // Allocate value
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2014-01-01T00:00", "2014-06-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ValueSpecification(valueProviderId, "1"));
        String reservationRequestId = allocate(reservationRequest);
        ValueReservation valueReservation1 = (ValueReservation) checkAllocated(reservationRequestId);

        Assert.assertEquals("Value should be allocated.", "1", valueReservation1.getValue());

        // Modify allocated value
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSpecification(new ValueSpecification(valueProviderId, "2"));
        ValueReservation valueReservation2 = (ValueReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("The same (only modified) reservation should be allocated.",
                valueReservation1.getId(), valueReservation2.getId());
        Assert.assertEquals("Modified value should be allocated.", "2", valueReservation2.getValue());
    }

    @Test
    public void testValueExtension() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("{number:0:100}").withAllowedAnyRequestedValue());
        String valueProviderId = createResource(valueProvider);

        // Allocate a value (#1)
        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2014-01-01T00:00", "2014-06-01T00:00");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new ValueSpecification(valueProviderId));
        String reservationRequest1Id = allocate(reservationRequest1);
        ValueReservation valueReservation1 = (ValueReservation) checkAllocated(reservationRequest1Id);

        Assert.assertEquals("First number from range should be allocated.", "0", valueReservation1.getValue());

        // Allocate same value (#2) before
        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2013-01-01T00:00", "2013-06-01T00:00");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new ValueSpecification(valueProviderId));
        String reservationRequest2Id = allocate(reservationRequest2);
        ValueReservation valueReservation2_1 = (ValueReservation) checkAllocated(reservationRequest2Id);

        Assert.assertEquals("Same number should be allocated.", "0", valueReservation2_1.getValue());

        // Extend #2 to not intersect #1
        reservationRequest2 = getReservationRequest(reservationRequest2Id, ReservationRequest.class);
        reservationRequest2.setSlot("2013-01-01T00:00", "2014-01-01T00:00");
        ValueReservation valueReservation2_2 = (ValueReservation) allocateAndCheck(reservationRequest2);

        Assert.assertEquals("The same (only modified) reservation should be allocated.",
                valueReservation2_1.getId(), valueReservation2_2.getId());
        Assert.assertEquals("Value should not be changed.",
                valueReservation2_1.getValue(), valueReservation2_2.getValue());

        // Extend #2 to intersect #1
        reservationRequest2 = getReservationRequest(reservationRequest2Id, ReservationRequest.class);
        reservationRequest2.setSlot("2013-01-01T00:00", "2014-06-01T00:00");
        ValueReservation valueReservation2_3 = (ValueReservation) allocateAndCheck(reservationRequest2);

        Assert.assertEquals("The same (only modified) reservation should be allocated.",
                valueReservation2_1.getId(), valueReservation2_2.getId());
        Assert.assertEquals("Second number from range should be allocated.", "1", valueReservation2_3.getValue());
    }

    @Test
    public void testAliasModification() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("{number:0:100}", AliasType.ROOM_NAME));
        createResource(aliasProvider);

        // Allocate alias
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2014-01-01T00:00", "2014-06-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("1"));
        String reservationRequestId = allocate(reservationRequest);
        AliasReservation aliasReservation1 = (AliasReservation) checkAllocated(reservationRequestId);

        Assert.assertEquals("Alias should be allocated.", "1", aliasReservation1.getValue());

        // Modify allocated alias
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("2"));
        AliasReservation aliasReservation2 = (AliasReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("The same (only modified) reservation should be allocated.",
                aliasReservation1.getId(), aliasReservation2.getId());
        Assert.assertEquals("Modified Alias should be allocated.", "2", aliasReservation2.getValue());
        Assert.assertEquals("The same (only modified) value reservation should be allocated.",
                aliasReservation1.getValueReservation().getId(), aliasReservation2.getValueReservation().getId());
    }

    @Test
    public void testAliasExtension() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture(Period.parse("P1Y"));
        aliasProvider.addCapability(new AliasProviderCapability("{number:0:100}", AliasType.ROOM_NAME));
        createResource(aliasProvider);

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
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSlot("2013-01-01T00:00", "P2Y");
        AliasReservation aliasReservation2 = (AliasReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("Value reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1.getValueReservation().getId(), aliasReservation2.getValueReservation().getId());
        Assert.assertEquals("Alias reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1.getId(), aliasReservation2.getId());
    }

    @Test
    public void testAliasSetModification() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("{number:0:100}", AliasType.ROOM_NAME));
        createResource(aliasProvider);

        // Allocate alias set
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2014-01-01T00:00", "2014-06-01T00:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("1"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("2"));
        reservationRequest.setSpecification(aliasSetSpecification);
        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation1 = checkAllocated(reservationRequestId);

        List<String> childReservationIds1 = reservation1.getChildReservationIds();
        Assert.assertEquals("Two child alias reservations should be allocated.", 2, childReservationIds1.size());
        AliasReservation aliasReservation1_1 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds1.get(0));
        AliasReservation aliasReservation1_2 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds1.get(1));
        Assert.assertEquals("First alias should be allocated.", "1", aliasReservation1_1.getValue());
        Assert.assertEquals("Second alias should be allocated.", "2", aliasReservation1_2.getValue());

        // Modify allocated alias set
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        aliasSetSpecification = (AliasSetSpecification) reservationRequest.getSpecification();
        aliasSetSpecification.getAliases().get(1).setValue("22");
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("2"));
        Reservation reservation2 = allocateAndCheck(reservationRequest);

        Assert.assertEquals("The same (only modified) reservation should be allocated.",
                reservation1.getId(), reservation2.getId());
        List<String> childReservationIds2 = reservation2.getChildReservationIds();
        Assert.assertEquals("Three child alias reservations should be allocated.", 3, childReservationIds2.size());
        AliasReservation aliasReservation2_1 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds2.get(0));
        AliasReservation aliasReservation2_2 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds2.get(1));
        AliasReservation aliasReservation2_3 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds2.get(2));
        Assert.assertEquals("First alias should be allocated.", "1", aliasReservation2_1.getValue());
        Assert.assertEquals("The same (only modified) child reservation should be allocated.",
                aliasReservation1_1.getId(), aliasReservation2_1.getId());
        Assert.assertEquals("Modified alias should be allocated.", "22", aliasReservation2_2.getValue());
        Assert.assertEquals("The same (only modified) child reservation should be allocated.",
                aliasReservation1_2.getId(), aliasReservation2_2.getId());

        Assert.assertTrue("New child reservation should be allocated.",
                !aliasReservation1_1.getId().equals(aliasReservation2_3.getId()) &&
                        !aliasReservation1_2.getId().equals(aliasReservation2_3.getId()));
        Assert.assertEquals("Third alias should be allocated.", "2", aliasReservation2_3.getValue());
    }

    @Test
    public void testAliasSetExtension() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture(Period.parse("P1Y"));
        aliasProvider.addCapability(new AliasProviderCapability("{number:0:100}", AliasType.ROOM_NAME));
        createResource(aliasProvider);

        // In 2013
        setWorkingInterval(Interval.parse("2013-01-01T00:00/2013-02-01T00:00"));
        // Allocate a new alias reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("1"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("2"));
        reservationRequest.setSpecification(aliasSetSpecification);
        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation1 = checkAllocated(reservationRequestId);

        List<String> childReservationIds1 = reservation1.getChildReservationIds();
        Assert.assertEquals("Two child alias reservations should be allocated.", 2, childReservationIds1.size());
        AliasReservation aliasReservation1_1 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds1.get(0));
        AliasReservation aliasReservation1_2 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds1.get(1));

        // In 2014
        setWorkingInterval(Interval.parse("2014-01-01T00:00/2014-02-01T00:00"));
        // Extend the validity of the alias reservation
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSlot("2013-01-01T00:00", "P2Y");
        Reservation reservation2 = allocateAndCheck(reservationRequest);

        Assert.assertEquals("Reservation identifiers should be same (the reservation should be only extended)",
                reservation1.getId(), reservation2.getId());
        List<String> childReservationIds2 = reservation2.getChildReservationIds();
        Assert.assertEquals("Two child alias reservations should be allocated.", 2, childReservationIds2.size());
        AliasReservation aliasReservation2_1 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds2.get(0));
        AliasReservation aliasReservation2_2 =
                (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN, childReservationIds2.get(1));
        Assert.assertEquals("Reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1_1.getId(), aliasReservation2_1.getId());
        Assert.assertEquals("Reservation identifiers should be same (the reservation should be only extended)",
                aliasReservation1_2.getId(), aliasReservation2_2.getId());
    }

    @Test
    public void testRoomModification() throws Exception
    {
        DeviceResource multipoint = new DeviceResource();
        multipoint.setName("multipoint");
        multipoint.setAllocatable(true);
        multipoint.addTechnology(Technology.H323);
        multipoint.addCapability(new RoomProviderCapability(5));
        createResource(multipoint);

        // Allocate a new room reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(3, Technology.H323));
        String reservationRequestId = allocate(reservationRequest);
        RoomReservation roomReservation1 = (RoomReservation) checkAllocated(reservationRequestId);

        // Increase the room capacity
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        ((RoomSpecification) reservationRequest.getSpecification()).setParticipantCount(5);
        RoomReservation roomReservation2 = (RoomReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("Reservation identifiers should be same (only the room capacity should be increased)",
                roomReservation1.getId(), roomReservation2.getId());
        Assert.assertEquals("Executable identifiers should be same.",
                roomReservation1.getExecutable().getId(), roomReservation2.getExecutable().getId());
    }

    @Test
    public void testRoomExtension() throws Exception
    {
        DeviceResource multipoint = new DeviceResource();
        multipoint.setName("multipoint");
        multipoint.setAllocatable(true);
        multipoint.addTechnology(Technology.H323);
        multipoint.addCapability(new RoomProviderCapability(5));
        createResource(multipoint);

        // Allocate a new room reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(3, Technology.H323));
        String reservationRequestId = allocate(reservationRequest);
        RoomReservation roomReservation1 = (RoomReservation) checkAllocated(reservationRequestId);

        // Extend the validity of the room reservation
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        RoomReservation roomReservation2 = (RoomReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals("Reservation identifiers should be same (only the room capacity should be increased)",
                roomReservation1.getId(), roomReservation2.getId());
        Assert.assertEquals("Executable identifiers should be same.",
                roomReservation1.getExecutable().getId(), roomReservation2.getExecutable().getId());
    }

    @Test
    public void testAliasWithRoomCapacityExtension() throws Exception
    {
        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAllocatable(true);
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10,
                new AliasType[]{AliasType.ROOM_NAME, AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(new AliasProviderCapability(
                "test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}").withPermanentRoom());
        connectServer.addCapability(new AliasProviderCapability(
                "test", AliasType.ROOM_NAME).withPermanentRoom());
        createResource(connectServer);

        // Allocate a new alias reservation
        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.setSharedExecutable(true);
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME));
        aliasReservationRequest.setSpecification(aliasSetSpecification);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        Reservation aliasReservation1 = checkAllocated(aliasReservationRequestId);

        // Allocate a room capacity for alias
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new RoomSpecification(10, Technology.ADOBE_CONNECT));
        reservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        // Extend the validity of the alias reservation
        aliasReservationRequest = getReservationRequest(aliasReservationRequestId, ReservationRequest.class);
        aliasReservationRequest.startModification();
        aliasReservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        Reservation aliasReservation2 = allocateAndCheck(aliasReservationRequest);

        Assert.assertEquals(aliasReservation1.getId(), aliasReservation2.getId());
        Executable.RoomExecutable resourceRoom1 = (Executable.RoomExecutable) aliasReservation1.getExecutable();
        Executable.RoomExecutable resourceRoom2 = (Executable.RoomExecutable) aliasReservation2.getExecutable();
        Assert.assertEquals(resourceRoom1.getId(), resourceRoom2.getId());
        Assert.assertEquals(resourceRoom1.getAliases(), resourceRoom2.getAliases());
    }

    @Test
    public void testMaintenanceForcesReallocation() throws Exception
    {
        DeviceResource multipoint1 = new DeviceResource();
        multipoint1.setName("multipoint1");
        multipoint1.setAllocatable(true);
        multipoint1.addTechnology(Technology.H323);
        multipoint1.addCapability(new RoomProviderCapability(10));
        String multipoint1Id = createResource(multipoint1);

        DeviceResource multipoint2 = new DeviceResource();
        multipoint2.setName("multipoint2");
        multipoint2.setAllocatable(true);
        multipoint2.addTechnology(Technology.H323);
        multipoint2.addCapability(new RoomProviderCapability(5));
        String multipoint2Id = createResource(multipoint2);

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

        Assert.assertEquals("Maintenance reservation should be allocated.",
                multipoint1Id, maintenanceReservation.getResourceId());
        Assert.assertEquals("Room should be migrated to another device.",
                multipoint2Id, roomReservation.getResourceId());
    }*/

    /**
     * Test periodic request for users.
     *
     * @throws Exception
     */
    @Test
    public void testDeletePeriodicity() throws Exception
    {
        /*UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(Locale.ENGLISH);
        userSettings.setUseWebService(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);*/

        ReservationService reservationService = getReservationService();

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture("P1M");
        String aliasProviderId = getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSet = new cz.cesnet.shongo.controller.api.ReservationRequestSet();
        reservationRequestSet.setDescription("Alias Reservation Request");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-03-04T12:00", "PT30M", "P1D", "2012-03-15"));
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String reservationRequestId = reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequestSet);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-20T00:00"));


        //AbstractReservationRequest reservationRequestModification = getReservationRequest(reservationRequestId, ReservationRequestSet.class);
        ReservationRequest reservationRequestModification = new ReservationRequest();
        reservationRequestModification.setId(reservationRequestId);
        reservationRequestModification.setPurpose(reservationRequestSet.getPurpose());
        reservationRequestModification.setSpecification(reservationRequestSet.getSpecification());
        reservationRequestModification.setSlot("2012-03-04T12:00", "2012-03-04T13:00");
        reservationRequestId = reservationService.modifyReservationRequest(SECURITY_TOKEN, reservationRequestModification);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-23T00:00"));
    }
}
