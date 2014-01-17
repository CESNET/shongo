package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.notification.manager.NotificationExecutor;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Tests for notifying about new/modified/deleted {@link Reservation}s by emails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotificationTest extends AbstractControllerTest
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

        getController().getConfiguration().setAdministrators(new LinkedList<PersonInformation>(){{
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
        RoomSpecification roomSpecification = new RoomSpecification(4000,
                new Technology[]{Technology.H323, Technology.SIP});
        roomSpecification.addRoomSetting(new H323RoomSetting().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        ((RoomSpecification) reservationRequest.getSpecification()).getAvailability().setParticipantCount(3);
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
        Assert.assertEquals(9, notificationExecutor.getNotificationCount());
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
        Assert.assertEquals(10, notificationExecutor.getNotificationCount());
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
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
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
        Assert.assertEquals(7, notificationExecutor.getNotificationCount());
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
        Assert.assertEquals(4, notificationExecutor.getNotificationCount()); // new/deleted
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
        Assert.assertEquals(4, notificationExecutor.getNotificationCount()); // new/deleted
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
        Assert.assertEquals(4, notificationExecutor.getNotificationCount());

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequestSet.class);
        reservationRequest.removeSlot(reservationRequest.getSlots().get(1));
        reservationService.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessorAndScheduler(new Interval("2012-01-01T00:00/2012-03-01T00:00"));

        // 1x resource-admin: deleted
        // 1x user: changes (deleted)
        Assert.assertEquals(6, notificationExecutor.getNotificationCount());
    }

    /**
     * {@link cz.cesnet.shongo.controller.notification.manager.NotificationExecutor} for testing.
     */
    private static class TestingNotificationExecutor extends NotificationExecutor
    {
        /**
         * Number of executed notifications.
         */
        private int notificationCount = 0;

        /**
         * @return {@link #notificationCount}
         */
        public int getNotificationCount()
        {
            return notificationCount;
        }

        @Override
        public void executeNotification(Notification notification)
        {
            for (PersonInformation recipient : notification.getRecipients()) {
                NotificationMessage recipientMessage = notification.getRecipientMessage(recipient);
                logger.debug("Notification for {} (reply-to: {})...\nSUBJECT:\n{}\n\nCONTENT:\n{}", new Object[]{
                        recipient, notification.getReplyTo(), recipientMessage.getTitle(), recipientMessage.getContent()
                });
            }
            notificationCount++;
        }
    }
}
