package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;

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
    protected void onInit()
    {
        super.onInit();

        getController().addNotificationExecutor(notificationExecutor);
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
        mcu.addAdministrator(new OtherPerson("Martin Srom", "cheater@seznam.cz"));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        UserSettings userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN);
        userSettings.setLocale(UserSettings.LOCALE_CZECH);
        userSettings.setTimeZone(DateTimeZone.forID("+05:00"));
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN, userSettings);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Room Reservation Request");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new RoomSpecification(4, new Technology[]{Technology.H323, Technology.SIP}));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        ((RoomSpecification) reservationRequest.getSpecification()).setParticipantCount(5);
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN_ROOT, reservationRequestId);
        runScheduler();

        Assert.assertEquals(4, notificationExecutor.getNotificationCount()); // new/deleted/new/deleted
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
        aliasProvider.addCapability(new AliasProviderCapability("001", AliasType.H323_E164));
        aliasProvider.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI));
        aliasProvider.setAllocatable(true);
        aliasProvider.addAdministrator(new OtherPerson("Martin Srom", "cheater@seznam.cz"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("Alias Reservation Request");
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        ((AliasSpecification) reservationRequest.getSpecification()).setAliasTypes(new HashSet<AliasType>()
        {{
                add(AliasType.SIP_URI);
            }});
        reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        Assert.assertEquals(4, notificationExecutor.getNotificationCount()); // new/deleted/new/deleted
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
        firstAliasProvider.addAdministrator(new OtherPerson("Martin Srom", "martin.srom@cesnet.cz"));
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("001");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.addAdministrator(new OtherPerson("Martin Srom", "martin.srom@cesnet.cz"));
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

        Assert.assertEquals(2, notificationExecutor.getNotificationCount()); // new/deleted
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
        firstAliasProvider.addAdministrator(new OtherPerson("Martin Srom", "martin.srom@cesnet.cz"));
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

        Assert.assertEquals(2, notificationExecutor.getNotificationCount()); // new/deleted
    }

    /**
     * {@link NotificationExecutor} for testing.
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
                logger.debug("Notification '{}' for {}...\n{}", new Object[]{recipientMessage.getName(),
                        recipient, recipientMessage.getContent()
                });
            }
            notificationCount++;
        }
    }
}
