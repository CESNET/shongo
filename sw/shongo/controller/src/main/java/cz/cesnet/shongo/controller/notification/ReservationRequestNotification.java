package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import org.joda.time.DateTimeZone;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends AbstractReservationRequestNotification
{

    private Target target;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<AbstractNotification> events = new LinkedList<AbstractNotification>();

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestNotification(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, ControllerConfiguration configuration)
    {
        super(reservationRequest, authorizationManager.getUserSettingsManager(), configuration);

        EntityManager entityManager = authorizationManager.getEntityManager();

        this.target = Target.createInstance(reservationRequest, entityManager);

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    /**
     * @param event to be added to the {@link #events}
     */
    public void addEvent(AbstractNotification event)
    {
        events.add(event);
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
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

        // Number of child events of each type
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int modifiedReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (AbstractNotification event : events) {
            if (event instanceof AllocationFailedNotification) {
                allocationFailedNotifications++;
            }
            else if (event instanceof ReservationNotification) {
                ReservationNotification reservationNotification = (ReservationNotification) event;
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
                throw new TodoImplementException(event.getClass());
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
        int totalNotifications = events.size();
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
                "reservationRequest", renderContext.formatDateTime(getReservationRequestUpdatedAt()), targetBuilder));
        titleBuilder.append(" ");
        titleBuilder.append(resultDescriptionBuilder);

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "reservation-request.ftl");
        for (AbstractNotification event : events) {
            NotificationMessage childMessage;
            if (event instanceof ConfigurableNotification) {
                ConfigurableNotification configurableEvent = (ConfigurableNotification) event;
                childMessage = configurableEvent.renderMessageForConfiguration(configuration);
            }
            else {
                throw new TodoImplementException(event.getClass());
            }
            message.appendChildMessage(childMessage);
        }
        return message;
    }
}
