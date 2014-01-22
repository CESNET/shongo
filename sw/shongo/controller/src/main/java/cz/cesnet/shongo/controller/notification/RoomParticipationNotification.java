package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.notification.manager.NotificationManager;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link ConfigurableNotification} for {@link RoomEndpoint} participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomParticipationNotification extends ConfigurableNotification
{
    private Interval slot;

    private Map<PersonInformation, AbstractParticipant> participantByPerson = new HashMap<PersonInformation, AbstractParticipant>();

    public RoomParticipationNotification(RoomEndpoint roomEndpoint, AuthorizationManager authorizationManager)
    {
        super(authorizationManager.getUserSettingsManager());

        slot = roomEndpoint.getSlot();

        for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                PersonInformation personInformation = personParticipant.getPersonInformation();
                addRecipient(personInformation);
                participantByPerson.put(personInformation, participant);
            }
        }
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration,
            NotificationManager manager)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification",
                manager.getConfiguration());
        renderContext.addParameter("slot", slot);

        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append("Participant Notification Title");

        return renderMessageFromTemplate(renderContext, titleBuilder.toString(), "room-participation.ftl");
    }

    @Override
    public NotificationRecord createRecordForRecipient(PersonInformation recipient, EntityManager entityManager)
    {
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setCreatedAt(getCreatedAt());
        notificationRecord.setRecipientType(NotificationRecord.RecipientType.USER);
        notificationRecord.setRecipientId(0l);
        notificationRecord.setNotificationType(NotificationRecord.NotificationType.ROOM_CREATED);
        notificationRecord.setTargetId(participantByPerson.get(recipient).getId());
        return notificationRecord;
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }
}
