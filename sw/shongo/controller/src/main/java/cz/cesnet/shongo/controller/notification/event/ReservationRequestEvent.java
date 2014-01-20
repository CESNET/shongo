package cz.cesnet.shongo.controller.notification.event;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.notification.Target;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableEvent} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestEvent extends ConfigurableEvent
{
    private String id;

    private String url;

    private DateTime updatedAt;

    private String updatedBy;

    private String description;

    private Target target;

    /**
     * List of {@link AbstractEvent}s which are part of the {@link ReservationEvent}.
     */
    private List<AbstractEvent> events = new LinkedList<AbstractEvent>();

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestEvent(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, ControllerConfiguration configuration)
    {
        super(authorizationManager.getUserSettingsManager(), configuration);

        EntityManager entityManager = authorizationManager.getEntityManager();

        this.id = ObjectIdentifier.formatId(reservationRequest);
        this.url = configuration.getNotificationReservationRequestUrl(this.id);
        this.updatedAt = reservationRequest.getUpdatedAt();
        this.updatedBy = reservationRequest.getUpdatedBy();
        this.description = reservationRequest.getDescription();
        this.target = Target.createInstance(reservationRequest, entityManager);

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER)) {
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
     * @param event to be added to the {@link #events}
     */
    public void addEvent(AbstractEvent event)
    {
        events.add(event);
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected ConfigurableEvent.Configuration createConfiguration(Locale locale, DateTimeZone timeZone,
            boolean administrator)
    {
        return new ParentConfiguration(locale, timeZone, administrator);
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(ConfigurableEvent.Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification",
                this.configuration.getNotificationUserSettingsUrl());

        // Number of child events of each type
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int modifiedReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (AbstractEvent event : events) {
            if (event instanceof AllocationFailedEvent) {
                allocationFailedNotifications++;
            }
            else if (event instanceof ReservationEvent) {
                ReservationEvent reservationNotification = (ReservationEvent) event;
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
                "reservationRequest", renderContext.formatDateTime(updatedAt), targetBuilder));
        titleBuilder.append(" ");
        titleBuilder.append(resultDescriptionBuilder);

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "reservation-request.ftl");
        for (AbstractEvent event : events) {
            NotificationMessage childMessage;
            if (event instanceof ConfigurableEvent) {
                ConfigurableEvent configurableEvent = (ConfigurableEvent) event;
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
