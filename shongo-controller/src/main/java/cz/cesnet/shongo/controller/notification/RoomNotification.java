package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.DateTime;
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
            new LinkedHashMap<PersonInformation, PersonParticipant>();

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
    protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        if (!super.onBeforeAdded(notificationManager, entityManager)) {
            return false;
        }

        // Skip adding the notification if the same notification already exists and add all participants to it
        Class<? extends RoomNotification> notificationType = getClass();
        RoomNotification notification = notificationManager.getRoomNotification(getRoomEndpointId(), notificationType);
        if (notification != null) {
            logger.debug("Skipping {} because {} already exists.", this, notification);
            notification.mergeParticipants(this);
            return false;
        }

        return true;
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        // Add notification to manager for grouping the same notifications
        Long roomEndpointId = getRoomEndpointId();
        Map<Class<? extends RoomNotification>, RoomNotification> notifications =
                notificationManager.roomNotificationsByRoomEndpointId.get(roomEndpointId);
        if (notifications == null) {
            notifications = new HashMap<Class<? extends RoomNotification>, RoomNotification>();
            notificationManager.roomNotificationsByRoomEndpointId.put(roomEndpointId, notifications);
        }
        if (notifications.put(getClass(), this) != null) {
            throw new RuntimeException(getClass().getSimpleName() +
                    " already exists for target " + roomEndpointId + ".");
        }

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

        Long roomEndpointId = getRoomEndpointId();
        Map<Class<? extends RoomNotification>, RoomNotification> notifications =
                notificationManager.roomNotificationsByRoomEndpointId.get(roomEndpointId);
        if (notifications != null) {
            AbstractNotification notification = notifications.get(getClass());
            if (!this.equals(notification)) {
                throw new RuntimeException(getClass().getSimpleName() +
                        " doesn't exist for target " + roomEndpointId + ".");
            }
            notifications.remove(getClass());
        }

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
    public RoomEndpoint getRoomEndpoint()
    {
        return null;
    }

    /**
     * @return {@link RoomEndpoint#id}
     */
    public final Long getRoomEndpointId()
    {
        RoomEndpoint roomEndpoint = getRoomEndpoint();
        if (roomEndpoint != null) {
            return roomEndpoint.getId();
        }
        else {
            return null;
        }
    }

    /**
     * @return {@link RoomEndpoint#getParticipantNotificationState()}
     */
    public final NotificationState getNotificationState()
    {
        RoomEndpoint roomEndpoint = getRoomEndpoint();
        if (roomEndpoint != null) {
            return roomEndpoint.getParticipantNotificationState();
        }
        else {
            return null;
        }
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

    /**
     * @return {@link Interval} of the room
     */
    public abstract Interval getInterval();

    /**
     * @return {@link Interval#getStart()} for {@link #getInterval()}
     */
    public DateTime getStart()
    {
        return getInterval().getStart();
    }

    /**
     * @param notification to be merged into this {@link RoomNotification}
     */
    protected void mergeParticipants(RoomNotification notification)
    {
        for (PersonParticipant participant : notification.participants.values()) {
            addParticipant(participant);
        }
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    /**
     * @param roomEndpoint
     * @return top {@link RoomEndpoint} from given {@code roomEndpoint}
     */
    public static RoomEndpoint getTopRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        while (roomEndpoint instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) roomEndpoint;
            roomEndpoint = usedRoomEndpoint.getReusedRoomEndpoint();
        }
        return roomEndpoint;
    }

    /**
     * @param roomEndpoint
     * @return room name for given {@code roomEndpoint}
     */
    public static String getRoomName(RoomEndpoint roomEndpoint)
    {
        for (Alias alias : roomEndpoint.getAliases()) {
            if (alias.getType().equals(AliasType.ROOM_NAME)) {
                return alias.getValue();
            }
        }
        return null;
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
            this.roomEndpoint.loadLazyProperties();
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
            this.roomEndpoint.loadLazyProperties();
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

            if (!super.onBeforeAdded(notificationManager, entityManager)) {
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

        protected RoomSimple createNotification(RoomEndpoint roomEndpoint,
                ReservationRequest reservationRequest, AbstractParticipant participant)
        {
            throw new TodoImplementException();
        }

        @Override
        public RoomEndpoint getRoomEndpoint()
        {
            return roomEndpoint;
        }

        @Override
        public Interval getInterval()
        {
            return roomEndpoint.getOriginalSlot();
        }

        @Override
        public RoomEndpoint getGroupRoomEndpoint()
        {
            return getTopRoomEndpoint(this.roomEndpoint);
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
                if (!usedRoomEndpoint.isParticipantNotificationEnabled()) {
                    continue;
                }
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
                RoomDeleted roomDeleted = notificationManager.getRoomNotification(
                        migratedFromExecutable.getId(), RoomDeleted.class);
                if (roomDeleted != null) {
                    RoomModified roomModified = RoomModified.create(roomDeleted, this);
                    if (roomModified != null) {
                        notificationManager.addNotification(roomModified, entityManager);
                    }
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
            Executable migrateToExecutable = roomEndpoint.getMigrateToExecutable();
            if (migrateToExecutable == null) {
                switch (roomEndpoint.getState()) {
                    case STOPPED:
                    case STOPPING_FAILED:
                    case FINALIZED:
                    case FINALIZATION_FAILED:
                        // Do not notify about deletion of finished rooms
                        return false;
                }
            }

            if (!super.onBeforeAdded(notificationManager, entityManager)) {
                return false;
            }

            if (migrateToExecutable != null) {
                RoomCreated roomCreated = notificationManager.getRoomNotification(
                        migrateToExecutable.getId(), RoomCreated.class);
                if (roomCreated != null) {
                    RoomModified roomModified = RoomModified.create(this, roomCreated);
                    if (roomModified != null) {
                        notificationManager.addNotification(roomModified, entityManager);
                    }
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
        private final RoomEndpoint oldRoomEndpoint;

        private final RoomEndpoint newRoomEndpoint;

        private boolean roomModified = false;

        protected Map<PersonInformation, PersonParticipant> oldParticipants =
                new HashMap<PersonInformation, PersonParticipant>();

        /**
         * Constructor.
         *
         * @param oldRoomEndpoint sets the {@link #oldRoomEndpoint}
         * @param newRoomEndpoint sets the {@link #newRoomEndpoint}
         */
        private RoomModified(RoomEndpoint oldRoomEndpoint, RoomEndpoint newRoomEndpoint)
        {
            this.newRoomEndpoint = newRoomEndpoint;
            this.newRoomEndpoint.loadLazyProperties();

            if (oldRoomEndpoint != null) {
                this.oldRoomEndpoint = oldRoomEndpoint;
                this.oldRoomEndpoint.loadLazyProperties();

                if (!ObjectHelper.isSame(oldRoomEndpoint.getMeetingName(), newRoomEndpoint.getMeetingName())) {
                    roomModified = true;
                }
                if (!ObjectHelper.isSame(oldRoomEndpoint.getMeetingDescription(),
                        newRoomEndpoint.getMeetingDescription())) {
                    roomModified = true;
                }
                if (!oldRoomEndpoint.getSlot().equals(newRoomEndpoint.getSlot())) {
                    roomModified = true;
                }
                if (!ObjectHelper.isSameIgnoreOrder(oldRoomEndpoint.getAliases(), newRoomEndpoint.getAliases())) {
                    roomModified = true;
                }
                if (!ObjectHelper.isSame(oldRoomEndpoint.getRoomDescription(), newRoomEndpoint.getRoomDescription())) {
                    roomModified = true;
                }
                if (!ObjectHelper.isSame(oldRoomEndpoint.getPin(), newRoomEndpoint.getPin())) {
                    roomModified = true;
                }
            }
            else {
                this.oldRoomEndpoint = null;
                this.roomModified = false;
            }
        }

        /**
         * @param roomDeleted
         * @param roomCreated
         * @return new instance of {@link RoomModified} for given arguments or {@code null} if it is not needed
         */
        public static RoomModified create(RoomDeleted roomDeleted, RoomCreated roomCreated)
        {
            RoomModified roomModified = new RoomModified(roomDeleted.roomEndpoint, roomCreated.roomEndpoint);
            boolean isRoomModified = roomModified.isRoomModified();

            // Get set of common participants
            Set<PersonInformation> participants = new LinkedHashSet<PersonInformation>(roomCreated.getParticipants());
            participants.retainAll(roomDeleted.getParticipants());

            // Move participants to this notification
            for (PersonInformation participant : participants) {
                PersonParticipant oldParticipant = roomDeleted.getParticipant(participant);
                PersonParticipant newParticipant = roomCreated.getParticipant(participant);
                if (roomModified.addParticipant(oldParticipant, newParticipant)) {
                    isRoomModified = true;
                }
            }
            roomDeleted.removeParticipants(participants);
            roomCreated.removeParticipants(participants);

            if (isRoomModified) {
                return roomModified;
            }
            else {
                return null;
            }
        }

        /**
         * @param roomEndpoint
         * @param oldParticipant
         * @param newParticipant
         * @return new instance of {@link RoomModified} for given arguments or {@code null} if it is not needed
         */
        public static RoomModified create(RoomEndpoint roomEndpoint, AbstractParticipant oldParticipant,
                AbstractParticipant newParticipant)
        {
            RoomModified roomModified = new RoomModified(null, roomEndpoint);
            if (!(oldParticipant instanceof PersonParticipant)) {
                throw new IllegalArgumentException("Old participant must person.");
            }
            if (!(newParticipant instanceof PersonParticipant)) {
                throw new IllegalArgumentException("New participant must person.");
            }
            PersonParticipant oldPersonParticipant = (PersonParticipant) oldParticipant;
            PersonParticipant newPersonParticipant = (PersonParticipant) newParticipant;
            if (roomModified.addParticipant(oldPersonParticipant, newPersonParticipant)) {
                return roomModified;
            }
            else {
                return null;
            }
        }

        /**
         * @return {@link #oldRoomEndpoint}
         */
        public RoomEndpoint getOldRoomEndpoint()
        {
            return oldRoomEndpoint;
        }

        /**
         * @return {@link #newRoomEndpoint}
         */
        public RoomEndpoint getNewRoomEndpoint()
        {
            return newRoomEndpoint;
        }

        /**
         * @return {@link #roomModified}
         */
        public boolean isRoomModified()
        {
            return roomModified;
        }

        /**
         * @param participant
         * @return new {@link PersonParticipant} for given {@code participant}
         */
        public PersonParticipant getNewParticipant(PersonInformation participant)
        {
            return getParticipant(participant);
        }

        /**
         * @param participant
         * @return old {@link PersonParticipant} for given {@code participant}
         */
        public PersonParticipant getOldParticipant(PersonInformation participant)
        {
            return oldParticipants.get(participant);
        }

        /**
         * @param participant
         * @return true whether {@link PersonParticipant#role} has changed for given {@code recipient}
         */
        public boolean isParticipantRoleModified(PersonInformation participant)
        {
            return !getParticipant(participant).getRole().equals(getOldParticipant(participant).getRole());
        }

        @Override
        public RoomEndpoint getRoomEndpoint()
        {
            return newRoomEndpoint;
        }

        @Override
        public RoomEndpoint getGroupRoomEndpoint()
        {
            return getTopRoomEndpoint(this.newRoomEndpoint);
        }

        @Override
        public Interval getInterval()
        {
            return newRoomEndpoint.getOriginalSlot();
        }

        @Override
        protected void mergeParticipants(RoomNotification notification)
        {
            super.mergeParticipants(notification);

            this.oldParticipants.putAll(notification.participants);
        }

        /**
         * @param oldParticipant
         * @param newParticipant
         * @return true whether participant was added,
         * false otherwise
         */
        private boolean addParticipant(PersonParticipant oldParticipant, PersonParticipant newParticipant)
        {
            if (!ObjectHelper.isSame(oldParticipant, newParticipant) || isRoomModified()) {
                oldParticipants.put(oldParticipant.getPersonInformation(), oldParticipant);
                addParticipant(newParticipant);
                return true;
            }
            else {
                return false;
            }
        }
    }

}

