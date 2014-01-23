package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.notification.manager.NotificationManager;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for {@link RoomEndpoint} participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class RoomParticipationNotification extends ConfigurableNotification
{
    protected Map<PersonInformation, PersonParticipant> participantByPerson =
            new HashMap<PersonInformation, PersonParticipant>();

    public RoomParticipationNotification(AuthorizationManager authorizationManager)
    {
        super(authorizationManager.getUserSettingsManager());
    }

    protected abstract NotificationRecord.NotificationType getNotificationType();

    protected abstract Long getTargetId();

    protected void addRecipient(PersonParticipant personParticipant)
    {
        PersonInformation personInformation = personParticipant.getPersonInformation();
        addRecipient(personInformation);
        this.participantByPerson.put(personInformation, personParticipant);
    }

    @Override
    public boolean removeRecipient(PersonInformation recipient)
    {
        participantByPerson.remove(recipient);
        return super.removeRecipient(recipient);
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append("Participant Notification - ");
        titleBuilder.append(getNotificationType());

        return renderTemplateMessage(renderContext, titleBuilder.toString(), "room-participation.ftl");
    }

    @Override
    public NotificationRecord createRecord(PersonInformation recipient, EntityManager entityManager)
    {
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setCreatedAt(getCreatedAt());
        notificationRecord.setRecipientType(NotificationRecord.RecipientType.PARTICIPANT);
        notificationRecord.setRecipientId(participantByPerson.get(recipient).getId());
        notificationRecord.setNotificationType(getNotificationType());
        notificationRecord.setTargetId(getTargetId());
        return notificationRecord;
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    public static abstract class Abstract extends RoomParticipationNotification
    {
        private RoomEndpoint roomEndpoint;

        private Abstract(RoomEndpoint roomEndpoint, AuthorizationManager authorizationManager)
        {
            super(authorizationManager);

            this.roomEndpoint = roomEndpoint;

            for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                if (participant instanceof PersonParticipant) {
                    PersonParticipant personParticipant = (PersonParticipant) participant;
                    addRecipient(personParticipant);
                }
            }
        }

        public RoomEndpoint getRoomEndpoint()
        {
            return roomEndpoint;
        }

        @Override
        protected Long getTargetId()
        {
            return roomEndpoint.getId();
        }
    }

    public static abstract class Simple extends Abstract
    {
        private Simple(RoomEndpoint roomEndpoint, AuthorizationManager authorizationManager)
        {
            super(roomEndpoint, authorizationManager);
        }

        public abstract Long getReservationRequestId();
    }

    public static class Created extends Simple
    {
        private RoomReservation roomReservation;

        public Created(RoomReservation roomReservation, AuthorizationManager authorizationManager)
        {
            super((RoomEndpoint) roomReservation.getExecutable(), authorizationManager);

            this.roomReservation = roomReservation;
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_CREATED;
        }

        @Override
        public Long getReservationRequestId()
        {
            return roomReservation.getTopReservation().getReservationRequest().getId();
        }
    }

    public static class Deleted extends Simple
    {
        private Long reservationRequestId;

        public Deleted(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
                AuthorizationManager authorizationManager)
        {
            super(roomEndpoint, authorizationManager);

            this.reservationRequestId = (reservationRequest != null ? reservationRequest.getId() : null);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_DELETED;
        }

        @Override
        public Long getReservationRequestId()
        {
            return reservationRequestId;
        }
    }

    public static class Modified extends RoomParticipationNotification
    {
        private Deleted deleted;

        private Created created;

        public Modified(Deleted deleted, Created created, AuthorizationManager authorizationManager)
        {
            super(authorizationManager);
            this.deleted = deleted;
            this.created = created;

            Set<PersonInformation> recipients = new HashSet<PersonInformation>(deleted.getRecipients());
            recipients.retainAll(created.getRecipients());
            for (PersonInformation recipient : recipients) {
                addRecipient(this.created.participantByPerson.get(recipient));
            }
            deleted.removeRecipients(recipients);
            created.removeRecipients(recipients);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_MODIFIED;
        }

        @Override
        protected Long getTargetId()
        {
            return created.getTargetId();
        }
    }

    public static class Available extends Abstract
    {
        public Available(RoomEndpoint roomEndpoint, AuthorizationManager authorizationManager)
        {
            super(roomEndpoint, authorizationManager);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_AVAILABLE;
        }
    }
}

