package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomGroupNotification extends ConfigurableNotification
{
    private RoomEndpoint roomEndpoint;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<RoomNotification> notifications = new LinkedList<RoomNotification>();

    public RoomGroupNotification(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;
    }

    public List<RoomNotification> getNotifications()
    {
        return Collections.unmodifiableList(notifications);
    }

    public void addNotification(RoomNotification roomNotification)
    {
        notifications.add(roomNotification);
    }

    public void removeNotification(RoomNotification roomNotification)
    {
        notifications.remove(roomNotification);
    }

    @Override
    public Set<PersonInformation> getRecipients()
    {
        Set<PersonInformation> recipients = new HashSet<PersonInformation>();
        for (RoomNotification notification : notifications) {
            recipients.addAll(notification.getParticipants());
        }
        return recipients;
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage getRenderedMessage(PersonInformation recipient,
            Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        // Get notifications for recipient
        List<RoomNotification> roomNotifications = new LinkedList<RoomNotification>();
        int roomCreatedCount = 0;
        int roomDeletedCount = 0;
        Set<ParticipantRole> participantRoles = new HashSet<ParticipantRole>();
        for (RoomNotification roomNotification : this.notifications) {
            PersonParticipant personParticipant = roomNotification.getParticipant(recipient);
            if (personParticipant == null) {
                continue;
            }
            ParticipantRole participantRole = personParticipant.getRole();
            participantRoles.add(participantRole);
            roomNotifications.add(roomNotification);
            if (roomNotification instanceof RoomNotification.RoomCreated) {
                roomCreatedCount++;
            }
            else if (roomNotification instanceof RoomNotification.RoomDeleted) {
                roomDeletedCount++;
            }
        }
        if (roomNotifications.size() == 0) {
            throw new RuntimeException("No notifications for " + recipient);
        }

        // Sort notifications by time slot
        Collections.sort(roomNotifications, new Comparator<RoomNotification>()
        {
            @Override
            public int compare(RoomNotification o1, RoomNotification o2)
            {
                return o1.getInterval().getStart().compareTo(o2.getInterval().getStart());
            }
        });

        // Setup required parameters
        int totalCount = roomNotifications.size();
        String roomName = getRoomName();
        ParticipantRole sameParticipantRole = (participantRoles.size() == 1 ? participantRoles.iterator().next() : null);

        // Build message (to be used in title and content)
        String messageCode;
        String messageRoomCode;
        String messageTypeCode;
        if (roomCreatedCount == totalCount) {
            String sameParticipantRolePrefix = getPrefixByRole(sameParticipantRole);
            messageCode = sameParticipantRolePrefix + ".title";
            messageRoomCode = sameParticipantRolePrefix + ".room";
            messageTypeCode = sameParticipantRolePrefix + ".created";
        }
        else {
            messageCode = "room.participation.access.title";
            messageRoomCode = "room.participation.access.room";
            if (roomDeletedCount == totalCount) {
                messageTypeCode = "room.participation.access.deleted";
            }
            else {
                messageTypeCode = "room.participation.access.modified";
            }
        }
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(renderContext.message(messageCode, renderContext.message(messageTypeCode)));
        messageBuilder.append(renderContext.message(messageRoomCode));
        if (roomName != null) {
            messageBuilder.append(" ");
            messageBuilder.append(roomName);
        }

        // Build title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(messageBuilder.toString());
        titleBuilder.append(renderContext.message("room.participation.titleSlot",
                renderContext.formatInterval(roomNotifications.iterator().next().getInterval())));
        if (totalCount > 1) {
            titleBuilder.append(renderContext.message("room.participation.titleMore", totalCount - 1));
        }

        // Build content
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(messageBuilder.toString());
        contentBuilder.append(":");
        contentBuilder.append("\n");

        // Build message
        for (RoomNotification roomNotification : this.notifications) {
            PersonParticipant personParticipant = roomNotification.getParticipant(recipient);
            if (personParticipant == null) {
                throw new RuntimeException(PersonParticipant.class.getSimpleName() + " doesn't exist for " + recipient);
            }
            ParticipantRole participantRole = personParticipant.getRole();
            if (roomNotification instanceof RoomNotification.RoomCreated) {
                String participantRolePrefix = getPrefixByRole(participantRole);
                messageCode = participantRolePrefix + ".slot";
                messageTypeCode = participantRolePrefix + ".created";
            }
            else {
                messageCode = "room.participation.access.slot";
                if (roomNotification instanceof RoomNotification.RoomDeleted) {
                    messageTypeCode = "room.participation.access.deleted";
                }
                else {
                    messageTypeCode = "room.participation.access.modified";
                }
            }

            contentBuilder.append("\n* ");
            contentBuilder.append(renderContext.message(messageCode,
                    renderContext.formatInterval(roomNotification.getInterval()),
                    renderContext.message(messageTypeCode)));
            if (roomNotification instanceof RoomNotification.RoomModified) {
                RoomNotification.RoomModified roomModified = (RoomNotification.RoomModified) roomNotification;
                ParticipantRole oldParticipantRole = roomModified.getOldParticipant(recipient).getRole();
                ParticipantRole newParticipantRole = roomModified.getParticipant(recipient).getRole();

                contentBuilder.append(":");

                String oldParticipantRolePrefix = getPrefixByRole(oldParticipantRole);
                contentBuilder.append("\n  -");
                contentBuilder.append(renderContext.message(oldParticipantRolePrefix + ".title",
                        renderContext.message(oldParticipantRolePrefix + ".deleted")));

                String newParticipantRolePrefix = getPrefixByRole(newParticipantRole);
                contentBuilder.append("\n  -");
                contentBuilder.append(renderContext.message(newParticipantRolePrefix + ".title",
                        renderContext.message(newParticipantRolePrefix + ".created")));
            }
            contentBuilder.append("\n");
        }

        return new NotificationMessage(renderContext.getLanguage(), titleBuilder.toString(), contentBuilder.toString());
    }

    private String getPrefixByRole(ParticipantRole participantRole)
    {
        if (ParticipantRole.ADMINISTRATOR.equals(participantRole)) {
            return  "room.participation.ADMINISTRATOR";
        }
        else if (ParticipantRole.PRESENTER.equals(participantRole)) {
            return "room.participation.PRESENTER";
        }
        else {
            return "room.participation.access";
        }

    }

    private String getRoomName()
    {
        String roomName;
        for (Alias alias : roomEndpoint.getAliases()) {
            if (alias.getType().equals(AliasType.ROOM_NAME)) {
                return alias.getValue();
            }
        }
        return null;
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration,
            NotificationManager manager)
    {
        throw new TodoImplementException();
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        notificationManager.roomGroupNotificationByRoomEndpointId.put(roomEndpoint.getId(), this);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
        super.onAfterRemoved(notificationManager);

        notificationManager.roomGroupNotificationByRoomEndpointId.remove(roomEndpoint.getId());
    }
}
