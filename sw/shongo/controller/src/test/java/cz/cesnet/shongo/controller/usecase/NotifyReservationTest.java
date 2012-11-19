package cz.cesnet.shongo.controller.usecase;

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
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setName("Testing");
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequest);

        Assert.assertEquals(notificationExecutor.getSentCount(), 1);
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
            logger.debug("Notification '{}'...\n{}", new Object[]{notification.getName(),
                    getNotificationAsString(notification)
            });
            sentCount++;
        }
    }
}
