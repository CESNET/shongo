package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends ConfigurableNotification
{
    private String id;

    private String url;

    private DateTime updatedAt;

    private String updatedBy;

    private String description;

    private Target target;

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
            AuthorizationManager authorizationManager, cz.cesnet.shongo.controller.Configuration configuration)
    {
        super(authorizationManager.getUserSettingsProvider(), configuration);

        this.id = EntityIdentifier.formatId(reservationRequest);
        this.url = configuration.getNotificationReservationRequestUrl(this.id);
        this.updatedAt = reservationRequest.getUpdatedAt();
        this.updatedBy = reservationRequest.getUpdatedBy();
        this.description = reservationRequest.getDescription();
        this.target = Target.createInstance(reservationRequest.getSpecification());

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, Role.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    public String getId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public DateTime getUpdatedAt()
    {
        return updatedAt;
    }

    public String getUpdatedBy()
    {
        return updatedBy;
    }

    public String getDescription()
    {
        return description;
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(Notification notification)
    {
        notifications.add(notification);
    }

    @Override
    protected ConfigurableNotification.Configuration createConfiguration(Locale locale, DateTimeZone timeZone,
            boolean administrator)
    {
        return new ParentConfiguration(locale, timeZone, administrator);
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(ConfigurableNotification.Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification",
                this.configuration.getNotificationUserSettingsUrl());

        // Number of child notifications of each type
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int modifiedReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (Notification notification : notifications) {
            if (notification instanceof AllocationFailedNotification) {
                allocationFailedNotifications++;
            }
            else if (notification instanceof ReservationNotification) {
                ReservationNotification reservationNotification = (ReservationNotification) notification;
                switch (reservationNotification.getType()) {
                    case NEW:
                        newReservationNotifications++;
                        break;
                    case MODIFIED:
                        modifiedReservationNotifications++;
                        break;
                    case DELETED:
                        deletedReservationNotifications++;
                        break;
                    default:
                        throw new TodoImplementException(reservationNotification.getType());
                }
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
        }

        // Description of reservation request target
        StringBuilder targetBuilder = new StringBuilder();
        targetBuilder.append(renderContext.message("reservationRequest.for." + target.getType()));
        if (target instanceof Target.Room) {
            Target.Room room = (Target.Room) target;
            String roomName = room.getName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }
        else if (target instanceof Target.Alias) {
            Target.Alias alias = (Target.Alias) target;
            String roomName = alias.getRoomName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }

        // Description of the reservation request result
        StringBuilder resultDescriptionBuilder = new StringBuilder();
        int totalNotifications = notifications.size();
        if (totalNotifications == allocationFailedNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.failed"));
        }
        else if (totalNotifications == newReservationNotifications
                || totalNotifications == modifiedReservationNotifications
                || totalNotifications == deletedReservationNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
        }
        else {
            Map<String, Integer> childrenTypes = new HashMap<String, Integer>();
            childrenTypes.put("reservationRequest.child.failed", allocationFailedNotifications);
            childrenTypes.put("reservationRequest.child.new", newReservationNotifications);
            childrenTypes.put("reservationRequest.child.modified", modifiedReservationNotifications);
            childrenTypes.put("reservationRequest.child.deleted", deletedReservationNotifications);

            if (allocationFailedNotifications > 0) {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.partialSuccess"));
            }
            else {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
            }
            resultDescriptionBuilder.append(" (");
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.child"));
            resultDescriptionBuilder.append(": ");
            boolean separator = false;
            for (Map.Entry<String, Integer> entry : childrenTypes.entrySet()) {
                if (entry.getValue() == 0) {
                    continue;
                }
                if (separator) {
                    resultDescriptionBuilder.append(", ");
                }
                resultDescriptionBuilder.append(renderContext.message(
                        entry.getKey(), entry.getValue()));
                separator = true;
            }
            resultDescriptionBuilder.append(")");
        }

        // Build notification title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(renderContext.message(
                "reservationRequest", renderContext.formatDateTime(updatedAt), targetBuilder));
        titleBuilder.append(" ");
        titleBuilder.append(resultDescriptionBuilder);

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "reservation-request.ftl");
        for (Notification notification : notifications) {
            NotificationMessage childMessage;
            if (notification instanceof ConfigurableNotification) {
                ConfigurableNotification configurableNotification =
                        (ConfigurableNotification) notification;
                childMessage = configurableNotification.renderMessageForConfiguration(configuration);
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
            message.appendChildMessage(childMessage);
        }
        return message;
    }
}
