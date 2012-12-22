package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.notification.NotificationExecutor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for notifying about new {@link Reservation}s by emails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotifyReservationTest extends AbstractControllerTest
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
    public void test() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
        mcu.addCapability(new AliasProviderCapability(AliasType.SIP_URI, "950000001@cesnet.cz", true));
        mcu.setAllocatable(true);
        mcu.addAdministrator(new OtherPerson("Martin Srom", "cheater@seznam.cz"));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setName("Testing request");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H1M");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(4, new Technology[]{Technology.H323, Technology.SIP}));

        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        ((RoomSpecification)reservationRequest.getSpecification()).setParticipantCount(5);
        allocateAndCheck(reservationRequest);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        runScheduler();

        Assert.assertEquals(notificationExecutor.getSentCount(), 3); // new/modified/deleted
    }

    /**
     * {@link NotificationExecutor} for testing.
     */
    private static class TestingNotificationExecutor extends NotificationExecutor
    {
        /**
         * Number of already sent emails
         */
        private int sentCount = 0;

        /**
         * @return {@link #sentCount}
         */
        public int getSentCount()
        {
            return sentCount;
        }

        @Override
        public void executeNotification(Notification notification)
        {
            StringBuilder recipientString = new StringBuilder();
            for (cz.cesnet.shongo.controller.common.Person recipient : notification.getRecipients()) {
                if (recipientString.length() > 0) {
                    recipientString.append(", ");
                }
                recipientString.append(String.format("%s (%s)", recipient.getInformation().getFullName(),
                        recipient.getInformation().getPrimaryEmail()));
            }
            logger.debug("Notification '{}' for {}...\n{}", new Object[]{notification.getName(),
                    recipientString.toString(), notification.getContent()});
            sentCount++;
        }
    }
}
