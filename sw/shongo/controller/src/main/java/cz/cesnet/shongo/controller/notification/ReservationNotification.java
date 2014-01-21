package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.acl.AclIdentity;
import cz.cesnet.shongo.controller.acl.AclProvider;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link ConfigurableNotification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotification extends AbstractReservationRequestNotification
{
    private AclProvider aclProvider;

    private UserInformation user;

    private ReservationNotification.Type type;

    private String id;

    private Set<String> owners = new HashSet<String>();

    private Interval slot;

    private Target target;

    private Map<String, Target> childTargetByReservation = new LinkedHashMap<String, Target>();

    /**
     * Constructor.
     *
     * @param type
     * @param reservation
     * @param reservationRequest
     * @param configuration
     */
    public ReservationNotification(ReservationNotification.Type type, Reservation reservation,
            AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager,
            ControllerConfiguration configuration)
    {
        super(reservationRequest, authorizationManager.getUserSettingsManager(), configuration);

        this.aclProvider = authorizationManager.getAuthorization().getAclProvider();

        EntityManager entityManager = authorizationManager.getEntityManager();

        String updatedBy = getReservationRequestUpdatedBy();
        if (updatedBy != null) {
            this.user = authorizationManager.getUserInformation(updatedBy);
        }
        this.type = type;
        this.id = ObjectIdentifier.formatId(reservation);
        this.slot = reservation.getSlot();
        this.target = Target.createInstance(reservation, entityManager);
        this.owners.addAll(authorizationManager.getUserIdsWithRole(reservation, ObjectRole.OWNER));

        // Add administrators as recipients
        addAdministratorRecipientsForReservation(reservation);

        // Add child targets
        for (Reservation childReservation : reservation.getChildReservations()) {
            addChildTargets(childReservation, entityManager);
        }
    }

    public ReservationNotification.Type getType()
    {
        return type;
    }

    public String getId()
    {
        return id;
    }

    public Set<String> getOwners()
    {
        return owners;
    }

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

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification",
                this.configuration.getNotificationUserSettingsUrl());
        renderContext.addParameter("target", target);

        StringBuilder titleBuilder = new StringBuilder();
        if (configuration.isAdministrator()) {
            titleBuilder.append("[");
            titleBuilder.append(target.getResourceName());
            titleBuilder.append("] [");
            titleBuilder.append(renderContext.message("target.type." + target.getType()));
            titleBuilder.append("] ");
            titleBuilder.append(renderContext.message("reservation.type." + type));
            titleBuilder.append(" ");
            titleBuilder.append(renderContext.message("reservation"));
            titleBuilder.append(" (rsv:");
            titleBuilder.append(ObjectIdentifier.parse(id).getPersistenceId());
            titleBuilder.append(") ");
            titleBuilder.append(renderContext.formatInterval(slot));
        }
        else {
            titleBuilder.append(renderContext.message("reservation.type." + type));
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
                slot = new Interval(slot.getStart().plus(room.getSlotBefore()),
                        slot.getEnd().minus(room.getSlotAfter()));
            }
            renderContext.addParameter("slot", slot);
            templateFileName = "reservation-request-reservation.ftl";
        }
        else {
            templateFileName = "reservation.ftl";
        }
        return renderMessageFromTemplate(renderContext, titleBuilder.toString(), templateFileName);
    }

    @Override
    protected NotificationMessage renderMessageForRecipient(PersonInformation recipient)
    {
        NotificationMessage notificationMessage = super.renderMessageForRecipient(recipient);
        if (user != null) {
            notificationMessage.appendTitleAfter("] ", "(" + user.getFullName() + ") ");
        }
        return notificationMessage;
    }

    @Override
    public NotificationRecord createRecordForRecipient(PersonInformation recipient, EntityManager entityManager)
    {
        NotificationRecord notificationRecord = new NotificationRecord();
        notificationRecord.setCreatedAt(getCreatedAt());
        notificationRecord.setRecipientType(NotificationRecord.RecipientType.USER);
        notificationRecord.setRecipientId(0l);
        if (type.equals(Type.NEW)) {
            notificationRecord.setNotificationType(NotificationRecord.NotificationType.ROOM_CREATED);
        }
        else {
            notificationRecord.setNotificationType(NotificationRecord.NotificationType.ROOM_DELETED);
        }
        notificationRecord.setTargetId(ObjectIdentifier.parseId(Reservation.class, id));
        return notificationRecord;
    }

    /**
     * Add recipients by given {@code reservation}.
     *
     * @param reservation
     */
    private void addAdministratorRecipientsForReservation(Reservation reservation)
    {
        if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            for (AbstractPerson person : resourceReservation.getResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            for (AbstractPerson person : roomReservation.getDeviceResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            for (AbstractPerson person : aliasReservation.getAliasProviderCapability().getResource()
                    .getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof ValueReservation) {
            ValueReservation valueReservation = (ValueReservation) reservation;
            for (AbstractPerson person : valueReservation.getValueProvider().getCapabilityResource()
                    .getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addAdministratorRecipientsForReservation(childReservation);
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

    public static enum Type
    {
        NEW,
        MODIFIED,
        DELETED
    }
}
