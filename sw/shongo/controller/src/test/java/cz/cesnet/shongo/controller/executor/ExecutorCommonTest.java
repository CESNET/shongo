package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link cz.cesnet.shongo.controller.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorCommonTest extends AbstractExecutorTest
{
    private static Logger logger = LoggerFactory.getLogger(ExecutorCommonTest.class);

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testRoom() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Allocate {@link cz.cesnet.shongo.controller.api.CompartmentExecutable} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testCompartment() throws Exception
    {
        McuTestAgent terminalAgent = getController().addJadeAgent("terminal", new McuTestAgent());
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("9500872{digit:2}", AliasType.H323_E164));
        String aliasProviderId = getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        terminal.setMode(new ManagedMode(terminalAgent.getName()));
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalId));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);
        allocateAndCheck(reservationRequest);

        // Start compartment
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("Three executables should be started.", 3, result.getStartedExecutables().size());
        Assert.assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Assert.assertEquals("The second started executable should be connection.",
                Connection.class, result.getStartedExecutables().get(1).getClass());
        Assert.assertEquals("The third started executable should be compartment.",
                Compartment.class, result.getStartedExecutables().get(2).getClass());
        // Stop compartment
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Three executables should be stopped.", 3, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.endpoint.Dial.class);
                add(cz.cesnet.shongo.connector.api.jade.endpoint.HangUpAll.class);
            }}, terminalAgent.getPerformedCommandClasses());
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testAlias() throws Exception
    {
        McuTestAgent connectServerAgent = getController().addJadeAgent("connectServer", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, connectServer);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.ADOBE_CONNECT));

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, connectServerAgent.getPerformedCommandClasses());
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testRoomWithSetting() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
        cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom createRoomAction =
                mcuAgent.getPerformedCommandByClass(
                        cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
        Assert.assertEquals("1234", createRoomAction.getRoom().getRoomSetting(H323RoomSetting.class).getPin());
    }

    /**
     * Allocate {@link RoomEndpoint} through {@link AliasReservation} and reuse it by {@link RoomReservation}.
     * The preference of reused room is also tested by presence of fake connect server (which should not be used).
     *
     * @throws Exception
     */
    @Test
    public void testReusedAlias() throws Exception
    {
        McuTestAgent connectServerAgent = getController().addJadeAgent("connectServer", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource connectServerFake = new DeviceResource();
        connectServerFake.setName("connectServerFake");
        connectServerFake.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFake.addCapability(new RoomProviderCapability(10));
        connectServerFake.addCapability(new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServerFake.setAllocatable(true);
        connectServerFake.setMode(new ManagedMode(connectServerAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN, connectServerFake);

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String connectServerId = getResourceService().createResource(SECURITY_TOKEN, connectServer);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot(dateTime, duration);
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(
                new AliasSpecification(Technology.ADOBE_CONNECT).withResourceId(connectServerId));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestId);
        Assert.assertEquals("Alias should not be allocated from the fake connect server.",
                "test", aliasReservation.getValue());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT));
        reservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        allocateAndCheck(reservationRequest);

        // Start virtual rooms
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        Assert.assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Assert.assertEquals("The second started executable should be used virtual room.",
                UsedRoomEndpoint.class, result.getStartedExecutables().get(1).getClass());
        // Stop virtual rooms
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        List<Class<? extends Command>> performedCommandClasses = connectServerAgent.getPerformedCommandClasses();
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, performedCommandClasses);
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both of them separately (first {@link RoomEndpoint} and then
     * {@link cz.cesnet.shongo.controller.executor.Compartment}).
     *
     * @throws Exception
     */
    @Test
    public void testReusedRoomStartedSeparately() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        roomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String roomReservationRequestId = allocate(roomReservationRequest);
        String roomReservationId = checkAllocated(roomReservationRequestId).getId();

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.setReusedReservationRequestId(roomReservationRequestId);
        allocateAndCheck(reservationRequest);

        // Start compartment
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be compartment.",
                Compartment.class, result.getStartedExecutables().get(0).getClass());

        // Stop virtual room and compartment
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both at once ({@link RoomEndpoint} through the {@link cz.cesnet.shongo.controller.executor.Compartment}).
     *
     * @throws Exception
     */
    @Test
    public void testReusedRoomStartedAtOnce() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        roomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String roomReservationRequestId = allocate(roomReservationRequest);
        String roomReservationId = checkAllocated(roomReservationRequestId).getId();

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.setReusedReservationRequestId(roomReservationRequestId);
        allocateAndCheck(reservationRequest);

        // Execute compartment
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        Assert.assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Assert.assertEquals("The second started executable should be compartment.",
                Compartment.class, result.getStartedExecutables().get(1).getClass());

        // Stop compartment
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * TODO:
     *
     * @throws Exception
     */
    @Test
    public void testRoomUpdate() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        String user2Id = getUserId(SECURITY_TOKEN_USER2);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN_USER1, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        String reservationRequestId = allocate(SECURITY_TOKEN_USER1, reservationRequest);
        checkAllocated(reservationRequestId);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        // Update room
        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequestId, Role.OWNER);
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());

        // Update room
        deleteAclRecord(user2Id, reservationRequestId, Role.OWNER);
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());

        // Stop virtual room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * TODO:
     *
     * @throws Exception
     */
    @Test
    public void testReusedAliasUpdate() throws Exception
    {
        McuTestAgent connectServerAgent = getController().addJadeAgent("connectServer", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        String user2Id = getUserId(SECURITY_TOKEN_USER2);
        String user3Id = getUserId(SECURITY_TOKEN_USER3);

        DeviceResource connectServerFake = new DeviceResource();
        connectServerFake.setName("connectServerFake");
        connectServerFake.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFake.addCapability(new RoomProviderCapability(10));
        connectServerFake.addCapability(new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServerFake.setAllocatable(true);
        connectServerFake.setMode(new ManagedMode(connectServerAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN_USER1, connectServerFake);

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String connectServerId = getResourceService().createResource(SECURITY_TOKEN_USER1, connectServer);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot(dateTime, duration);
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(
                new AliasSpecification(Technology.ADOBE_CONNECT).withResourceId(connectServerId));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(SECURITY_TOKEN_USER1, aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestId);
        Assert.assertEquals("Alias should not be allocated from the fake connect server.",
                "test", aliasReservation.getValue());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT));
        reservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequestId, Role.OWNER);

        // Start virtual rooms
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        Assert.assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Assert.assertEquals("The second started executable should be used virtual room.",
                UsedRoomEndpoint.class, result.getStartedExecutables().get(1).getClass());

        // Get room id
        RoomExecutable roomExecutable = (RoomExecutable) getExecutorService().getExecutable(
                SECURITY_TOKEN_USER1, aliasReservation.getExecutable().getId());
        String roomId = roomExecutable.getRoomId();
        String roomResourceId = roomExecutable.getResourceId();
        Assert.assertNotNull("Alias room should have roomId.", roomId);

        // Check room
        Room room = getResourceControlService().getRoom(SECURITY_TOKEN_USER1, roomResourceId, roomId);
        Assert.assertEquals("Room should have 10 licenses.", 10, room.getLicenseCount());
        Assert.assertEquals("Room should have 2 participants.", 2, room.getParticipants().size());

        // Update alias
        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user3Id, aliasReservationRequestId, Role.OWNER);
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());
        // Check room
        room = getResourceControlService().getRoom(SECURITY_TOKEN_USER1, roomResourceId, roomId);
        Assert.assertEquals("Room should have 10 licenses.", 10, room.getLicenseCount());
        Assert.assertEquals("Room should have 3 participants.", 3, room.getParticipants().size());

        // Stop virtual rooms
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        List<Class<? extends Command>> performedCommandClasses = connectServerAgent.getPerformedCommandClasses();
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.GetRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.GetRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, performedCommandClasses);
    }

    /**
     * Test delete {@link ReservationRequest} with started {@link ResourceRoomEndpoint}.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteStarted() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DateTime dateTime = DateTime.now();
        Period duration = Period.parse("PT2H");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationRequestId = allocate(roomReservationRequest);
        checkAllocated(roomReservationRequestId);

        // Execute compartment
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        Thread.sleep(1000);

        // Delete reservation request and the reservation
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        // Run scheduler to modify room ending date/time
        runScheduler();

        // Stop compartment
        result = runExecutor(DateTime.now());
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }
}
