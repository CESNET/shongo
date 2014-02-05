package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import org.joda.time.Interval;

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
     * Map of {@link AbstractParticipant}s by participant {@link PersonInformation}s.
     */
    private Map<PersonInformation, PersonParticipant> participants =
            new HashMap<PersonInformation, PersonParticipant>();

    /**
     * @param personParticipant to be added as participant
     */
    protected void addParticipant(PersonParticipant personParticipant)
    {
        this.participants.put(personParticipant.getPersonInformation(), personParticipant);
    }

    /**
     * @param participant
     */
    protected void removeParticipant(PersonInformation participant)
    {
        participants.remove(participant);
    }

    /**
     * @param participants to be removed by the {@link #removeParticipant}
     */
    public final void removeParticipants(Set<PersonInformation> participants)
    {
        for (PersonInformation participant : participants) {
            removeParticipant(participant);
        }
    }

    /**
     * @return {@link #recipients}
     */
    public Set<PersonInformation> getParticipants()
    {
        return participants.keySet();
    }

    /**
     * @return true whether {@link #recipients} is not empty, false otherwise
     */
    public final boolean hasParticipants()
    {
        return !participants.isEmpty();
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        throw new RuntimeException(RoomNotification.class.getSimpleName() + " should be rendered from " +
                RoomGroupNotification.class.getSimpleName());
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        RoomEndpoint groupRoomEndpoint = getGroupRoomEndpoint();
        if (groupRoomEndpoint != null) {
            Long groupRoomEndpointId = groupRoomEndpoint.getId();
            RoomGroupNotification roomGroupNotification =
                    notificationManager.roomGroupNotificationByRoomEndpointId.get(groupRoomEndpointId);
            if (roomGroupNotification == null) {
                roomGroupNotification = new RoomGroupNotification(groupRoomEndpoint);
                notificationManager.addNotification(roomGroupNotification, entityManager);
            }
            roomGroupNotification.addNotification(this);
        }

        super.onAfterAdded(notificationManager, entityManager);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
        super.onAfterRemoved(notificationManager);

        RoomEndpoint groupRoomEndpoint = getGroupRoomEndpoint();
        if (groupRoomEndpoint != null) {
            Long groupRoomEndpointId = groupRoomEndpoint.getId();
            RoomGroupNotification roomGroupNotification =
                    notificationManager.roomGroupNotificationByRoomEndpointId.get(groupRoomEndpointId);
            if (roomGroupNotification != null) {
                roomGroupNotification.removeNotification(this);
            }
        }
    }

    /**
     * @return {@link RoomEndpoint#id}
     */
    public Long getRoomEndpointId()
    {
        return null;
    }

    /**
     * @return {@link RoomEndpoint#id}
     */
    public RoomEndpoint getGroupRoomEndpoint()
    {
        return null;
    }

    /**
     * @param participant
     * @return {@link PersonParticipant} for given {@code participant}
     */
    public PersonParticipant getParticipant(PersonInformation participant)
    {
        return participants.get(participant);
    }

    public abstract Interval getInterval();

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    /**
     * {@link RoomNotification} for a single {@link RoomEndpoint}.
     */
    protected static abstract class RoomSimple extends RoomNotification
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
         * {@link AbstractParticipant} which should be notified by the {@link RoomSimple}.
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
        private RoomSimple(RoomEndpoint roomEndpoint)
        {
            this.roomEndpoint = roomEndpoint;
        }

        /**
         * Constructor.
         *
         * @param roomEndpoint sets the {@link #roomEndpoint}
         * @param participant  adds participant
         */
        private RoomSimple(RoomEndpoint roomEndpoint, AbstractParticipant participant)
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
        private RoomSimple(RoomEndpoint roomEndpoint, EntityManager entityManager)
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
         * @param participant        adds participant
         */
        private RoomSimple(RoomEndpoint roomEndpoint, ReservationRequest reservationRequest,
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
                    RoomSimple notification =
                            createNotification(usedRoomEndpoint, reservationRequest, participant);
                    notificationManager.addNotification(notification, entityManager);
                }
                return false;
            }

            // Skip adding the notification if the same notification already exists and add all participants to it
            Class<? extends RoomSimple> notificationType = getClass();
            RoomSimple notification = notificationManager
                    .getRoomSimpleNotification(getRoomEndpointId(), notificationType);
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
                    addParticipant(personParticipant);
                }
            }
            // Add all room participants
            else {
                for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                    if (participant instanceof PersonParticipant) {
                        PersonParticipant personParticipant = (PersonParticipant) participant;
                        addParticipant(personParticipant);
                    }
                }
            }

            return true;
        }

        @Override
        protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
        {
            // Add notification to manager for grouping the same notifications
            Long roomEndpointId = getRoomEndpointId();
            Map<Class<? extends RoomSimple>, RoomSimple> notifications =
                    notificationManager.roomSimpleNotificationsByRoomEndpointId.get(roomEndpointId);
            if (notifications == null) {
                notifications = new HashMap<Class<? extends RoomSimple>, RoomSimple>();
                notificationManager.roomSimpleNotificationsByRoomEndpointId.put(roomEndpointId, notifications);
            }
            if (notifications.put(getClass(), this) != null) {
                throw new RuntimeException("Notification " + getClass().getSimpleName() +
                        " already exists for target " + roomEndpointId + ".");
            }

            super.onAfterAdded(notificationManager, entityManager);
        }

        @Override
        protected void onAfterRemoved(NotificationManager notificationManager)
        {
            super.onAfterRemoved(notificationManager);

            Long roomEndpointId = getRoomEndpointId();
            Map<Class<? extends RoomSimple>, RoomSimple> notifications =
                    notificationManager.roomSimpleNotificationsByRoomEndpointId.get(roomEndpointId);
            if (notifications != null) {
                AbstractNotification notification = notifications.get(getClass());
                if (!this.equals(notification)) {
                    throw new RuntimeException("Notification " + getClass().getSimpleName() +
                            " doesn't exist for target " + roomEndpointId + ".");
                }
                notifications.remove(getClass());
            }
        }

        protected RoomSimple createNotification(RoomEndpoint roomEndpoint,
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
        public Interval getInterval()
        {
            return roomEndpoint.getSlot();
        }

        @Override
        public RoomEndpoint getGroupRoomEndpoint()
        {
            RoomEndpoint roomEndpoint = this.roomEndpoint;
            while (roomEndpoint instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) roomEndpoint;
                roomEndpoint = usedRoomEndpoint.getReusedRoomEndpoint();
            }
            return roomEndpoint;
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

    public static class RoomCreated extends RoomSimple
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

            Executable migratedFromExecutable = roomEndpoint.getMigrateFromExecutable();
            if (migratedFromExecutable != null) {
                RoomDeleted roomDeleted = notificationManager.getRoomSimpleNotification(
                        migratedFromExecutable.getId(), RoomDeleted.class);
                if (roomDeleted != null) {
                    RoomModified modifiedNotification = new RoomModified(roomDeleted, this);
                    notificationManager.addNotification(modifiedNotification, entityManager);
                    if (!roomDeleted.hasParticipants()) {
                        notificationManager.removeNotification(roomDeleted, entityManager);
                    }
                    return hasParticipants();
                }
            }

            return true;
        }

        @Override
        protected RoomSimple createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            return new RoomCreated(roomEndpoint, reservationRequest, participant);
        }
    }

    public static class RoomDeleted extends RoomSimple
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

            Executable migrateToExecutable = roomEndpoint.getMigrateToExecutable();
            if (migrateToExecutable != null) {
                RoomCreated roomCreated = notificationManager.getRoomSimpleNotification(
                        migrateToExecutable.getId(), RoomCreated.class);
                if (roomCreated != null) {
                    RoomModified modifiedNotification = new RoomModified(this, roomCreated);
                    notificationManager.addNotification(modifiedNotification, entityManager);
                    if (!roomCreated.hasParticipants()) {
                        notificationManager.removeNotification(roomCreated, entityManager);
                    }
                    return hasParticipants();
                }
            }

            return true;
        }

        @Override
        protected RoomSimple createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            return new RoomDeleted(roomEndpoint, reservationRequest, participant);
        }
    }

    public static class RoomModified extends RoomNotification
    {
        private final RoomDeleted roomDeleted;

        private final RoomCreated roomCreated;

        protected Map<PersonInformation, PersonParticipant> oldParticipants =
                new HashMap<PersonInformation, PersonParticipant>();

        private RoomModified(RoomDeleted roomDeleted, RoomCreated roomCreated)
        {
            this.roomDeleted = roomDeleted;
            this.roomCreated = roomCreated;

            Set<PersonInformation> participants = new LinkedHashSet<PersonInformation>(roomCreated.getParticipants());
            participants.retainAll(roomDeleted.getParticipants());
            for (PersonInformation participant : participants) {
                oldParticipants.put(participant, this.roomDeleted.getParticipant(participant));
                addParticipant(this.roomCreated.getParticipant(participant));
            }
            roomDeleted.removeParticipants(participants);
            roomCreated.removeParticipants(participants);
        }

        public PersonParticipant getOldParticipant(PersonInformation participant)
        {
            return oldParticipants.get(participant);
        }

        @Override
        public Long getRoomEndpointId()
        {
            return roomCreated.getRoomEndpointId();
        }

        @Override
        public RoomEndpoint getGroupRoomEndpoint()
        {
            return roomCreated.getGroupRoomEndpoint();
        }

        @Override
        public Interval getInterval()
        {
            return roomCreated.getInterval();
        }
    }

}

