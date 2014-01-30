package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;

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
        titleBuilder.append(getClass().getSimpleName());

        return renderTemplateMessage(renderContext, titleBuilder.toString(), "room-participation.ftl");
    }

    /**
     * @return {@link RoomEndpoint#id}
     */
    public Long getRoomEndpointId()
    {
        return null;
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    /**
     * {@link RoomNotification} for a single {@link RoomEndpoint}.
     */
    protected static abstract class RoomSimpleNotification extends RoomNotification
    {
        /**
         * @see RoomEndpoint
         */
        protected RoomEndpoint roomEndpoint;

        /**
         * {@link UsedRoomEndpoint}s for the {@link #roomEndpoint}.
         */
        private Map<UsedRoomEndpoint, ReservationRequest> usedRoomEndpoints;

        /**
         * {@link AbstractParticipant} which should be notified by the {@link RoomSimpleNotification}.
         * If {@code null} it means that all {@link RoomEndpoint#getParticipants()} should be notified.
         */
        private AbstractParticipant participant;

        /**
         * For which is this {@link #roomEndpoint} allocated.
         */
        protected ReservationRequest reservationRequest;

        /**
         * Constructor.
         *
         * @param roomEndpoint sets the {@link #roomEndpoint}
         */
        private RoomSimpleNotification(RoomEndpoint roomEndpoint)
        {
            this.roomEndpoint = roomEndpoint;
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint sets the {@link #roomEndpoint}
         * @param participant  adds recipient
         */
        private RoomSimpleNotification(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            this.roomEndpoint = roomEndpoint;
            this.participant = participant;
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint  sets the {@link #roomEndpoint}
         * @param entityManager sets the {@link #reservationRequest}
         */
        private RoomSimpleNotification(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            this(roomEndpoint);

            ExecutableManager executableManager = new ExecutableManager(entityManager);
            if (isPermanentRoom()) {
                this.usedRoomEndpoints = getUsedRoomEndpoints(this.roomEndpoint, executableManager);
            }
            else {
                this.reservationRequest = getReservationRequestForRoomEndpoint(this.roomEndpoint, executableManager);
            }
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint       sets the {@link #roomEndpoint}
         * @param reservationRequest sets the {@link #reservationRequest}
         * @param participant        adds recipient
         */
        private RoomSimpleNotification(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
                AbstractParticipant participant)
        {
            this(roomEndpoint, participant);

            this.reservationRequest = reservationRequest;
        }

        /**
         * @return true whether {@link #roomEndpoint} is permanent room,
         * false otherwise
         */
        public boolean isPermanentRoom()
        {
            return roomEndpoint.getRoomConfiguration().getLicenseCount() == 0;
        }


        @Override
        protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            if (!super.onBeforeAdded(notificationManager, entityManager)) {
                return false;
            }

            // Skip adding the notification for permanent room and add notifications for future usages instead
            if (isPermanentRoom()) {
                if (usedRoomEndpoints == null) {
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    this.usedRoomEndpoints = getUsedRoomEndpoints(this.roomEndpoint, executableManager);
                }
                logger.debug("Skipping {} for permanent room and adding notifications for usages {}.", this,
                        usedRoomEndpoints.keySet());
                for (UsedRoomEndpoint usedRoomEndpoint : usedRoomEndpoints.keySet()) {
                    ReservationRequest reservationRequest = usedRoomEndpoints.get(usedRoomEndpoint);
                    RoomSimpleNotification notification =
                            createNotification(usedRoomEndpoint, reservationRequest, participant);
                    notificationManager.addNotification(notification, entityManager);
                }
                return false;
            }

            // Skip adding the notification if the same notification already exists and add all participants to it
            Class<? extends RoomSimpleNotification> notificationType = getClass();
            RoomSimpleNotification notification = notificationManager
                    .getNotification(getRoomEndpointId(), notificationType);
            if (notification != null) {
                logger.debug("Skipping {} because {} already exists.", this, notification);
                if (participant != null) {
                    logger.warn("Add participant to existing reservation {}.", participant);
                }
                /*if (participants != null && notification.participants != null) {
                    // Existing notification should notify also all participants from this skipped notification
                    for (AbstractParticipant participant : participants) {
                        notification.participants.add(participant);
                    }
                }
                else {
                    // Existing notifications should notify all participants
                    notification.participants = null;
                }*/
                return false;
            }

            // Setup reservation request which can be used to determine whether ModifyRoom should be constructed
            if (reservationRequest == null) {
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                this.reservationRequest = getReservationRequestForRoomEndpoint(roomEndpoint, executableManager);
            }

            // Add single participant
            if (participant != null) {
                if (participant instanceof PersonParticipant) {
                    PersonParticipant personParticipant = (PersonParticipant) participant;
                    addRecipient(personParticipant);
                }
            }
            // Add all room participants
            else {
                for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                    if (participant instanceof PersonParticipant) {
                        PersonParticipant personParticipant = (PersonParticipant) participant;
                        addRecipient(personParticipant);
                    }
                }
            }

            return true;
        }

        @Override
        protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            // Add notification to manager for grouping the same notifications
            Long targetId = getRoomEndpointId();
            Map<Class<? extends AbstractNotification>, AbstractNotification> notifications =
                    notificationManager.notificationsByTargetId.get(targetId);
            if (notifications == null) {
                notifications = new HashMap<Class<? extends AbstractNotification>, AbstractNotification>();
                notificationManager.notificationsByTargetId.put(targetId, notifications);
            }
            if (notifications.put(getClass(), this) != null) {
                throw new RuntimeException("Notification " + getClass().getSimpleName() +
                        " already exists for target " + targetId + ".");
            }

            super.onAfterAdded(notificationManager, entityManager);
        }

        @Override
        protected void onAfterRemoved(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onAfterRemoved(notificationManager, entityManager);

            Long targetId = getRoomEndpointId();
            Map<Class<? extends AbstractNotification>, AbstractNotification> notifications =
                    notificationManager.notificationsByTargetId.get(targetId);
            if (notifications != null) {
                AbstractNotification notification = notifications.get(getClass());
                if (!this.equals(notification)) {
                    throw new RuntimeException("Notification " + getClass().getSimpleName() +
                            " doesn't exist for target " + targetId + ".");
                }
                notifications.remove(getClass());
            }
        }

        protected RoomSimpleNotification createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            throw new TodoImplementException();
        }

        @Override
        public Long getRoomEndpointId()
        {
            return roomEndpoint.getId();
        }

        @Override
        public String toString()
        {
            return String.format(getClass().getSimpleName() + " (targetId: %d, participant: %s)",
                    getRoomEndpointId(), participant);
        }

        /**
         * @param roomEndpoint
         * @param executableManager
         * @return {@link ReservationRequest} for given {@code roomEndpoint} or null if not exists
         */
        private static ReservationRequest getReservationRequestForRoomEndpoint(RoomEndpoint roomEndpoint,
                ExecutableManager executableManager)
        {
            Reservation reservation = executableManager.getReservation(roomEndpoint);
            if (reservation != null) {
                Allocation allocation = reservation.getAllocation();
                if (allocation != null) {
                    AbstractReservationRequest reservationRequest = allocation.getReservationRequest();
                    if (reservationRequest instanceof ReservationRequest) {
                        return (ReservationRequest) reservationRequest;
                    }
                }
            }
            return null;
        }

        /**
         * @param roomEndpoint
         * @param executableManager
         * @return map of {@link ReservationRequest} by {@link UsedRoomEndpoint} for given {@code roomEndpoint}
         */
        private static Map<UsedRoomEndpoint, ReservationRequest> getUsedRoomEndpoints(RoomEndpoint roomEndpoint,
                ExecutableManager executableManager)
        {
            Map<UsedRoomEndpoint, ReservationRequest> usedRoomEndpoints =
                    new LinkedHashMap<UsedRoomEndpoint, ReservationRequest>();
            for (UsedRoomEndpoint usedRoomEndpoint : executableManager.getFutureUsedRoomEndpoint(roomEndpoint)) {
                ReservationRequest reservationRequest =
                        getReservationRequestForRoomEndpoint(usedRoomEndpoint, executableManager);
                usedRoomEndpoints.put(usedRoomEndpoint, reservationRequest);
            }
            return usedRoomEndpoints;
        }
    }

    public static class RoomCreated extends RoomSimpleNotification
    {
        public RoomCreated(RoomEndpoint roomEndpoint)
        {
            super(roomEndpoint);
        }

        public RoomCreated(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            super(roomEndpoint, participant);
        }

        private RoomCreated(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
                AbstractParticipant participant)
        {
            super(roomEndpoint, reservationRequest, participant);
        }

        @Override
        protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            if (!super.onBeforeAdded(notificationManager, entityManager)) {
                return false;
            }
            if (reservationRequest != null) {
                Long reservationRequestId = reservationRequest.getId();
                List<RoomNotification> roomNotifications =
                        notificationManager.getRoomNotificationsByReservationRequestId(reservationRequestId);
                RoomDeleted roomDeleted = null;
                for (RoomNotification roomNotification : roomNotifications) {
                    if (roomNotification instanceof RoomDeleted &&
                            !((RoomDeleted) roomNotification).getRoomEndpointId().equals(getRoomEndpointId())) {
                        roomDeleted = (RoomDeleted) roomNotification;
                        break;
                    }
                }
                if (roomDeleted != null) {
                    RoomModified modifiedNotification = new RoomModified(roomDeleted, this);
                    notificationManager.addNotification(modifiedNotification, entityManager);
                    if (!roomDeleted.hasRecipients()) {
                        notificationManager.removeNotification(roomDeleted, entityManager);
                    }
                    return hasRecipients();
                }
                else {
                    roomNotifications.add(this);
                }
            }
            return true;
        }

        @Override
        protected RoomSimpleNotification createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            return new RoomCreated(roomEndpoint, reservationRequest, participant);
        }

        @Override
        protected void onAfterRemoved(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onAfterRemoved(notificationManager, entityManager);

            if (reservationRequest != null) {
                notificationManager.getRoomNotificationsByReservationRequestId(reservationRequest.getId()).remove(this);
            }
        }
    }

    public static class RoomDeleted extends RoomSimpleNotification
    {
        public RoomDeleted(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            super(roomEndpoint, entityManager);
        }

        public RoomDeleted(RoomEndpoint roomEndpoint, AbstractParticipant participant)
        {
            super(roomEndpoint, participant);
        }

        private RoomDeleted(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
                AbstractParticipant participant)
        {
            super(roomEndpoint, reservationRequest, participant);
        }

        @Override
        protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            if (!super.onBeforeAdded(notificationManager, entityManager)) {
                return false;
            }

            if (reservationRequest != null) {
                Long reservationRequestId = reservationRequest.getId();
                List<RoomNotification> roomNotifications =
                        notificationManager.getRoomNotificationsByReservationRequestId(reservationRequestId);
                RoomCreated roomCreated = null;
                for (RoomNotification roomNotification : roomNotifications) {
                    if (roomNotification instanceof RoomCreated &&
                            !((RoomCreated) roomNotification).getRoomEndpointId().equals(getRoomEndpointId())) {
                        roomCreated = (RoomCreated) roomNotification;
                        break;
                    }
                }
                if (roomCreated != null) {
                    RoomNotification modifiedNotification = new RoomModified(this, roomCreated);
                    notificationManager.addNotification(modifiedNotification, entityManager);
                    if (!roomCreated.hasRecipients()) {
                        notificationManager.removeNotification(roomCreated, entityManager);
                    }
                    return hasRecipients();
                }
                else {
                    roomNotifications.add(this);
                }
            }
            return true;
        }

        @Override
        protected RoomSimpleNotification createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            return new RoomDeleted(roomEndpoint, reservationRequest, participant);
        }

        @Override
        protected void onAfterRemoved(NotificationManager notificationManager, EntityManager entityManager)
        {
            super.onAfterRemoved(notificationManager, entityManager);

            if (reservationRequest != null) {
                notificationManager.getRoomNotificationsByReservationRequestId(reservationRequest.getId()).remove(this);
            }
        }
    }

    public static class RoomModified extends RoomNotification
    {
        private RoomDeleted roomDeleted;

        private RoomCreated roomCreated;

        private RoomModified(RoomDeleted roomDeleted, RoomCreated roomCreated)
        {
            this.roomDeleted = roomDeleted;
            this.roomCreated = roomCreated;

            Set<PersonInformation> recipients = new LinkedHashSet<PersonInformation>(roomCreated.getRecipients());
            recipients.retainAll(roomDeleted.getRecipients());
            for (PersonInformation recipient : recipients) {
                addRecipient(this.roomCreated.participantByPerson.get(recipient));
            }
            roomDeleted.removeRecipients(recipients);
            roomCreated.removeRecipients(recipients);
        }

        @Override
        public Long getRoomEndpointId()
        {
            return roomCreated.getRoomEndpointId();
        }
    }

    public static class RoomAvailable extends RoomSimpleNotification
    {
        public RoomAvailable(RoomEndpoint roomEndpoint)
        {
            super(roomEndpoint);
        }
    }

}

