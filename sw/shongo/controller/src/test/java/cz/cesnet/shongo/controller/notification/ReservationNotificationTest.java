package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
        super.before();
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
     * Test single technology virtual room.
     *
     * @throws Exception
     */
    @Test
    public void testRoom() throws Exception
    {
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
        mcu.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(UserSettings.LOCALE_CZECH);
        userSettings.setHomeTimeZone(DateTimeZone.forID("+05:00"));
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
                add(AllocationFailedNotification.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        aliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

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
        roomSpecification.getEstablishment().getAliasSpecifications().get(0).setAliasTypes(new HashSet<AliasType>()
        {{
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
                add(AllocationFailedNotification.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        aliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

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
        aliasSpecification.setAliasTypes(new HashSet<AliasType>()
        {{
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
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        firstAliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("001");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, secondAliasProvider);

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
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        firstAliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

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
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
            }}, getNotificationTypes());
    }

    @Test
    public void testReusedAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        aliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

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
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        aliasProvider.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

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
                add(ReservationNotification.New.class);
                add(ReservationRequestNotification.class);
                add(ReservationNotification.New.class);
                add(AllocationFailedNotification.class);
            }}, getNotificationTypes());
        clearNotificationRecords();

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequestSet.class);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        reservationService.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessorAndScheduler(new Interval("2012-01-01T00:00/2012-03-01T00:00"));

        // 1x resource-admin: deleted
        // 1x user: changes (deleted)
        Assert.assertEquals(new ArrayList<Class<? extends AbstractNotification>>()
        {{
                add(ReservationNotification.Deleted.class);
                add(ReservationRequestNotification.class);
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
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.addAdministrator(new AnonymousPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request");
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

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Class<? extends RoomNotification>>()
        {{
                add(RoomNotification.RoomCreated.class);
                add(RoomNotification.RoomCreated.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomDeleted.class);
                add(RoomNotification.RoomDeleted.class);
            }}, getNotificationTypes(RoomNotification.class));
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
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create Permanent Room with Permanent Participant 1
        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setDescription("Alias Reservation Request");
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification permanentRoomSpecification = new RoomSpecification(Technology.H323);
        permanentRoomSpecification.addParticipant(new PersonParticipant("Martin Srom", "srom@cesnet.cz"));
        permanentRoomReservationRequest.setSpecification(permanentRoomSpecification);
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        AbstractRoomExecutable permanentRoom = (AbstractRoomExecutable) permanentRoomReservation.getExecutable();
        String permanentRoomId = permanentRoom.getId();

        // Create Capacity 1 with Capacity Participant 1
        ReservationRequest firstCapacityReservationRequest = new ReservationRequest();
        firstCapacityReservationRequest.setDescription("Capacity Reservation Request");
        firstCapacityReservationRequest.setSlot("2012-01-01T12:00", "PT1H");
        firstCapacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstCapacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        RoomSpecification firstCapacitySpecification = new RoomSpecification();
        RoomAvailability roomAvailability = firstCapacitySpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.setParticipantNotificationEnabled(true);
        firstCapacitySpecification.addParticipant(new PersonParticipant("Ondrej Pavelka", "pavelka@cesnet.cz"));
        firstCapacityReservationRequest.setSpecification(firstCapacitySpecification);
        String firstCapacityReservationRequestId = allocate(firstCapacityReservationRequest);
        Reservation firstCapacityReservation = checkAllocated(firstCapacityReservationRequestId);
        AbstractRoomExecutable firstCapacity = (AbstractRoomExecutable) firstCapacityReservation.getExecutable();
        String firstCapacityId = firstCapacity.getId();

        // Create Capacity 2 without participants
        ReservationRequest secondCapacityReservationRequest = new ReservationRequest();
        secondCapacityReservationRequest.setDescription("Capacity Reservation Request");
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

        // Add Permanent Participant 2 (should be notified about both capacities)
        RoomExecutableParticipantConfiguration permanentRoomParticipants = permanentRoom.getParticipantConfiguration();
        permanentRoomParticipants.addParticipant(new PersonParticipant("Jan Ruzicka", "janru@cesnet.cz"));
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
        PersonInformation capacityParticipant1 =
                checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                        RoomNotification.RoomCreated.class, null, firstCapacityId).getRecipient();
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomCreated.class, permanentParticipant1, secondCapacityId);
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
        notificationRecords = getNotificationRecords(RoomNotification.class);
        checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 1",
                RoomNotification.RoomDeleted.class, permanentParticipant2, firstCapacityId);
        checkNotification(notificationRecords, "Permanent Participant 2 - Capacity 2",
                RoomNotification.RoomDeleted.class, permanentParticipant2, secondCapacityId);
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

        // Check modified and deleted notifications
        notificationRecords = getNotificationRecords(RoomNotification.class);
        permanentParticipant1 = checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 1",
                RoomNotification.RoomModified.class, null, newFirstCapacityId).getRecipient();
        capacityParticipant1 = checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                RoomNotification.RoomModified.class, null, newFirstCapacityId).getRecipient();
        checkNotification(notificationRecords, "Capacity Participant 2 - Capacity 1",
                RoomNotification.RoomDeleted.class, capacityParticipant2, firstCapacityId);
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomModified.class, permanentParticipant1, newSecondCapacityId);
        clearNotificationRecords();

        // Delete all
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstCapacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, secondCapacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();

        // Check deleted reservation requests
        notificationRecords = getNotificationRecords(RoomNotification.class);
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 1",
                RoomNotification.RoomDeleted.class, permanentParticipant1, newFirstCapacityId);
        checkNotification(notificationRecords, "Capacity Participant 1 - Capacity 1",
                RoomNotification.RoomDeleted.class, capacityParticipant1, newFirstCapacityId);
        checkNotification(notificationRecords, "Permanent Participant 1 - Capacity 2",
                RoomNotification.RoomDeleted.class, permanentParticipant1, newSecondCapacityId);
        clearNotificationRecords();
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
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request");
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

        // Delete room
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler(DateTime.parse("2012-06-22T14:50"));

        // Check performed actions on connector agents
        Assert.assertEquals(new ArrayList<Class<? extends RoomNotification>>()
        {{
                add(RoomNotification.RoomCreated.class);
                add(RoomNotification.RoomCreated.class);
                add(RoomNotification.RoomAvailable.class);
                add(RoomNotification.RoomAvailable.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomModified.class);
                add(RoomNotification.RoomDeleted.class);
                add(RoomNotification.RoomDeleted.class);
            }}, getNotificationTypes(RoomNotification.class));
    }

    private void clearNotificationRecords()
    {
        notificationExecutor.notificationRecords.clear();
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
                notificationExecutor.notificationRecords) {
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
            Long executableId = ObjectIdentifier.parseId(
                    cz.cesnet.shongo.controller.booking.executable.Executable.class, requiredRoomEndpointId);
            Assert.assertEquals(message + " - RoomEndpointId", executableId, roomNotification.getRoomEndpointId());
        }
        if (requiredRecipient != null) {
            Assert.assertEquals(message + " - Recipient", requiredRecipient, recipient);
        }
        return notificationRecord;
    }

    /**
     * {@link NotificationExecutor} for testing.
     */
    private class TestingNotificationExecutor extends NotificationExecutor
    {
        /**
         * Executed {@link AbstractNotification}.
         */
        private List<NotificationRecord> notificationRecords = new LinkedList<NotificationRecord>();

        /**
         * @return size of {@link #notificationRecords}
         */
        public int getNotificationCount()
        {
            return notificationRecords.size();
        }

        @Override
        public void executeNotification(PersonInformation recipient, AbstractNotification notification,
                NotificationManager manager, EntityManager entityManager)
        {
            NotificationMessage recipientMessage = notification.getMessage(recipient, manager, entityManager);
            logger.debug("Notification for {} (reply-to: {})...\nSUBJECT:\n{}\n\nCONTENT:\n{}", new Object[]{
                    recipient, notification.getReplyTo(), recipientMessage.getTitle(), recipientMessage.getContent()
            });
            notificationRecords.add(new NotificationRecord<AbstractNotification>(recipient, notification));
        }

        private class NotificationRecord<T extends AbstractNotification>
        {
            private final PersonInformation recipient;

            private final T notification;

            private NotificationRecord(PersonInformation recipient, T notification)
            {
                this.recipient = recipient;
                this.notification = notification;
            }

            public PersonInformation getRecipient()
            {
                return recipient;
            }

            public T getNotification()
            {
                return notification;
            }
        }
    }
}
