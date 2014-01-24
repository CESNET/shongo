package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for {@link RoomEndpoint} participants.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class RoomNotification extends ConfigurableNotification
{
    /**
     * Map of {@link AbstractParticipant}s by recipient {@link PersonInformation}s.
     */
    protected Map<PersonInformation, PersonParticipant> participantByPerson =
            new HashMap<PersonInformation, PersonParticipant>();

    /**
     * @param personParticipant to be added as recipient
     */
    protected void addRecipient(PersonParticipant personParticipant)
    {
        PersonInformation personInformation = personParticipant.getPersonInformation();
        addRecipient(personInformation);
        this.participantByPerson.put(personInformation, personParticipant);
    }

    /**
     * @return {@link NotificationRecord.NotificationType} for creation of {@link NotificationRecord}
     */
    protected abstract NotificationRecord.NotificationType getNotificationType();

    /**
     * @return {@link NotificationRecord#targetId} for creation of {@link NotificationRecord}
     */
    protected abstract Long getTargetId();

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

    /**
     * {@link RoomNotification} for a single {@link RoomEndpoint}.
     */
    public static abstract class SimpleRoomNotification extends RoomNotification
    {
        /**
         * @see RoomEndpoint
         */
        protected RoomEndpoint roomEndpoint;

        /**
         * For which is this {@link #roomEndpoint} allocated.
         */
        protected ReservationRequest reservationRequest;

        /**
         * Constructor.
         *
         * @param roomEndpoint sets the {@link #roomEndpoint}
         */
        private SimpleRoomNotification(RoomEndpoint roomEndpoint)
        {
            this.roomEndpoint = roomEndpoint;

            for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                if (participant instanceof PersonParticipant) {
                    PersonParticipant personParticipant = (PersonParticipant) participant;
                    addRecipient(personParticipant);
                }
            }
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint sets the {@link #roomEndpoint}
         * @param participant  adds recipient
         */
        private SimpleRoomNotification(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            this.roomEndpoint = roomEndpoint;

            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                addRecipient(personParticipant);
            }
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint       sets the {@link #roomEndpoint}
         * @param reservationRequest sets the {@link #reservationRequest}
         */
        private SimpleRoomNotification(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest)
        {
            this(roomEndpoint);

            this.reservationRequest = reservationRequest;
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint       sets the {@link #roomEndpoint}
         * @param reservationRequest sets the {@link #reservationRequest}
         * @param participant        adds recipient
         */
        private SimpleRoomNotification(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
                AbstractParticipant participant)
        {
            this(roomEndpoint, participant);

            this.reservationRequest = reservationRequest;
        }


        @Override
        protected void onAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            if (reservationRequest == null) {
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                Reservation reservation = executableManager.getReservation(roomEndpoint);
                if (reservation != null) {
                    Allocation allocation = reservation.getAllocation();
                    if (allocation != null) {
                        AbstractReservationRequest reservationRequest = allocation.getReservationRequest();
                        if (reservationRequest instanceof ReservationRequest) {
                            this.reservationRequest = (ReservationRequest) reservationRequest;
                        }
                    }
                }
            }
            super.onAdded(notificationManager, entityManager);
        }

        @Override
        protected Long getTargetId()
        {
            return roomEndpoint.getId();
        }
    }

    public static class RoomCreated extends SimpleRoomNotification
    {
        public RoomCreated(RoomEndpoint roomEndpoint)
        {
            super(roomEndpoint);
        }

        public RoomCreated(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            super(roomEndpoint, participant);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_CREATED;
        }

        @Override
        protected void onAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onAdded(notificationManager, entityManager);

            if (reservationRequest != null) {
                Long reservationRequestId = reservationRequest.getId();
                List<RoomNotification> roomNotifications =
                        notificationManager.getRoomNotificationsByReservationRequestId(reservationRequestId);
                RoomDeleted roomDeleted = null;
                for (RoomNotification roomNotification : roomNotifications) {
                    if (roomNotification instanceof RoomDeleted &&
                            !roomNotification.getTargetId().equals(getTargetId())) {
                        roomDeleted = (RoomDeleted) roomNotification;
                        break;
                    }
                }
                if (roomDeleted != null) {
                    RoomNotification modifiedNotification = new RoomModified(roomDeleted, this);
                    if (modifiedNotification.hasRecipients()) {
                        notificationManager.addNotification(modifiedNotification, entityManager);
                    }
                }
                else {
                    roomNotifications.add(this);
                }
            }
        }

        @Override
        protected void onRemoved(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onRemoved(notificationManager, entityManager);

            if (reservationRequest != null) {
                notificationManager.getRoomNotificationsByReservationRequestId(reservationRequest.getId()).remove(this);
            }
        }
    }

    public static class RoomDeleted extends SimpleRoomNotification
    {
        public RoomDeleted(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest)
        {
            super(roomEndpoint, reservationRequest);
        }

        public RoomDeleted(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            super(roomEndpoint, participant);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_DELETED;
        }

        @Override
        protected void onAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onAdded(notificationManager, entityManager);

            if (reservationRequest != null) {
                Long reservationRequestId = reservationRequest.getId();
                List<RoomNotification> roomNotifications =
                        notificationManager.getRoomNotificationsByReservationRequestId(reservationRequestId);
                RoomCreated roomCreated = null;
                for (RoomNotification roomNotification : roomNotifications) {
                    if (roomNotification instanceof RoomCreated &&
                            !roomNotification.getTargetId().equals(getTargetId())) {
                        roomCreated = (RoomCreated) roomNotification;
                        break;
                    }
                }
                if (roomCreated != null) {
                    RoomNotification modifiedNotification = new RoomModified(this, roomCreated);
                    if (modifiedNotification.hasRecipients()) {
                        notificationManager.addNotification(modifiedNotification, entityManager);
                    }
                }
                else {
                    roomNotifications.add(this);
                }
            }
        }

        @Override
        protected void onRemoved(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onRemoved(notificationManager, entityManager);

            if (reservationRequest != null) {
                notificationManager.getRoomNotificationsByReservationRequestId(reservationRequest.getId()).remove(this);
            }
        }
    }

    public static class RoomModified extends RoomNotification
    {
        private RoomDeleted roomDeleted;

        private RoomCreated roomCreated;

        public RoomModified(RoomDeleted roomDeleted, RoomCreated roomCreated)
        {
            this.roomDeleted = roomDeleted;
            this.roomCreated = roomCreated;

            Set<PersonInformation> recipients = new HashSet<PersonInformation>(roomDeleted.getRecipients());
            recipients.retainAll(roomCreated.getRecipients());
            for (PersonInformation recipient : recipients) {
                addRecipient(this.roomCreated.participantByPerson.get(recipient));
            }
            roomDeleted.removeRecipients(recipients);
            roomCreated.removeRecipients(recipients);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_MODIFIED;
        }

        @Override
        protected Long getTargetId()
        {
            return roomCreated.getTargetId();
        }
    }

    public static class Available extends SimpleRoomNotification
    {
        public Available(RoomEndpoint roomEndpoint)
        {
            super(roomEndpoint);
        }

        @Override
        protected NotificationRecord.NotificationType getNotificationType()
        {
            return NotificationRecord.NotificationType.ROOM_AVAILABLE;
        }
    }
}

