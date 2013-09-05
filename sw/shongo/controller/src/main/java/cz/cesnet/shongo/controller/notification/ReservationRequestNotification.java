package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends ConfigurableNotification
{
    /**
     * Available {@link java.util.Locale}s for {@link ReservationNotification}.
     */
    public static List<Locale> AVAILABLE_LOCALES = new LinkedList<Locale>(){{
        add(cz.cesnet.shongo.controller.api.UserSettings.LOCALE_ENGLISH);
        add(cz.cesnet.shongo.controller.api.UserSettings.LOCALE_CZECH);
    }};

    private cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequest = null;

    /**
     * List of {@link Notification}s which are part of the {@link ReservationNotification}.
     */
    private List<Notification> notifications = new LinkedList<Notification>();

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestNotification(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        super(authorizationManager.getUserSettingsProvider());

        this.reservationRequest = reservationRequest.toApi(false);

        EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequestId, Role.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(ReservationNotification notification)
    {
        notifications.add(notification);
    }

    @Override
    protected List<Locale> getAvailableLocals()
    {
        return AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration)
    {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("Changes in reservation request ");
        nameBuilder.append(reservationRequest.getId());

        StringBuilder contentBuilder = new StringBuilder();
        for (Notification notification : notifications) {
            NotificationMessage notificationMessage;
            if (notification instanceof ConfigurableNotification) {
                ConfigurableNotification configurableNotification =
                        (ConfigurableNotification) notification;
                notificationMessage = configurableNotification.renderMessageForConfiguration(configuration);
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
            contentBuilder.append(notificationMessage.getTitle());
            contentBuilder.append("\n");
            contentBuilder.append(notificationMessage.getContent());
        }
        return new NotificationMessage(nameBuilder.toString(), contentBuilder.toString());
    }
}
