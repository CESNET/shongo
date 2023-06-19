package cz.cesnet.shongo.controller.notification;


import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.TagType;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataException;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataFilter;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataService;
import cz.cesnet.shongo.controller.booking.request.auxdata.tagdata.NotifyEmailAuxData;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.util.iCalendar;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link ConfigurableNotification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReservationNotification extends AbstractReservationRequestNotification
{
    private UserInformation user;

    private String id;

    private UserInformation organizer;

    private Set<String> owners = new HashSet<String>();

    private Interval slot;

    private Target target;

    private Map<String, Target> childTargetByReservation = new LinkedHashMap<String, Target>();

    private ReservationNotification(Reservation reservation,
            AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        super(reservationRequest);

        EntityManager entityManager = authorizationManager.getEntityManager();

        String updatedBy = getReservationRequestUpdatedBy();
        if (updatedBy != null && UserInformation.isLocal(updatedBy)) {
            this.user = authorizationManager.getUserInformation(updatedBy);
        }
        this.id = ObjectIdentifier.formatId(reservation);
        this.slot = reservation.getSlot();
        this.target = Target.createInstance(reservation, entityManager);
        this.owners.addAll(authorizationManager.getUserIdsWithRole(reservation, ObjectRole.OWNER).getUserIds());
        this.organizer = authorizationManager.getUserInformation(reservation.getUserId());

        // Add administrators as recipients
        addAdministratorRecipientsForReservation(reservation.getTargetReservation(), authorizationManager);
        addRecipientsFromNotificationTags(reservationRequest, entityManager);

        // Add child targets
        for (Reservation childReservation : reservation.getChildReservations()) {
            addChildTargets(childReservation, entityManager);
        }
    }

    private void addRecipientsFromNotificationTags(AbstractReservationRequest reservationRequest,
            EntityManager entityManager)
    {
        AuxDataFilter filter = AuxDataFilter.builder()
                .tagType(TagType.NOTIFY_EMAIL)
                .enabled(true)
                .build();

        List<NotifyEmailAuxData> notifyEmailAuxData;
        try {
            notifyEmailAuxData = AuxDataService.getTagData(reservationRequest, filter, entityManager);
        } catch (JsonProcessingException e) {
            logger.error("Error while parsing auxData", e);
            return;
        } catch (AuxDataException e) {
            logger.warn("Error while getting notify email aux data for reservation request {}.", reservationRequest.getId(), e);
            return;
        }

        List<PersonInformation> tagPersonInformationList = notifyEmailAuxData
                .stream()
                .map(this::notifyEmailDataToPersonInformation)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        logger.debug("Adding tag recipients: {}", tagPersonInformationList);
        tagPersonInformationList.forEach(personInformation -> addRecipient(personInformation, true));
    }

    private Collection<TagPersonInformation> notifyEmailDataToPersonInformation(NotifyEmailAuxData notifyEmailAuxData)
    {
        return notifyEmailAuxData
                .getData()
                .stream()
                .map(email -> new TagPersonInformation(notifyEmailAuxData.getTag().getName(), email))
                .collect(Collectors.toList());
    }

    public String getId()
    {
        return id;
    }

    public Set<String> getOwners()
    {
        return owners;
    }

    @Override
    public Interval getSlot()
    {
        return slot;
    }

    public Target getTarget()
    {
        return target;
    }

    public Map<String, Target> getChildTargetByReservation()
    {
        return childTargetByReservation;
    }

    public abstract String getType();

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();

        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);
        renderContext.addParameter("target", target);

        StringBuilder titleBuilder = new StringBuilder();
        if (configuration.isAdministrator()) {
            titleBuilder.append("[");
            titleBuilder.append(target.getResourceName());
            titleBuilder.append("] [");
            titleBuilder.append(renderContext.message("target.type." + target.getType()));
            titleBuilder.append("] ");
            if (target instanceof Target.Room) {
                Target.Room roomTarget = (Target.Room) target;
                Target.Room reusedRoomTarget = roomTarget.getReusedRoom();
                if (reusedRoomTarget != null) {
                    String roomName = reusedRoomTarget.getName();
                    if (roomName != null) {
                        titleBuilder.append("[");
                        titleBuilder.append(roomName);
                        titleBuilder.append("] ");
                    }
                }
            }
            titleBuilder.append(renderContext.message("reservation.type." + getType()));
            titleBuilder.append(" ");
            titleBuilder.append(renderContext.message("reservation"));
            titleBuilder.append(" (");
            titleBuilder.append(getTitleReservationId(renderContext));
            titleBuilder.append(") ");
            titleBuilder.append(renderContext.formatInterval(slot));
        }
        else {
            titleBuilder.append(renderContext.message("reservation.type." + getType()));
            titleBuilder.append(" ");
            titleBuilder.append(renderContext.message("reservation"));
            titleBuilder.append(" - ");
            titleBuilder.append(renderContext.message("target.type." + target.getType()));
        }

        String templateFileName;
        if (configuration instanceof ParentConfiguration) {
            Interval slot = getSlot();
            if (this.target instanceof Target.Room) {
                // We must compute the original time slot
                Target.Room room = (Target.Room) this.target;
                slot = RoomEndpoint.getOriginalSlot(slot, room.getSlotBefore(), room.getSlotAfter());
            }
            renderContext.addParameter("slot", slot);

            if (this.getPeriod() != null) {
                if (this.getPeriod().equals(Period.days(1))) {
                    //TODO:hezci
                }

                renderContext.addParameter("period", this.getPeriod());
                renderContext.addParameter("end", this.getEnd());

                HashMap<String, String> errorsBySlot = new LinkedHashMap<String, String>();
                for (AllocationFailedNotification notification : getAdditionalFailedRequestNotifications()) {
                    String stringSlot = renderContext.formatInterval(notification.getSlot());
                    errorsBySlot.put(stringSlot, notification.getUserError().getMessage(locale, timeZone));
                }
                if (!errorsBySlot.isEmpty()) {
                    renderContext.addParameter("errors", errorsBySlot);
                }

                List<Interval> deleted = new LinkedList<Interval>();
                for (Interval deletedSlot : getAdditionalDeletedSlots()) {
                    deleted.add(deletedSlot);
                }
                if (!deleted.isEmpty()) {
                    renderContext.addParameter("deletedList", deleted);
                }
            }
            templateFileName = "reservation-request-reservation.ftl";
        }
        else {
            templateFileName = "reservation.ftl";
        }

        // Add recording to notifications
        renderContext.addParameter("roomRecorded", "no");
        for (Target childTarget : childTargetByReservation.values()) {
            if (childTarget instanceof Target.RecordingService) {
                renderContext.addParameter("roomRecorded", "yes");
            }
        }
        NotificationMessage message = renderTemplateMessage(renderContext, titleBuilder.toString(), templateFileName);

        ConfiguredRenderContext context = new ConfiguredRenderContext(configuration, "notification", manager);
        // Add iCal to attachments
        iCalendar calendar = new iCalendar();
        //TODO: slouzi k pridavani priloh do mailu, presunout groupovani iCalu do vytvareni samotnych notifikaci
        iCalendar.Method method;
        if (this instanceof ReservationNotification.New) {
            method = iCalendar.Method.CREATE;
        }
        else if (this instanceof ReservationNotification.Deleted) {
            method = iCalendar.Method.CANCEL;
        }
        else {
            throw new TodoImplementException(this.getClass());
        }
        final String summary = addEvent(calendar, context, method);
        message.addAttachment(new iCalendarNotificationAttachment(summary + ".ics", calendar));
        return message;
    }

    private String addEvent(iCalendar iCalendar, RenderContext context, iCalendar.Method method)
    {
        Interval interval = slot;
        String meetingDescription = getReservationRequestDescription();

        String summary = target.getResourceName();
        String location = target.getResourceName();
        if (target instanceof Target.Room) {
            Target.Room room = (Target.Room) target;
            summary = room.getName();
            for (Alias alias : room.getAliases()) {
                AliasType aliasType = alias.getType();
                if (AliasType.H323_E164.equals(aliasType)) {
                    location = "Video: " + alias.getValue();
                    break;
                }
                else if (AliasType.ADOBE_CONNECT_URI.equals(aliasType)) {
                    location = "Web: " + alias.getValue();
                    break;
                }
                else if (AliasType.FREEPBX_CONFERENCE_NUMBER.equals(aliasType)) {
                    location = "Conference: " + alias.getValue();
                    break;
                }
            }
        }

        if (summary != null) {
            summary = summary.replaceAll("[^a-zA-Z0-9]", "_");
        }
        else {
            summary = context.message("reservation");
        }

        iCalendar.Event event = iCalendar.addEvent(LocalDomain.getLocalDomainName(), id, summary);
        event.setInterval(interval, context.getTimeZone());
        if (meetingDescription != null) {
            event.setDescription(meetingDescription);
        }
        if (location != null) {
            event.setLocation(location);
        }
        event.setOrganizer("mailto:" + organizer.getPrimaryEmail());
        event.setOrganizerName(organizer.getFullName());

        event.setMethod(method);
        return summary;
    }

    /**
     * @return "rsv:<reservation-id>" string
     * @param renderContext
     */
    protected String getTitleReservationId(RenderContext renderContext)
    {
        StringBuilder titleReservationId = new StringBuilder();
        titleReservationId.append("rsv:");
        titleReservationId.append(ObjectIdentifier.parse(id).getPersistenceId());
        return titleReservationId.toString();
    }

    @Override
    protected NotificationMessage renderMessage(PersonInformation recipient,
            NotificationManager notificationManager, EntityManager entityManager)
    {
        NotificationMessage notificationMessage = super.renderMessage(recipient, notificationManager, entityManager);
        if (user != null) {
            notificationMessage.appendTitleAfter("] ", "(" + user.getFullName() + ") ");
        }
        return notificationMessage;
    }

    /**
     * Add recipients by given {@code reservation}.
     *
     * @param reservation
     * @param authorizationManager
     */
    private void addAdministratorRecipientsForReservation(Reservation reservation,
            AuthorizationManager authorizationManager)
    {
        Resource resource = reservation.getAllocatedResource();
        if (resource != null) {
            for (PersonInformation resourceAdministrator : resource.getAdministrators(authorizationManager)) {
                addRecipient(resourceAdministrator, true);
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addAdministratorRecipientsForReservation(childReservation, authorizationManager);
        }
    }

    /**
     * Add to {@link #childTargetByReservation} from given {@code reservation}.
     *
     * @param reservation
     * @param entityManager
     */
    private void addChildTargets(Reservation reservation, EntityManager entityManager)
    {
        Target target = Target.createInstance(reservation, entityManager);
        childTargetByReservation.put(ObjectIdentifier.formatId(reservation), target);
        for (Reservation childReservation : reservation.getChildReservations()) {
            addChildTargets(childReservation, entityManager);
        }
    }

    /**
     * @param reservation for which the {@link AbstractReservationRequest} should be returned
     * @return {@link AbstractReservationRequest} for given {@code reservation}
     */
    public static AbstractReservationRequest getReservationRequest(Reservation reservation)
    {
        Allocation allocation = reservation.getAllocation();
        return (allocation != null ? allocation.getReservationRequest() : null);
    }

    public static enum Type
    {
        NEW,
        MODIFIED,
        DELETED
    }

    public static class New extends ReservationNotification
    {
        private Long previousReservationId;

        public New(Reservation reservation, Reservation previousReservation, AuthorizationManager authorizationManager)
        {
            super(reservation, getReservationRequest(reservation), authorizationManager);

            this.previousReservationId = (previousReservation != null ? previousReservation.getId() : null);
        }

        @Override
        public String getType()
        {
            return "NEW";
        }

        @Override
        protected String getTitleReservationId(RenderContext renderContext)
        {
            if (previousReservationId != null) {
                StringBuilder titleReservationId = new StringBuilder();
                titleReservationId.append(super.getTitleReservationId(renderContext));
                titleReservationId.append(", ");
                titleReservationId.append(renderContext.message("reservation.replace"));
                titleReservationId.append(" rsv:");
                titleReservationId.append(previousReservationId);
                return titleReservationId.toString();
            }
            else {
                return super.getTitleReservationId(renderContext);
            }
        }
    }

    public static class Deleted extends ReservationNotification
    {
        public Deleted(Reservation reservation, AbstractReservationRequest reservationRequest,
                AuthorizationManager authorizationManager)
        {
            super(reservation, reservationRequest, authorizationManager);
        }

        public Deleted(Reservation reservation, AuthorizationManager authorizationManager)
        {
            super(reservation, getReservationRequest(reservation), authorizationManager);
        }

        @Override
        public String getType()
        {
            return "DELETED";
        }
    }

    private static class TagPersonInformation implements PersonInformation
    {

        private final String name;
        private final String email;

        public TagPersonInformation(String name, String email)
        {
            this.name = name;
            this.email = email;
        }

        @Override
        public String getFullName()
        {
            return name;
        }

        @Override
        public String getRootOrganization()
        {
            return null;
        }

        @Override
        public String getPrimaryEmail()
        {
            return email;
        }

        @Override
        public String toString()
        {
            return "Tag[" + name + "] (" + email + ")";
        }
    }
}
