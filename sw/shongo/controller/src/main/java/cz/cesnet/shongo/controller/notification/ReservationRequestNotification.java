package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;

/**
 * {@link cz.cesnet.shongo.controller.notification.Notification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends Notification
{
    cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequest = null;

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestNotification(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        super(null, authorizationManager.getUserSettingsProvider());

        this.reservationRequest = reservationRequest.toApi(false);

        EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequestId, Role.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    @Override
    protected NotificationMessage renderMessage(NotificationConfiguration configuration)
    {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("Changes in reservation request ");
        nameBuilder.append(reservationRequest.getId());

        StringBuilder contentBuilder = new StringBuilder();
        for (Notification notification : getChildNotifications()) {
            NotificationMessage childMessage = notification.renderMessage(configuration);
            contentBuilder.append(childMessage.getName());
            contentBuilder.append("\n");
            contentBuilder.append(childMessage.getContent());
        }
        return new NotificationMessage(nameBuilder.toString(), contentBuilder.toString());
    }
}
