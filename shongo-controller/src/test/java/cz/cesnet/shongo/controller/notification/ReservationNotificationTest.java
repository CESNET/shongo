package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests for notifying about new/modified/deleted {@link Reservation}s by emails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotificationTest extends AbstractExecutorTest
{
    /**
     * @see TestingNotificationExecutor
     */
    private TestingNotificationExecutor notificationExecutor = new TestingNotificationExecutor();

    @Override
    public void before() throws Exception
    {
        System.setProperty(ControllerConfiguration.NOTIFICATION_USER_SETTINGS_URL,
                "https://127.0.0.1:8182/user/settings");
        System.setProperty(ControllerConfiguration.NOTIFICATION_RESERVATION_REQUEST_URL,
                "https://127.0.0.1:8182/detail/${reservationRequestId}");
        System.setProperty(ControllerConfiguration.NOTIFICATION_RESERVATION_REQUEST_CONFIRMATION_URL,
                "https://127.0.0.1:8182/resource/reservation-request/confirmation/?resource-id=${resourceId}&date=${date}");
        System.setProperty(ControllerConfiguration.TIMEZONE, "UTC");
        super.before();
    }

    @Override
    public void configureSystemProperties()
    {
        super.configureSystemProperties();
        System.setProperty(ControllerConfiguration.NOTIFICATION_USER_SETTINGS_URL,
                "https://127.0.0.1:8182/user/settings");
        System.setProperty(ControllerConfiguration.NOTIFICATION_RESERVATION_REQUEST_URL,
                "https://127.0.0.1:8182/detail/${reservationRequestId}");
        System.setProperty(ControllerConfiguration.NOTIFICATION_RESERVATION_REQUEST_CONFIRMATION_URL,
                "https://127.0.0.1:8182/resource/reservation-request/confirmation/?resource-id=${resourceId}&date=${date}");
        System.setProperty(ControllerConfiguration.TIMEZONE, "UTC");
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        getController().addNotificationExecutor(notificationExecutor);

        getController().getConfiguration().setAdministrators(new LinkedList<PersonInformation>()
        {{
                add(new PersonInformation()
                {
                    @Override
                    public String getFullName()
                    {
                        return "admin";
                    }

                    @Override
                    public String getRootOrganization()
                    {
                        return null;
                    }

                    @Override
                    public String getPrimaryEmail()
                    {
                        return "martin.srom@cesnet.cz";
                    }

                    @Override
                    public String toString()
                    {
                        return getFullName();
                    }
                });
            }});
    }

    /**
     * Test user settings.
     *
     * @throws Exception
     */
    @Test
    public void testDisabledResourceAdministratorNotifications() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN_USER1, resource);

        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER1);
        userSettings.setResourceAdministratorNotifications(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER1, userSettings);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Resource Reservation Request");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        // 1x user: changes (new)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
            }}, getNotificationTypes());
    }

    /**
     * Test single technology virtual room.
     *
     * @throws Exception
     */
    @Test
    public void testRoom() throws Exception
    {
        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(3));
        tcs.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN_ROOT, tcs);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10,
                new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164, AliasType.SIP_URI}));
        mcu.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, mcu);

        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(UserSettings.LOCALE_CZECH);
        userSettings.setHomeTimeZone(DateTimeZone.forID("+05:00"));
        userSettings.setCurrentTimeZone(DateTimeZone.forID("+06:00"));
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(new Technology[]{Technology.H323, Technology.SIP});
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(4000);
        roomAvailability.setSlotMinutesBefore(10);
        roomAvailability.setSlotMinutesAfter(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        roomAvailability = ((RoomSpecification) reservationRequest.getSpecification()).getAvailability();
        roomAvailability.setParticipantCount(3);
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        ((RoomSpecification) reservationRequest.getSpecification()).getAvailability().setParticipantCount(6);
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN_ROOT, reservationRequestId);
        runScheduler();

        // 1x system-admin: allocation-failed
        // 4x resource-admin: new, deleted, new, deleted
        // 4x user: changes(allocation-failed), changes (new), changes (deleted, new), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(AllocationFailedNotification.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test permanent room.
     *
     * @throws Exception
     */
    @Test
    public void testPermanentRoomAndCapacity() throws Exception
    {
        DeviceResource aliasProvider = new DeviceResource();
        aliasProvider.addTechnology(Technology.SIP);
        aliasProvider.setName("aliasProvider");
        aliasProvider.addCapability(new RoomProviderCapability(10));
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(new ValueProvider.Pattern("{hash}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        aliasProvider.addCapability(aliasProviderCapability);
        aliasProvider.setAllocatable(true);
        aliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setDescription("Alias Reservation Request");
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(AliasType.ADOBE_CONNECT_URI));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocationFailed(permanentRoomReservationRequestId);

        permanentRoomReservationRequest =
                getReservationRequest(permanentRoomReservationRequestId, ReservationRequest.class);
        RoomSpecification roomSpecification =
                (RoomSpecification) permanentRoomReservationRequest.getSpecification();
        roomSpecification.getEstablishment().getAliasSpecifications().get(0).setAliasTypes(new HashSet<AliasType>() {{
            add(AliasType.SIP_URI);
        }});
        permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setDescription("Capacity Reservation Request");
        capacityReservationRequest.setSlot("2012-01-01T12:00", "PT1H");
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId = allocate(capacityReservationRequest);
        checkAllocated(capacityReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, capacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();

        // 1x system-admin: allocation-failed
        // 2x resource-admin: new, deleted
        // 3x user: changes (allocation-failed), changes (new), changes (deleted)
        // 2x resource-admin: new, deleted
        // 2x user: changes (new), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(AllocationFailedNotification.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test single alias.
     *
     * @throws Exception
     */
    @Test
    public void testAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider
                .addCapability(new AliasProviderCapability("001", AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("$"));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        AliasSpecification aliasSpecification = (AliasSpecification) reservationRequest.getSpecification();
        aliasSpecification.setValue(null);
        aliasSpecification.setAliasTypes(new HashSet<AliasType>() {{
            add(AliasType.SIP_URI);
            }});
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        // 4x admin: new, deleted, new, deleted
        // 3x user: changes (new), changes (deleted, new), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test multiple aliases.
     *
     * @throws Exception
     */
    @Test
    public void testAliasSet() throws Exception
    {
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability("test");
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        firstAliasProvider.setAllocatable(true);
        firstAliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("001");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, secondAliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.H323_E164));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME));
        reservationRequest.setSpecification(aliasSetSpecification);

        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        // 2x admin: new, deleted
        // 2x user: changes (new), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test multiple aliases for owner.
     *
     * @throws Exception
     */
    @Test
    public void testOwnerAliasSet() throws Exception
    {
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        AliasProviderCapability aliasProviderCapability =
                new AliasProviderCapability("{hash}").withAllowedAnyRequestedValue();
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        firstAliasProvider.setAllocatable(true);
        createResource(SECURITY_TOKEN, firstAliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("*/*");
        reservationRequest.setPurpose(ReservationRequestPurpose.OWNER);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test1"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test2"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test3"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test4"));
        reservationRequest.setSpecification(aliasSetSpecification);

        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        // 2x admin: new, deleted
        // 2x user: changes (new), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    @Test
    public void testReusedAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        aliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        reservationRequest.setReusedReservationRequestId(aliasReservationRequestId);

        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation = checkAllocated(reservationRequestId);
        Assert.assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        Assert.assertEquals(aliasReservation.getId(), existingReservation.getReservation().getId());

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
        runScheduler();

        // 4x admin: new, new, deleted, deleted
        // 4x user: changes (new), changes (new), changes (deleted), changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                //add(ReservationNotification.Deleted.class);
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test periodic request.
     *
     * @throws Exception
     */
    @Test
    public void testPeriodic() throws Exception
    {
        ReservationService reservationService = getReservationService();

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture("P1M");
        aliasProvider.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.addSlot("2012-01-01T12:00", "P1D");
        reservationRequest.addSlot("2012-01-30T12:00", "P1D");
        reservationRequest.addSlot("2012-02-01T12:00", "P1D");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String reservationRequestId = reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessorAndScheduler(new Interval("2012-01-01T00:00/2012-03-01T00:00"));

        // 1x system-admin: allocation-failed
        // 2x resource-admin: new
        // 1x user: changes (allocation-failed, new, new)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationNotification.New.class);
                add(AllocationFailedNotification.class);
            }}, getNotificationTypes());
        clearNotificationRecords();

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequestSet.class);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        reservationRequestId = reservationService.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessorAndScheduler(new Interval("2012-01-01T00:00/2012-03-01T00:00"));

        // 1x resource-admin: deleted
        // 1x user: changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test participant notification for future room.
     *
     * @throws Exception
     */
    @Test
    public void testFutureRoomParticipation() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164}));
        mcu.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.addAdministratorEmail("martin.srom@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request\nTest multiline");
        reservationRequest.setSlot("2014-02-11T14:00", "PT3H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setMeetingName("First testing meeting");
        roomAvailability.setMeetingDescription("Long long long\nlong long long\nlong description");
        roomAvailability.setParticipantCount(5);
        roomAvailability.setParticipantNotificationEnabled(true);
        roomAvailability.setSlotMinutesBefore(10);
        roomAvailability.setSlotMinutesAfter(2);
        roomSpecification.addParticipant(new PersonParticipant(
                "Martin Srom", "srom@cesnet.cz", ParticipantRole.ADMINISTRATOR));
        roomSpecification.addParticipant(new PersonParticipant(
                "Ondrej Pavelka", "pavelka@cesnet.cz", ParticipantRole.PRESENTER));
        roomSpecification.addParticipant(new PersonParticipant(
                "Jan Ruzicka", "janru@cesnet.cz"));
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        Reservation roomReservation = checkAllocated(reservationRequestId);
        AbstractRoomExecutable room = (AbstractRoomExecutable) roomReservation.getExecutable();
        String roomId = room.getId();

        RoomExecutableParticipantConfiguration roomParticipants = room.getParticipantConfiguration();
        ((PersonParticipant) roomParticipants.getParticipants().get(0)).setRole(ParticipantRole.PARTICIPANT);
        getExecutableService().modifyExecutableConfiguration(SECURITY_TOKEN, roomId, roomParticipants);
        executeNotifications();

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequest.setSlot("2014-02-11T20:00", "PT2H");
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        // Check executed notifications
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                // Create room
                add(ReservationRequestNotification.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomCreated.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomCreated.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomCreated.class);
                add(ReservationNotification.New.class);
                // Modify executable participants
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                //Modify room
                //add(ReservationRequestNotification.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                //add(ReservationNotification.Deleted.class);
                add(ReservationNotification.New.class);
                // Delete room
                //add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
                //add(RoomGroupNotification.class);
                //add(RoomNotification.RoomDeleted.class);
                //add(RoomGroupNotification.class);
                //add(RoomNotification.RoomDeleted.class);
                //add(RoomGroupNotification.class);
                //add(RoomNotification.RoomDeleted.class);
            }}, getNotificationTypes());
    }

    /**
     * Test participant notifications for permanent room with capacities.
     *
     * @throws Exception
     */
    @Test
    public void testPermanentRoomParticipation() throws Exception
    {
        ExecutableService executableService = getExecutableService();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME}));
        mcu.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN_ROOT, mcu);

        // Disable automatic execution of notifications
        setNotificationExecutionEnabled(false);

        // Create Permanent Room with Permanent Participant 1
        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setDescription("Alias Reservation Request");
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification permanentRoomSpecification = new RoomSpecification(Technology.H323);
        permanentRoomSpecification.addParticipant(
                new PersonParticipant("Martin Srom", "srom@cesnet.cz", ParticipantRole.ADMINISTRATOR));
        permanentRoomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        permanentRoomReservationRequest.setSpecification(permanentRoomSpecification);
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        AbstractRoomExecutable permanentRoom = (AbstractRoomExecutable) permanentRoomReservation.getExecutable();
        String permanentRoomId = permanentRoom.getId();

        // Create Capacity 1 with Capacity Participant 1
        ReservationRequest firstCapacityReservationRequest = new ReservationRequest();
        firstCapacityReservationRequest.setDescription("Capacity Reservation Request 1\nmultiline test");
        firstCapacityReservationRequest.setSlot("2012-01-01T12:00", "PT1H");
        firstCapacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstCapacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        RoomSpecification firstCapacitySpecification = new RoomSpecification();
        RoomAvailability roomAvailability = firstCapacitySpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.setParticipantNotificationEnabled(true);
        firstCapacitySpecification.addRoomSetting(new H323RoomSetting().withPin("4321"));
        firstCapacitySpecification.addParticipant(new PersonParticipant("Ondrej Pavelka", "pavelka@cesnet.cz"));
        firstCapacityReservationRequest.setSpecification(firstCapacitySpecification);
        String firstCapacityReservationRequestId = allocate(firstCapacityReservationRequest);
        Reservation firstCapacityReservation = checkAllocated(firstCapacityReservationRequestId);
        AbstractRoomExecutable firstCapacity = (AbstractRoomExecutable) firstCapacityReservation.getExecutable();
        String firstCapacityId = firstCapacity.getId();

        // Create Capacity 2 without participants
        ReservationRequest secondCapacityReservationRequest = new ReservationRequest();
        secondCapacityReservationRequest.setDescription("Capacity Reservation Request 2");
        secondCapacityReservationRequest.setSlot("2012-01-01T14:00", "PT1H");
        secondCapacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        secondCapacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        RoomSpecification secondCapacitySpecification = new RoomSpecification();
        roomAvailability = secondCapacitySpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.setParticipantNotificationEnabled(true);
        secondCapacityReservationRequest.setSpecification(secondCapacitySpecification);
        String secondCapacityReservationRequestId = allocate(secondCapacityReservationRequest);
        Reservation secondCapacityReservation = checkAllocated(secondCapacityReservationRequestId);
        AbstractRoomExecutable secondCapacity = (AbstractRoomExecutable) secondCapacityReservation.getExecutable();
        String secondCapacityId = secondCapacity.getId();

        // Execute creation notifications
        executeNotifications();

        // Add Permanent Participant 2 (should be notified about both capacities)
        RoomExecutableParticipantConfiguration permanentRoomParticipants = permanentRoom.getParticipantConfiguration();
        permanentRoomParticipants.addParticipant(
                new PersonParticipant("Jan Ruzicka", "janru@cesnet.cz", ParticipantRole.PRESENTER));
        executableService.modifyExecutableConfiguration(SECURITY_TOKEN, permanentRoomId, permanentRoomParticipants);
        executeNotifications();

        // Add Capacity Participant 2 for Capacity 1 (should be notified about the single capacity)
        RoomExecutableParticipantConfiguration firstCapacityParticipants = firstCapacity.getParticipantConfiguration();
        firstCapacityParticipants.addParticipant(new PersonParticipant("Petr Holub", "holub@cesnet.cz"));
        executableService.modifyExecutableConfiguration(SECURITY_TOKEN, firstCapacityId, firstCapacityParticipants);
        executeNotifications();

        // Check created notifications
        List<TestingNotificationExecutor.NotificationRecord<RoomNotification>> notificationRecords =
                getNotificationRecords(RoomNotification.class);
        PersonInformation permanentParticipant1 =
                checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 1",
                        RoomNotification.RoomCreated.class, null, firstCapacityId).getRecipient();
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomCreated.class, permanentParticipant1, secondCapacityId);
        PersonInformation capacityParticipant1 =
                checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                        RoomNotification.RoomCreated.class, null, firstCapacityId).getRecipient();
        PersonInformation permanentParticipant2 =
                checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 1",
                        RoomNotification.RoomCreated.class, null, firstCapacityId).getRecipient();
        checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 2",
                RoomNotification.RoomCreated.class, permanentParticipant2, secondCapacityId);
        PersonInformation capacityParticipant2 = checkNotification(notificationRecords,
                "Capacity Participant 2 - Capacity 1",
                RoomNotification.RoomCreated.class, null, firstCapacityId).getRecipient();
        clearNotificationRecords();

        // Remove Permanent Participant 2 (should be notified about both capacities)
        permanentRoomParticipants.removeParticipant(permanentRoomParticipants.getParticipants().get(1));
        executableService.modifyExecutableConfiguration(SECURITY_TOKEN, permanentRoomId, permanentRoomParticipants);
        executeNotifications();

        // Check deleted notifications
