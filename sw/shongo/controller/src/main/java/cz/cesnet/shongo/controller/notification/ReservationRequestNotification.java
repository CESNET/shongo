package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
    private String id;

    private String url;

    private DateTime updatedAt;

    private String updatedBy;

    private String description;

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
        super(authorizationManager.getUserSettingsProvider());

        EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
        this.id = reservationRequestId.toId();
        this.url = configuration.getReservationRequestUrl(this.id);
        this.updatedAt = reservationRequest.getUpdatedAt();
        this.updatedBy = reservationRequest.getUpdatedBy();
        this.description = reservationRequest.getDescription();

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequestId, Role.OWNER)) {
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
        return new Configuration(locale, timeZone, administrator);
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(ConfigurableNotification.Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification");

        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(renderContext.message("reservationRequest"));
        titleBuilder.append(" ");
        titleBuilder.append(id);

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

    /**
     * {@link Configuration} for {@link ReservationRequestNotification}.
     * <p/>
     * We need a new class of {@link ConfigurableNotification.Configuration} because we want
     * the child {@link ReservationNotification}s to render in a different way when they are rendered from this class
     * (rendered content is cached by equal {@link ConfigurableNotification.Configuration}s).
     */
    public static class Configuration extends ConfigurableNotification.Configuration
    {
        /**
         * Constructor.
         *
         * @param locale        sets the {@link #locale}
         * @param timeZone      sets the {@link #timeZone}
         * @param administrator sets the {@link #administrator}
         */
        public Configuration(Locale locale, DateTimeZone timeZone, boolean administrator)
        {
            super(locale, timeZone, administrator);
        }
    }
}
