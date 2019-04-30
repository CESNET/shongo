package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AdobeConnectPermissions;
import cz.cesnet.shongo.api.AdobeConnectRoomSetting;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.booking.compartment.Compartment;
import cz.cesnet.shongo.controller.booking.compartment.Connection;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import jade.core.AID;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tests for {@link cz.cesnet.shongo.controller.executor.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableTest extends AbstractExecutorTest
{
    private static Logger logger = LoggerFactory.getLogger(ExecutableTest.class);

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
        String mcuId = createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));

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
                add(CreateRoom.class);
                add(DeleteRoom.class);
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
        String aliasProviderId = createResource(aliasProvider);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        terminal.setMode(new ManagedMode(terminalAgent.getName()));
        String terminalId = createResource(terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(mcu);

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(terminalId));
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 1));
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
                add(CreateRoom.class);
                add(DeleteRoom.class);
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
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}"));
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String mcuId = createResource(connectServer);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(Technology.ADOBE_CONNECT));

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
                add(CreateRoom.class);
                add(DeleteRoom.class);
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
        String mcuId = createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5, Technology.H323);
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
                add(CreateRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
        CreateRoom createRoomAction =
                mcuAgent.getPerformedCommandByClass(
                        CreateRoom.class);
        H323RoomSetting roomSetting = createRoomAction.getRoom().getRoomSetting(H323RoomSetting.class);
        Assert.assertNotNull(roomSetting);
        Assert.assertEquals("1234", roomSetting.getPin());
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link Compartment} and execute
     * both of them separately (first {@link RoomEndpoint} and then
     * {@link Compartment}).
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
        String mcuId = createResource(mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(10, Technology.H323));
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
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 10));
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
        Assert.assertEquals(new ArrayList<Class<? extends Command>>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link Compartment} and execute
     * both at once ({@link RoomEndpoint} through the {@link Compartment}).
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
        String mcuId = createResource(mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        roomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String roomReservationRequestId = allocate(roomReservationRequest);
        String roomReservationId = checkAllocated(roomReservationRequestId).getId();

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 10));
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
                add(CreateRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Test for updating virtual room when new ACL entry is created when room is active.
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
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));

        // Allocate reservation request
        String reservationRequestId = allocate(SECURITY_TOKEN_USER1, reservationRequest);
        checkAllocated(reservationRequestId);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);

        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        // Update room
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1,
                new AclEntry(user2Id, reservationRequestId, ObjectRole.OWNER));
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());

        // Update room
        deleteAclEntry(user2Id, reservationRequestId, ObjectRole.OWNER);
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());

        // Stop virtual room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Execute permanent room and then it's capacity and also stop it.
     *
     * @throws Exception
     */
    @Test
    public void testPermanentRoomWithCapacity() throws Exception
    {
        ConnectTestAgent connectAgent = getController().addJadeAgent("connect", new ConnectTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period capacityDuration = Period.parse("PT1H");

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connect");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}"));
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN_USER1, connectServer);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot(dateTime.minusDays(1), dateTime.plusDays(1));
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification permanentRoomSpecification = new RoomSpecification(Technology.ADOBE_CONNECT);
        AdobeConnectRoomSetting permanentRoomSetting = new AdobeConnectRoomSetting();
        permanentRoomSetting.setPin("1234");
        permanentRoomSetting.setAccessMode(AdobeConnectPermissions.PRIVATE);
        permanentRoomSpecification.addRoomSetting(permanentRoomSetting);
        permanentRoomReservationRequest.setSpecification(permanentRoomSpecification);
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(SECURITY_TOKEN_USER1, permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        String permanentRoomExecutableId = permanentRoomExecutable.getId();

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot(dateTime, capacityDuration);
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        RoomSpecification capacitySpecification = new RoomSpecification(10);
        AdobeConnectRoomSetting capacitySetting = new AdobeConnectRoomSetting();
        capacitySetting.setPin("abcd");
        capacitySetting.setAccessMode(AdobeConnectPermissions.PUBLIC);
        capacitySpecification.addRoomSetting(capacitySetting);
        capacityReservationRequest.setSpecification(capacitySpecification);
        String capacityRequestId = allocate(capacityReservationRequest);
        checkAllocated(capacityRequestId);

        // Start permanent room
        ExecutionResult result = runExecutor(dateTime.minusDays(1));
        Assert.assertEquals("Two executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be permanent room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Room permanentRoom = getRoom(permanentRoomExecutableId);
        AdobeConnectRoomSetting roomSetting = permanentRoom.getRoomSetting(AdobeConnectRoomSetting.class);
        Assert.assertEquals("1234", roomSetting.getPin());
        Assert.assertEquals(AdobeConnectPermissions.PRIVATE, roomSetting.getAccessMode());

        // Start capacity
        connectAgent.setDisabled(true);
        result = runExecutor(dateTime);
        Assert.assertEquals("None executable should be started.", 0, result.getStartedExecutables().size());
        connectAgent.setDisabled(false);
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        Assert.assertEquals("The started executable should be capacity.",
                UsedRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        permanentRoom = getRoom(permanentRoomExecutableId);
        roomSetting = permanentRoom.getRoomSetting(AdobeConnectRoomSetting.class);
        Assert.assertEquals("abcd", roomSetting.getPin());
        Assert.assertEquals(AdobeConnectPermissions.PUBLIC, roomSetting.getAccessMode());

        // Stop capacity
        result = runExecutor(dateTime.plus(capacityDuration));
        Assert.assertEquals("One executables should be stopped.", 1, result.getStoppedExecutables().size());
        Assert.assertEquals("The stopped executable should be capacity.",
                UsedRoomEndpoint.class, result.getStoppedExecutables().get(0).getClass());
        permanentRoom = getRoom(permanentRoomExecutableId);
        roomSetting = permanentRoom.getRoomSetting(AdobeConnectRoomSetting.class);
        Assert.assertNotNull(roomSetting);
        Assert.assertEquals("1234", roomSetting.getPin());
        Assert.assertEquals(AdobeConnectPermissions.PRIVATE, roomSetting.getAccessMode());

        // Stop permanent room
        result = runExecutor(dateTime.plusDays(1));
        Assert.assertEquals("One executables should be stopped.", 1, result.getStoppedExecutables().size());
        Assert.assertEquals("The stopped executable should be permanent room.",
                ResourceRoomEndpoint.class, result.getStoppedExecutables().get(0).getClass());

        // Check performed actions on connector agents
        List<Class<? extends Command>> performedCommandClasses = connectAgent.getPerformedCommandClasses();
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(CreateRoom.class);
                add(GetRoom.class);
                add(ModifyRoom.class);
                add(GetRoom.class);
                add(ModifyRoom.class);
                add(GetRoom.class);
                add(DeleteRoom.class);
            }}, performedCommandClasses);
    }

    /**
     * TODO:
     *
     * @throws Exception
     */
    @Test
    public void testPermanentRoomWithCapacityUpdate() throws Exception
    {
        McuTestAgent connectServerAgent = getController().addJadeAgent("connectServer", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        String user2Id = getUserId(SECURITY_TOKEN_USER2);
        String user3Id = getUserId(SECURITY_TOKEN_USER3);

        DeviceResource connectServerFake = new DeviceResource();
        connectServerFake.setName("connectServerFake");
        connectServerFake.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFake.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connectServerFake.addCapability(
                new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI,"{value}").withRestrictedToResource());
        connectServerFake.setAllocatable(true);
        connectServerFake.setMode(new ManagedMode(connectServerAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN_USER1, connectServerFake);

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{value}").withRestrictedToResource());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String connectServerId = getResourceService().createResource(SECURITY_TOKEN_USER1, connectServer);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot(dateTime, duration);
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(
                new RoomSpecification(Technology.ADOBE_CONNECT, connectServerId));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(SECURITY_TOKEN_USER1, permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        String permanentRoomExecutableId = permanentRoomExecutable.getId();
        Assert.assertEquals("Room should not be allocated from the fake connect server.",
                "test", permanentRoomExecutable.getAliasByType(AliasType.ADOBE_CONNECT_URI).getValue());

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot(dateTime, duration);
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(10));
        String capacityReservationRequestId = allocate(capacityReservationRequest);
        checkAllocated(capacityReservationRequestId);

        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1,
                new AclEntry(user2Id, capacityReservationRequestId, ObjectRole.OWNER));

        // Start virtual rooms
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        Assert.assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        Assert.assertEquals("The second started executable should be used virtual room.",
                UsedRoomEndpoint.class, result.getStartedExecutables().get(1).getClass());

        // Get room id
        permanentRoomExecutable = (RoomExecutable) getExecutableService().getExecutable(
                SECURITY_TOKEN_USER1, permanentRoomExecutableId);
        String roomId = permanentRoomExecutable.getRoomId();
        String roomResourceId = permanentRoomExecutable.getResourceId();
        Assert.assertNotNull("Alias room should have roomId.", roomId);

        // Check room
        Room room = getResourceControlService().getRoom(SECURITY_TOKEN_USER1, roomResourceId, roomId);
        Assert.assertEquals("Room should have 10 licenses.", 10, room.getLicenseCount());
        Assert.assertEquals("Room should have 2 participants.", 2, room.getParticipantRoles().size());

        // Update permanent room
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1,
                new AclEntry(user3Id, permanentRoomReservationRequestId, ObjectRole.OWNER));
        result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be updated.", 1, result.getUpdatedExecutables().size());
        // Check room
        room = getResourceControlService().getRoom(SECURITY_TOKEN_USER1, roomResourceId, roomId);
        Assert.assertEquals("Room should have 10 licenses.", 10, room.getLicenseCount());
        Assert.assertEquals("Room should have 3 participants.", 3, room.getParticipantRoles().size());

        // Stop virtual rooms
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        List<Class<? extends Command>> performedCommandClasses = connectServerAgent.getPerformedCommandClasses();
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(GetRoom.class);
                add(ModifyRoom.class);
                add(GetRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, performedCommandClasses);
    }

    /**
     * Allocate permanent room with capacity.
     * The preference of reused room is also tested by presence of fake connect server (which should not be used).
     *
     * @throws Exception
     */
    @Test
    public void testPermanentRoomWithCapacityPreference() throws Exception
    {
        McuTestAgent connectServerAgent = getController().addJadeAgent("connectServer", new McuTestAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource connectServerFake = new DeviceResource();
        connectServerFake.setName("connectServerFake");
        connectServerFake.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFake.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connectServerFake.addCapability(
                new AliasProviderCapability("fake",AliasType.ADOBE_CONNECT_URI, "{value}").withRestrictedToResource());
        connectServerFake.setAllocatable(true);
        connectServerFake.setMode(new ManagedMode(connectServerAgent.getName()));
        createResource(connectServerFake);

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{value}").withRestrictedToResource());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String connectServerId = createResource(connectServer);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot(dateTime, duration);
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(
                new RoomSpecification(Technology.ADOBE_CONNECT, connectServerId));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        Assert.assertEquals("Permanent room should not be allocated from the fake connect server.",
                "test", permanentRoomExecutable.getAliasByType(AliasType.ADOBE_CONNECT_URI).getValue());

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot(dateTime, duration);
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(10));
        allocateAndCheck(capacityReservationRequest);

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
                add(CreateRoom.class);
                add(ModifyRoom.class);
                add(ModifyRoom.class);
                add(DeleteRoom.class);
            }}, performedCommandClasses);
    }

    @Test
    public void testPermanentRoomWithCapacityTechnologyModification() throws Exception
    {
        final Set<Technology> technologiesH323 = new HashSet<Technology>(){{
            add(Technology.H323);
        }};
        final Set<Technology> technologiesAdobeConnect = new HashSet<Technology>(){{
            add(Technology.ADOBE_CONNECT);
        }};

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.setAllocatable(true);
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10));
        createResource(connect);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        // Create permanent room
        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        String permanentRoomExecutableId = permanentRoomExecutable.getId();

        // Allocate a permanent room capacity
        ReservationRequest capacityReservationRequest1 = new ReservationRequest();
        capacityReservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        capacityReservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest1.setReusedReservationRequestId(permanentRoomReservationRequestId);
        capacityReservationRequest1.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId1 = allocate(capacityReservationRequest1);
        checkAllocated(capacityReservationRequestId1);

        // Allocate a permanent room capacity
        ReservationRequest capacityReservationRequest2 = new ReservationRequest();
        capacityReservationRequest2.setSlot("2012-02-01T12:00", "PT2H");
        capacityReservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest2.setReusedReservationRequestId(permanentRoomReservationRequestId);
        capacityReservationRequest2.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId2 = allocate(capacityReservationRequest2);
        checkAllocated(capacityReservationRequestId2);

        Map<String, ReservationRequestSummary> reservationRequestMap = getReservationRequestMap();
        Assert.assertEquals(technologiesH323,
                reservationRequestMap.get(permanentRoomReservationRequestId).getSpecificationTechnologies());
        Assert.assertEquals(technologiesH323,
                reservationRequestMap.get(capacityReservationRequestId1).getSpecificationTechnologies());
        Assert.assertEquals(technologiesH323,
                reservationRequestMap.get(capacityReservationRequestId2).getSpecificationTechnologies());

        // Increase room capacity
        permanentRoomReservationRequest =
                getReservationRequest(permanentRoomReservationRequestId, ReservationRequest.class);
        RoomSpecification roomSpecification = ((RoomSpecification) permanentRoomReservationRequest.getSpecification());
        roomSpecification.getEstablishment().setTechnology(Technology.ADOBE_CONNECT);
        permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest, new DateTime("2012-02-01T00:00"));
        checkAllocated(permanentRoomReservationRequestId);

        capacityReservationRequest1 = getReservationRequest(capacityReservationRequestId1, ReservationRequest.class);
        capacityReservationRequest2 = getReservationRequest(capacityReservationRequestId2, ReservationRequest.class);
        RoomReservation capacityReservation1 =
                getReservation(capacityReservationRequest1.getLastReservationId(), RoomReservation.class);
        RoomReservation capacityReservation2 =
                getReservation(capacityReservationRequest2.getLastReservationId(), RoomReservation.class);
        UsedRoomExecutable capacity1 = (UsedRoomExecutable) capacityReservation1.getExecutable();
        UsedRoomExecutable capacity2 = (UsedRoomExecutable) capacityReservation2.getExecutable();
        Assert.assertEquals(technologiesH323, capacity1.getTechnologies());
        Assert.assertEquals(technologiesAdobeConnect, capacity2.getTechnologies());

        reservationRequestMap = getReservationRequestMap();
        Assert.assertEquals(technologiesAdobeConnect,
                reservationRequestMap.get(permanentRoomReservationRequestId).getSpecificationTechnologies());
        Assert.assertEquals(technologiesH323,
                reservationRequestMap.get(capacityReservationRequestId1).getSpecificationTechnologies());
        Assert.assertEquals(technologiesAdobeConnect,
                reservationRequestMap.get(capacityReservationRequestId2).getSpecificationTechnologies());
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
        String mcuId = createResource(mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(10, Technology.H323);
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
        runScheduler(dateTime.plusHours(1));

        // Stop compartment
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(CreateRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }

    /**
     * Test participants in room.
     *
     * @throws Exception
     */
    @Test
    public void testRoomParticipants() throws Exception
    {
        String userId2 = getUserId(SECURITY_TOKEN_USER2);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10));
        connect.setAllocatable(true);
        String firstMcuId = createResource(connect);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5, Technology.ADOBE_CONNECT);

        UserPerson userPerson = new UserPerson(userId2);
        userPerson.setName("name");
        userPerson.setOrganization("organization");
        userPerson.setEmail("email");

        roomSpecification.addParticipant(new PersonParticipant(userPerson));
        reservationRequest.setSpecification(roomSpecification);

        Reservation reservation = allocateAndCheck(reservationRequest);
        RoomExecutable roomExecutable = (RoomExecutable) reservation.getExecutable();
        String roomExecutableId = roomExecutable.getId();

        getExecutableService().getExecutable(SECURITY_TOKEN_USER2, roomExecutableId);
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testRoomModificationFailed() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent(){
            @Override
            public Object handleCommand(Command command, AID sender) throws CommandException
            {
                if (command instanceof ModifyRoom) {
                    ModifyRoom modifyRoom = (ModifyRoom) command;
                    if (modifyRoom.getRoom().getDescription().contains("ModifyRoomDisabled")) {
                        throw new RuntimeException("cannot modify room");
                    }
                }
                return super.handleCommand(command, sender);
            }
        });

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2H");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));

        // Allocate reservation request
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        // Start virtual room
        ExecutionResult result = runExecutor(dateTime);
        Assert.assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());

        // Modify room
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setDescription("ModifyRoomDisabled");
        reservationRequestId = allocate(reservationRequest, dateTime.plusHours(1));
        checkAllocated(reservationRequestId);

        // Check that nothing gets executed
        result = runExecutor(dateTime.plusHours(1));
        Assert.assertEquals("No executable should be stopped.", 0, result.getStoppedExecutables().size());
        Assert.assertEquals("No executable should be started.", 0, result.getStartedExecutables().size());

        // Stop the original room
        result = runExecutor(dateTime.plus(duration));
        Assert.assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Object>()
        {{
                add(CreateRoom.class);
                add(DeleteRoom.class);
            }}, mcuAgent.getPerformedCommandClasses());
    }
}