/*        notificationRecords = getNotificationRecords(RoomNotification.class);
        checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 1",
                RoomNotification.RoomDeleted.class, permanentParticipant2, firstCapacityId);
        checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 2",
                RoomNotification.RoomDeleted.class, permanentParticipant2, secondCapacityId);*/
        clearNotificationRecords();

        // Modify permanent room
        permanentRoomReservationRequest =
                getReservationRequest(permanentRoomReservationRequestId, ReservationRequest.class);
        permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);
        firstCapacityReservationRequest = getReservationRequest(
                firstCapacityReservationRequestId, ReservationRequest.class);
        firstCapacityReservation =
                firstCapacityReservationRequest.getLastReservation(getReservationService(), SECURITY_TOKEN);
        firstCapacity = (AbstractRoomExecutable) firstCapacityReservation.getExecutable();
        String newFirstCapacityId = firstCapacity.getId();
        secondCapacityReservationRequest = getReservationRequest(
                secondCapacityReservationRequestId, ReservationRequest.class);
        secondCapacityReservation =
                secondCapacityReservationRequest.getLastReservation(getReservationService(), SECURITY_TOKEN);
        secondCapacity = (AbstractRoomExecutable) secondCapacityReservation.getExecutable();
        String newSecondCapacityId = secondCapacity.getId();
        executeNotifications();

        // Check modified and deleted notifications
        notificationRecords = getNotificationRecords(RoomNotification.class);
        permanentParticipant1 = checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 1",
                RoomNotification.RoomModified.class, null, newFirstCapacityId).getRecipient();
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomModified.class, permanentParticipant1, newSecondCapacityId);
        capacityParticipant1 = checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                RoomNotification.RoomModified.class, null, newFirstCapacityId).getRecipient();
