package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Tests for notifying about new {@link cz.cesnet.shongo.controller.booking.request.ReservationRequest}s for confirmation by emails.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ReservationRequestConfirmationNotificationTest extends AbstractExecutorTest
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

    @Test
    public void testRequestWithConfirmation() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setConfirmByOwner(true);
        resource.setAllocatable(true);
        String resourceId = createResource(SECURITY_TOKEN_USER1, resource);

        ReservationRequest userReservationRequest1 = new ReservationRequest();
        userReservationRequest1.setSlot("2014-01-10T12:00", "PT2H");
        userReservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        userReservationRequest1.setSpecification(new ResourceSpecification(resourceId));
        String userRequest1Id = allocate(SECURITY_TOKEN_USER2, userReservationRequest1);
        Assert.assertEquals(0, getSchedulerResult().getAllocatedReservationRequests());
        Assert.assertEquals(0, getSchedulerResult().getFailedReservationRequests());
        Assert.assertEquals(0, getSchedulerResult().getDeletedReservations());


    }
}
