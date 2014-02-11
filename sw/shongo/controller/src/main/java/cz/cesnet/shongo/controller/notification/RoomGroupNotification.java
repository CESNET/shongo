package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.util.iCalendar;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomGroupNotification extends ConfigurableNotification
{
    private static final String NOTIFICATION_RECORD_PREFIX = "\n  -";

    private static final DateTimeFormatter ATTACHMENT_FILENAME_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd_HH:mm");

    private RoomEndpoint roomEndpoint;

    private String description;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<RoomNotification> notifications = new LinkedList<RoomNotification>();

    public RoomGroupNotification(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;
        this.roomEndpoint.loadLazyProperties();
        this.description = roomEndpoint.getRoomDescription();
    }

    public List<RoomNotification> getNotifications(PersonInformation participant)
    {
        List<RoomNotification> notifications = new LinkedList<RoomNotification>();
        for (RoomNotification roomNotification : this.notifications) {
            PersonParticipant personParticipant = roomNotification.getParticipant(participant);
            if (personParticipant == null) {
                continue;
            }
            notifications.add(roomNotification);
        }
        return notifications;
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
        Set<PersonInformation> recipients = new LinkedHashSet<PersonInformation>();
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
        RenderContext context = new ConfiguredRenderContext(configuration, "notification", manager);

        // Get notifications for recipient
        List<RoomNotification> roomNotifications = new LinkedList<RoomNotification>();
        int roomCreatedCount = 0;
        int roomDeletedCount = 0;
        boolean isRoomModified = false;
        boolean isParticipantRoleModified = false;
        String oldRoomName = null;
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
            else if (roomNotification instanceof RoomNotification.RoomModified) {
                RoomNotification.RoomModified roomModified = (RoomNotification.RoomModified) roomNotification;
                if (roomModified.isRoomModified()) {
                    isRoomModified = true;
                    oldRoomName = RoomNotification.getRoomName(roomModified.getOldRoomEndpoint());
                }
                if (roomModified.isParticipantRoleModified(recipient)) {
                    isParticipantRoleModified = true;
                }
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
        String roomName = RoomNotification.getRoomName(this.roomEndpoint);
        ParticipantRole sameParticipantRole =
                (participantRoles.size() == 1 ? participantRoles.iterator().next() : null);

        // Build message
        StringBuilder messageBuilder = new StringBuilder();
        if (roomCreatedCount == totalCount) {
            String sameParticipantRolePrefix = getPrefixByRole(sameParticipantRole);
            String messageCode = sameParticipantRolePrefix + ".title";
            String messageTypeCode = sameParticipantRolePrefix + ".created";
            String messageRoomName = "";
            if (roomName != null) {
                messageRoomName = context.message("room.participation.titleRoom", roomName);
            }
            messageBuilder.append(context.message(messageCode, context.message(messageTypeCode), messageRoomName));
        }
        else {
            if (roomDeletedCount == totalCount) {
                String messageRoomName = "";
                if (roomName != null) {
                    messageRoomName = context.message("room.participation.titleRoom", roomName);
                }
                messageBuilder.append(context.message("room.participation.access.title",
                        context.message("room.participation.access.deleted"), messageRoomName));
            }
            else {
                String messageCode;
                if (isRoomModified && isParticipantRoleModified) {
                    messageCode = "room.participation.modified.title.roomAndRole";
                }
                else if (isRoomModified) {
                    messageCode = "room.participation.modified.title.room";
                }
                else if (isParticipantRoleModified) {
                    messageCode = "room.participation.modified.title.role";
                }
                else {
                    throw new IllegalStateException();
                }
                String messageRoomName = "";
                String messageRoomNameModified = "";
                if (oldRoomName != null && roomName != null && !oldRoomName.equals(roomName)) {
                    messageRoomName = context.message("room.participation.titleRoom", oldRoomName);
                    messageRoomNameModified = context.message("room.participation.titleRoomModified", roomName);
                }
                else if (roomName != null) {
                    messageRoomName = context.message("room.participation.titleRoom", roomName);
                }
                messageBuilder.append(context.message(messageCode, messageRoomName, messageRoomNameModified));
            }
        }


        // Build title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(messageBuilder.toString());
        titleBuilder.append(context.message("room.participation.titleSlot",
                context.formatInterval(roomNotifications.iterator().next().getInterval())));
        if (totalCount > 1) {
            titleBuilder.append(context.message("room.participation.titleSlotMore", totalCount - 1));
        }

        // Build content
        StringBuilder contentBuilder = new StringBuilder();

        // Build message
        for (RoomNotification roomNotification : roomNotifications) {
            if (contentBuilder.length() > 0) {
                contentBuilder.append("\n");
            }
            contentBuilder.append("* ");
            contentBuilder.append(renderNotification(recipient, roomNotification, context));
            contentBuilder.append("\n");
        }

        // Add parameters
        context.addParameter("description", description);
        context.addParameter("notifications", contentBuilder.toString());
        // Add parameters for not-deleted room
        if (roomDeletedCount != totalCount) {
            context.addParameter("pin", roomEndpoint.getPin());
            context.addParameter("aliases", Alias.sortedList(roomEndpoint.getAliases()));
        }

        // Render notification message
        NotificationMessage message = renderTemplateMessage(context, titleBuilder.toString(), "room-group.ftl");

        // Add attachments
        DateTimeFormatter dateTimeFormatter = ATTACHMENT_FILENAME_FORMATTER.withZone(context.getTimeZone());
        for (RoomNotification roomNotification : roomNotifications) {
            String fileName = dateTimeFormatter.print(roomNotification.getStart()) + ".ics";
            iCalendar calendar = createCalendar(roomNotification, context);
            if (roomNotification instanceof RoomNotification.RoomCreated) {
                calendar.setMethod(iCalendar.Method.CREATE);
                fileName = "invite_" + fileName;
            }
            else if (roomNotification instanceof RoomNotification.RoomModified) {
                calendar.setMethod(iCalendar.Method.UPDATE);
                fileName = "invite_" + fileName;
            }
            else if (roomNotification instanceof RoomNotification.RoomDeleted) {
                calendar.setMethod(iCalendar.Method.CANCEL);
                fileName = "invite_" + fileName;
            }
            else {
                throw new TodoImplementException(roomNotification.getClass());
            }
            message.addAttachment(new iCalendarNotificationAttachment(
                    fileName, calendar, roomNotification.getNotificationState()));
        }

        return message;
    }

    private iCalendar createCalendar(RoomNotification roomNotification, RenderContext context)
    {
        RoomEndpoint roomEndpoint = roomNotification.getRoomEndpoint();
        Interval interval = roomNotification.getInterval();
        String meetingName = roomEndpoint.getMeetingName();
        if (meetingName == null) {
            meetingName = context.message("room.meeting");
        }
        String eventId = ObjectIdentifier.formatId(roomEndpoint);

        iCalendar iCalendar = new iCalendar();
        iCalendar.Event event = iCalendar.addEvent(Domain.getLocalDomainName(), eventId, meetingName);
        event.setDescription(roomEndpoint.getRoomDescription());
        event.setInterval(interval, context.getTimeZone());

        Set<PersonInformation> attendees = new LinkedHashSet<PersonInformation>();
        attendees.addAll(roomNotification.getParticipants());
        for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                PersonInformation personInformation = personParticipant.getPersonInformation();
                attendees.add(personInformation);
            }
        }
        for (PersonInformation attendee : attendees) {
            event.addAttendee(attendee.getFullName(), attendee.getPrimaryEmail());
        }
        return iCalendar;
    }

    private String renderNotification(PersonInformation recipient, RoomNotification notification, RenderContext context)
    {
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder recordsBuilder = new StringBuilder();

        String meetingName = notification.getRoomEndpoint().getMeetingName();
        if (meetingName != null) {
            meetingName = " \"" + meetingName + "\"";
        }
        else {
            meetingName = "";
        }

        // Append output and records
        if (notification instanceof RoomNotification.RoomCreated) {
            ParticipantRole participantRole = notification.getParticipant(recipient).getRole();
            String participantRolePrefix = getPrefixByRole(participantRole);
            String messageCode = participantRolePrefix + ".item";
            String messageTypeCode = participantRolePrefix + ".created";
            outputBuilder.append(context.message(messageCode, meetingName,
                    context.formatInterval(notification.getInterval()),
                    context.message(messageTypeCode)));
        }
        else if (notification instanceof RoomNotification.RoomDeleted) {
            outputBuilder.append(context.message("room.participation.access.item", meetingName,
                    context.formatInterval(notification.getInterval()),
                    context.message("room.participation.access.deleted")));
        }
        else if (notification instanceof RoomNotification.RoomModified) {
            RoomNotification.RoomModified roomModified = (RoomNotification.RoomModified) notification;
            boolean isRoomModified = roomModified.isRoomModified();
            boolean isParticipantRoleModified = roomModified.isParticipantRoleModified(recipient);

            // Append to output
            String messageCode;
            if (isRoomModified && isParticipantRoleModified) {
                messageCode = "room.participation.modified.item.roomAndRole";
            }
            else if (isRoomModified) {
                messageCode = "room.participation.modified.item.room";
            }
            else if (isParticipantRoleModified) {
                messageCode = "room.participation.modified.item.role";
            }
            else {
                throw new IllegalStateException();
            }
            Interval interval = notification.getInterval();
            outputBuilder.append(context.message(messageCode, meetingName, context.formatInterval(interval)));

            // Append participant role modified records
            if (isParticipantRoleModified) {
                // Append old role disabled
                ParticipantRole oldParticipantRole = roomModified.getOldParticipant(recipient).getRole();
                String oldParticipantRolePrefix = getPrefixByRole(oldParticipantRole);
                recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                recordsBuilder.append(context.message(oldParticipantRolePrefix + ".modified",
                        context.message(oldParticipantRolePrefix + ".deleted")));
                recordsBuilder.append(".");

                // Append new role enabled
                ParticipantRole newParticipantRole = roomModified.getNewParticipant(recipient).getRole();
                String newParticipantRolePrefix = getPrefixByRole(newParticipantRole);
                recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                recordsBuilder.append(context.message(newParticipantRolePrefix + ".modified",
                        context.message(newParticipantRolePrefix + ".created")));
                recordsBuilder.append(".");
            }

            // Append room modified records
            if (isRoomModified) {
                RoomEndpoint oldRoomEndpoint = roomModified.getOldRoomEndpoint();
                RoomEndpoint newRoomEndpoint = roomModified.getNewRoomEndpoint();

                Interval oldSlot = oldRoomEndpoint.getOriginalSlot();
                Interval newSlot = newRoomEndpoint.getOriginalSlot();
                if (!oldSlot.equals(newSlot)) {
                    recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                    recordsBuilder.append(context.message("room.participation.modified.item.oldSlot",
                            context.formatInterval(oldSlot)));
                }
            }
        }

        // Append slot before/after record
        if (!(notification instanceof RoomNotification.RoomDeleted)) {
            RoomEndpoint roomEndpoint = notification.getRoomEndpoint();
            int slotMinutesBefore = roomEndpoint.getSlotMinutesBefore();
            int slotMinutesAfter = roomEndpoint.getSlotMinutesAfter();
            if (slotMinutesBefore > 0 && slotMinutesAfter > 0) {
                recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                recordsBuilder.append(
                        context.message("room.slot.beforeAndAfter", slotMinutesBefore, slotMinutesAfter));
            }
            else if (slotMinutesBefore > 0) {
                recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                recordsBuilder.append(context.message("room.slot.before", slotMinutesBefore));
            }
            else if (slotMinutesAfter > 0) {
                recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
                recordsBuilder.append(context.message("room.slot.after", slotMinutesAfter));
            }
        }

        // Append description record
        String description = notification.getRoomEndpoint().getRoomDescription();
        String descriptionLabel = context.message("room.description.item");
        if (description != null && !description.equals(this.description)) {
            recordsBuilder.append(NOTIFICATION_RECORD_PREFIX);
            recordsBuilder.append(descriptionLabel);
            recordsBuilder.append(": ");
            recordsBuilder.append(context.indentNextLines(5 + descriptionLabel.length(), description));
        }

        // Append records if not empty
        if (recordsBuilder.length() > 0) {
            outputBuilder.append(":");
            outputBuilder.append(recordsBuilder);
        }
        else {
            outputBuilder.append(".");
        }

        return outputBuilder.toString();
    }

    private String getPrefixByRole(ParticipantRole participantRole)
    {
        if (ParticipantRole.ADMINISTRATOR.equals(participantRole)) {
            return "room.participation.ADMINISTRATOR";
        }
        else if (ParticipantRole.PRESENTER.equals(participantRole)) {
            return "room.participation.PRESENTER";
        }
        else {
            return "room.participation.access";
        }

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