/*        checkNotification(notificationRecords, "Capacity Participant 2 - Capacity 1",
                RoomNotification.RoomDeleted.class, capacityParticipant2, firstCapacityId);*/

        clearNotificationRecords();

        // Delete all
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstCapacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, secondCapacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();
        executeNotifications();

        // Check deleted reservation requests
/*        notificationRecords = getNotificationRecords(RoomNotification.class);
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 1",
                RoomNotification.RoomDeleted.class, permanentParticipant1, newFirstCapacityId);
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomDeleted.class, permanentParticipant1, newSecondCapacityId);
        checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                RoomNotification.RoomDeleted.class, capacityParticipant1, newFirstCapacityId);
        clearNotificationRecords();*/
    }

    /**
     * Test participant notification for started room.
     *
     * @throws Exception
     */
    @Test
    public void testStartedRoomParticipation() throws Exception
    {
        McuTestAgent mcuAgent = getController().addJadeAgent("mcu", new McuTestAgent());

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.H323_E164}));
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN_ROOT, mcu);

        // Create room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request\nTest multiline");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.setParticipantNotificationEnabled(true);
        roomSpecification.addParticipant(new PersonParticipant("Martin Srom", "srom@cesnet.cz"));
        roomSpecification.addParticipant(new PersonParticipant("Ondrej Pavelka", "pavelka@cesnet.cz"));
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);
        runExecutor(DateTime.parse("2012-06-22T14:10"));

        // Modify room
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequestId = allocate(reservationRequest, DateTime.parse("2012-06-22T14:20"));
        checkAllocated(reservationRequestId);
        runExecutor(DateTime.parse("2012-06-22T14:30"));

        // Modify room
        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequestId = allocate(reservationRequest, DateTime.parse("2012-06-22T14:40"));
        checkAllocated(reservationRequestId);
        runExecutor(DateTime.parse("2012-06-22T14:50"));

        // Stop room
        runExecutor(DateTime.parse("2012-06-22T16:30"));

        // Delete room
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler(DateTime.parse("2012-06-22T14:50"));

        // Check executed notifications
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                // Create
                add(ReservationRequestNotification.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomCreated.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomCreated.class);
                // Execute
                add(RoomAvailableNotification.class);
                add(RoomAvailableNotification.class);
                // Modify
                //add(ReservationRequestNotification.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                // Modify
                //add(ReservationRequestNotification.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                add(RoomGroupNotification.class);
                add(RoomNotification.RoomModified.class);
                // Delete
                //add(ReservationRequestNotification.class);
            }}, getNotificationTypes(AbstractNotification.class));
    }

    @Test
    public void testResourceMaintenance() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(SECURITY_TOKEN_USER1, resource);

        ReservationRequest userReservationRequest1 = new ReservationRequest();
        userReservationRequest1.setSlot("2014-01-10T12:00", "PT2H");
        userReservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        userReservationRequest1.setSpecification(new ResourceSpecification(resourceId));
        String userRequest1Id = allocate(SECURITY_TOKEN_USER2, userReservationRequest1);
        checkAllocated(userRequest1Id);
        Assert.assertEquals(1, getSchedulerResult().getAllocatedReservationRequests());
        Assert.assertEquals(0, getSchedulerResult().getFailedReservationRequests());
        //Assert.assertEquals(0, getSchedulerResult().getDeletedReservations());

        ReservationRequest userRequest2 = new ReservationRequest();
        userRequest2.setSlot("2014-01-20T12:00", "PT2H");
        userRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        userRequest2.setSpecification(new ResourceSpecification(resourceId));
        String userRequest2Id = allocate(SECURITY_TOKEN_USER3, userRequest2);
        checkAllocated(userRequest2Id);
        Assert.assertEquals(1, getSchedulerResult().getAllocatedReservationRequests());
        Assert.assertEquals(0, getSchedulerResult().getFailedReservationRequests());
        //Assert.assertEquals(0, getSchedulerResult().getDeletedReservations());

        ReservationRequest maintenanceRequest = new ReservationRequest();
        maintenanceRequest.setSlot("2014-01-01/2014-02-01");
        maintenanceRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        maintenanceRequest.setSpecification(new ResourceSpecification(resourceId));
        String maintenanceRequestId = allocate(SECURITY_TOKEN_USER1, maintenanceRequest);
        checkAllocationFailed(maintenanceRequestId);
        checkAllocated(userRequest1Id);
        checkAllocated(userRequest2Id);
        Assert.assertEquals(0, getSchedulerResult().getAllocatedReservationRequests());
        Assert.assertEquals(1, getSchedulerResult().getFailedReservationRequests());
        //Assert.assertEquals(0, getSchedulerResult().getDeletedReservations());

        maintenanceRequest = getReservationRequest(maintenanceRequestId, ReservationRequest.class);
        maintenanceRequest.setPriority(1);
        maintenanceRequestId = allocate(SECURITY_TOKEN_USER1, maintenanceRequest);
        checkAllocated(maintenanceRequestId);
        checkAllocationFailed(userRequest1Id);
        checkAllocationFailed(userRequest2Id);
        Assert.assertEquals(1, getSchedulerResult().getAllocatedReservationRequests());
        Assert.assertEquals(2, getSchedulerResult().getFailedReservationRequests());
        //Assert.assertEquals(2, getSchedulerResult().getDeletedReservations());

        // Check executed notifications
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                // Create
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                // Maintenance failed
                add(ReservationRequestNotification.class);
                add(AllocationFailedNotification.class);
                // Maintenance succeeds
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                // User requests deleted and failed
                add(ReservationRequestNotification.class);
                //add(ReservationNotification.Deleted.class);
                //add(ReservationNotification.Deleted.class);
                add(AllocationFailedNotification.class);
                add(ReservationRequestNotification.class);
                add(AllocationFailedNotification.class);
            }}, getNotificationTypes(AbstractNotification.class));
    }

    private void clearNotificationRecords()
    {
        notificationExecutor.getNotificationRecords().clear();
    }

    private List<TestingNotificationExecutor.NotificationRecord<AbstractNotification>> getNotificationRecords()
    {
        return getNotificationRecords(AbstractNotification.class);
    }

    private <T extends AbstractNotification> List<TestingNotificationExecutor.NotificationRecord<T>>
    getNotificationRecords(Class<T> notificationType)
    {
        List<TestingNotificationExecutor.NotificationRecord<T>> notificationRecords =
                new LinkedList<TestingNotificationExecutor.NotificationRecord<T>>();
        for (TestingNotificationExecutor.NotificationRecord notificationRecord :
                notificationExecutor.getNotificationRecords()) {
            AbstractNotification notification = notificationRecord.getNotification();
            if (notificationType.isInstance(notification)) {
                notificationRecords.add((TestingNotificationExecutor.NotificationRecord<T>) notificationRecord);
            }
        }
        return notificationRecords;
    }

    private List<Class<? extends AbstractNotification>> getNotificationTypes()
    {
        return getNotificationTypes(AbstractNotification.class);
    }

    private <T extends AbstractNotification> List<Class<? extends T>> getNotificationTypes(Class<T> notificationType)
    {
        List<Class<? extends T>> notificationTypes = new LinkedList<Class<? extends T>>();
        for (TestingNotificationExecutor.NotificationRecord<T> notificationRecord :
                getNotificationRecords(notificationType)) {
            notificationTypes.add((Class<? extends T>) notificationRecord.getNotification().getClass());
        }
        return notificationTypes;
    }

    private TestingNotificationExecutor.NotificationRecord checkNotification(
            List<TestingNotificationExecutor.NotificationRecord<RoomNotification>> notificationRecords,
            String message, Class<? extends AbstractNotification> requiredNotificationType,
            PersonInformation requiredRecipient, String requiredRoomEndpointId)
    {
        TestingNotificationExecutor.NotificationRecord notificationRecord = notificationRecords.get(0);
        PersonInformation recipient = notificationRecord.getRecipient();
        AbstractNotification notification = notificationRecord.getNotification();
        notificationRecords.remove(0);
        if (requiredNotificationType != null) {
            Assert.assertEquals(message + " - NotificationType", requiredNotificationType, notification.getClass());
        }
        if (requiredRoomEndpointId != null) {
            RoomNotification roomNotification = (RoomNotification) notification;
            Long executableId = ObjectIdentifier.parseLocalId(requiredRoomEndpointId, ObjectType.EXECUTABLE);
            Assert.assertEquals(message + " - RoomEndpointId", executableId, roomNotification.getRoomEndpointId());
        }
        if (requiredRecipient != null) {
            Assert.assertEquals(message + " - Recipient", requiredRecipient, recipient);
        }
        return notificationRecord;
    }

    /**
     * Test periodic request for users.
     *
     * @throws Exception
     */
    @Test
    public void testRequestWithPeriod() throws Exception
    {
        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        //userSettings.setLocale(Locale.ENGLISH);
        userSettings.setUseWebService(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationService reservationService = getReservationService();

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture("P1M");
        String aliasProviderId = getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.addSlot(new PeriodicDateTimeSlot("2012-03-03T12:00", "PT1H","P2D", "2012-03-13"));
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-09T00:00"));

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setDescription("Alias Reservation Request");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-03-04T12:00", "PT30M", "P1D", "2012-03-15"));
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-03-03T12:30", "PT30M", "P1D", "2012-03-07"));
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String reservationRequestId = reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequestSet);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-20T00:00"));


        ReservationRequestSet reservationRequestModification = getReservationRequest(reservationRequestId, ReservationRequestSet.class);
        reservationRequestModification.removeSlot(reservationRequestModification.getSlots().get(1));
        reservationRequestId = reservationService.modifyReservationRequest(SECURITY_TOKEN, reservationRequestModification);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-23T00:00"));
    }

    /**
     * Test periodic request for users.
     *
     * @throws Exception
     */
    @Test
    public void testRequestWithoutPeriod() throws Exception
    {
        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(Locale.ENGLISH);
        userSettings.setUseWebService(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationService reservationService = getReservationService();

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.setMaximumFuture("P1M");
        String aliasProviderId = getResourceService().createResource(SECURITY_TOKEN_ROOT, aliasProvider);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.addSlot(new PeriodicDateTimeSlot(DateTime.parse("2012-03-03T12:00"), Period.parse("PT1H"), Period.ZERO, null));
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runPreprocessorAndScheduler(new Interval("2012-03-01T00:00/2012-03-09T00:00"));
    }

    /**
     * Test reservation request with long periodicity with different timezone. Should not detect as modification.
     */
    @Test
    public void testNotificationForDifferentTimezone()
    {
        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(Locale.ENGLISH);
        userSettings.setUseWebService(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationService reservationService = getReservationService();

        Resource meetingRoom = new Resource();
        meetingRoom.setName("Meeting room");
        meetingRoom.setAllocatable(true);
        String meetingRoomId = getResourceService().createResource(SECURITY_TOKEN_USER1, meetingRoom);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setDescription("Meeting Room Reservation Request");
        reservationRequest.setSpecification(new ResourceSpecification(meetingRoomId));
        PeriodicDateTimeSlot slot = new PeriodicDateTimeSlot("2015-02-13T09:30", "PT1H30M", "P1W", "2016-12-31");
        slot.setTimeZone(DateTimeZone.forID("Europe/Prague"));
        reservationRequest.addSlot(slot);
        reservationRequest.setPurpose(ReservationRequestPurpose.USER);

        reservationService.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        DateTime start = new DateTime("2015-04-01T13:46");
        Period lookahead = new Period("P1W");
        Period schedulerPeriod = new Period("PT1H");
        DateTime end = new DateTime("2015-04-08T12:00");
        while(start.isBefore(end)) {
            runPreprocessorAndScheduler(new Interval(start, lookahead));
            start = start.plus(schedulerPeriod);
        }
        for (Class<? extends AbstractNotification> clazz : getNotificationTypes()) {
            Assert.assertNotSame(ReservationNotification.Deleted.class, clazz);
        }
    }

    @Test
    public void testResourceRemainingCapacity() throws Exception {
        DeviceResource tcs = new DeviceResource();
        tcs.setName("tcs");
        tcs.addTechnology(Technology.H323);
        tcs.addTechnology(Technology.SIP);
        tcs.addCapability(new RecordingCapability(3));
        tcs.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN_ROOT, tcs);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("95{digit:1}@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.addAdministratorEmail("pavelka@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN_ROOT, mcu);

        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(UserSettings.LOCALE_CZECH);
        userSettings.setHomeTimeZone(DateTimeZone.forID("+05:00"));
        userSettings.setCurrentTimeZone(DateTimeZone.forID("+06:00"));
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(new Technology[]{Technology.H323, Technology.SIP});
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(8);
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);


        ReservationRequest reservationRequestNew = new ReservationRequest();
        reservationRequestNew.setDescription("Room Reservation Request");
        reservationRequestNew.setSlot("2012-06-22T17:00", "PT2H2M");
        reservationRequestNew.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecificationNew = new RoomSpecification(new Technology[]{Technology.H323, Technology.SIP});
        RoomAvailability roomAvailabilityNew = roomSpecificationNew.createAvailability();
        roomAvailabilityNew.setParticipantCount(2);
        roomAvailabilityNew.addServiceSpecification(new RecordingServiceSpecification(true));
        roomSpecificationNew.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequestNew.setSpecification(roomSpecificationNew);
        String reservationRequestIdNew = allocate(reservationRequestNew);
        checkAllocated(reservationRequestIdNew);

        ReservationRequest reservationRequestOver = new ReservationRequest();
        reservationRequestOver.setDescription("Room Reservation Request");
        reservationRequestOver.setSlot("2012-06-22T14:00", "PT5H3M");
        reservationRequestOver.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecificationOver = new RoomSpecification(new Technology[]{Technology.H323, Technology.SIP});
        RoomAvailability roomAvailabilityOver = roomSpecificationOver.createAvailability();
        roomAvailabilityOver.setParticipantCount(1);
        roomAvailabilityOver.addServiceSpecification(new RecordingServiceSpecification(true));
        roomSpecificationOver.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequestOver.setSpecification(roomSpecificationOver);
        String reservationRequestIdOver = allocate(reservationRequestOver);
        checkAllocated(reservationRequestIdOver);

        runScheduler();

        //TODO: test remaining capacity (should not be <0)
        // 1x system-admin: allocation-failed
        // 4x resource-admin: new, deleted, new, deleted
        // 4x user: changes(allocation-failed), changes (new), changes (deleted, new), changes (deleted)
//        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
//        {{
//                add(ReservationRequestNotification.class);
//                add(AllocationFailedNotification.class);
//                add(ReservationRequestNotification.class);
//                add(ReservationNotification.New.class);
//                add(ReservationRequestNotification.class);
//                add(ReservationNotification.Deleted.class);
//                add(ReservationNotification.New.class);
//                add(ReservationRequestNotification.class);
//                add(ReservationNotification.Deleted.class);
//            }}, getNotificationTypes());
    }

    /**
     * Test for reallocating new reservations after lookahead
     */
    @Test
    public void testPeriodicCapacity() throws Exception
    {
        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(Locale.ENGLISH);
        userSettings.setUseWebService(false);
        userSettings.setCurrentTimeZone(DateTimeZone.forID("UTC"));
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationService reservationService = getReservationService();

        DeviceResource aliasProvider = new DeviceResource();
        aliasProvider.addTechnology(Technology.SIP);
        aliasProvider.setName("aliasProvider");

        aliasProvider.addCapability(new RoomProviderCapability(10));

        aliasProvider.addCapability(new cz.cesnet.shongo.controller.api.RecordingCapability());

        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(new ValueProvider.Pattern("{hash}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        aliasProvider.addCapability(aliasProviderCapability);

        aliasProvider.setAllocatable(true);
        aliasProvider.addAdministratorEmail("pavelka@cesnet.cz");
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setDescription("Alias Reservation Request");
        permanentRoomReservationRequest.setSlot("2015-01-01T08:00", "P1W");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.USER);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(AliasType.SIP_URI));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = reservationService.createReservationRequest(SECURITY_TOKEN_USER1, permanentRoomReservationRequest);
        runPreprocessorAndScheduler(new Interval(new DateTime("2015-01-01T00:00"), new Period("P1D")));
        for (Class<? extends AbstractNotification> clazz : getNotificationTypes()) {
            Assert.assertNotSame(ReservationNotification.Deleted.class, clazz);
        }

        PeriodicDateTimeSlot slot = new PeriodicDateTimeSlot("2015-01-01T09:00", "PT2H10M", "P1D", "2015-01-05");
        slot.setTimeZone(DateTimeZone.forID("Europe/Prague"));


        ReservationRequestSet capacityReservationRequest = new ReservationRequestSet();
        capacityReservationRequest.setDescription("Capacity Reservation Request");
        capacityReservationRequest.addSlot(slot);
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.USER);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId = reservationService.createReservationRequest(SECURITY_TOKEN_USER1, capacityReservationRequest);
        capacityReservationRequest.setId(capacityReservationRequestId);


        DateTime start = new DateTime("2015-01-01T08:00");
        Period lookahead = new Period("P1D");
        Period schedulerPeriod = new Period("PT1H");
        DateTime end = new DateTime("2015-01-01T23:59");
        while(start.isBefore(end)) {
            runPreprocessorAndScheduler(new Interval(start, lookahead));
            start = start.plus(schedulerPeriod);
            for (Class<? extends AbstractNotification> clazz : getNotificationTypes()) {
                Assert.assertNotSame(ReservationNotification.Deleted.class, clazz);
            }
        }

