package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReservationNotification extends AbstractReservationRequestNotification
{
    private UserInformation user;

    private String id;

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

        // Add administrators as recipients
        addAdministratorRecipientsForReservation(reservation.getTargetReservation(), authorizationManager);

        // Add child targets
        for (Reservation childReservation : reservation.getChildReservations()) {
            addChildTargets(childReservation, entityManager);
        }
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
        return renderTemplateMessage(renderContext, titleBuilder.toString(), templateFileName);
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
}