//        capacityReservationRequest.removeSlot(slot);
//        slot = new PeriodicDateTimeSlot("2015-06-09T10:00+02:00", "PT2H10M", "P1W", "2017-05-24");
//        capacityReservationRequest.addSlot(slot);
        RoomSpecification roomSpecification = (RoomSpecification) capacityReservationRequest.getSpecification();
        roomSpecification.getAvailability().addServiceSpecification(new RecordingServiceSpecification(true));
        reservationService.modifyReservationRequest(SECURITY_TOKEN_USER1, capacityReservationRequest);

        runPreprocessorAndScheduler(new Interval(start, lookahead));
        List<Class<? extends AbstractNotification>> notificationTypes = getNotificationTypes();
        Assert.assertTrue(notificationTypes.contains(ReservationRequestNotification.class));
        Assert.assertTrue(notificationTypes.contains(ReservationNotification.New.class));
        //Assert.assertTrue(notificationTypes.contains(ReservationNotification.Deleted.class));
        clearNotificationRecords();

        end = new DateTime("2015-01-05T23:59");
        while(start.isBefore(end)) {
            runPreprocessorAndScheduler(new Interval(start, lookahead));
            start = start.plus(schedulerPeriod);
            for (Class<? extends AbstractNotification> clazz : getNotificationTypes()) {
                Assert.assertNotSame(ReservationNotification.Deleted.class, clazz);
            }
        }
    }
}
