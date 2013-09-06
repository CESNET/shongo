package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.MessageSource;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.*;

/**
 * {@link ConfigurableNotification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotification extends ConfigurableNotification
{
    private Type type;

    private String id;

    private Set<String> owners = new HashSet<String>();

    private Interval slot;

    private String reservationRequestId;

    private String reservationRequestUrl;

    private String reservationRequestDescription;

    private DateTime reservationRequestUpdatedAt;

    private String reservationRequestUpdatedBy;

    private Target target;

    /**
     * Constructor.
     *
     * @param type
     * @param reservation
     * @param reservationRequest
     * @param configuration
     */
    public ReservationNotification(Type type, Reservation reservation, AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, cz.cesnet.shongo.controller.Configuration configuration)
    {
        super(authorizationManager.getUserSettingsProvider());

        EntityIdentifier reservationId = new EntityIdentifier(reservation);

        this.type = type;
        this.id = reservationId.toId();
        this.slot = reservation.getSlot();
        this.target = Target.createInstance(reservation, authorizationManager.getEntityManager());
        this.owners.addAll(authorizationManager.getUserIdsWithRole(reservationId, Role.OWNER));

        if (reservationRequest != null) {
            this.reservationRequestId = EntityIdentifier.formatId(reservationRequest);
            this.reservationRequestUrl = configuration.getReservationRequestUrl(this.reservationRequestId);
            this.reservationRequestDescription = reservationRequest.getDescription();
            this.reservationRequestUpdatedAt = reservationRequest.getUpdatedAt();
            this.reservationRequestUpdatedBy = reservationRequest.getUpdatedBy();
        }

        // Add administrators as recipients
        addAdministratorRecipientsForReservation(reservation);
    }

    public Type getType()
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

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public String getReservationRequestUrl()
    {
        return reservationRequestUrl;
    }

    public String getReservationRequestDescription()
    {
        return reservationRequestDescription;
    }

    public DateTime getReservationRequestUpdatedAt()
    {
        return reservationRequestUpdatedAt;
    }

    public String getReservationRequestUpdatedBy()
    {
        return reservationRequestUpdatedBy;
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification");
        renderContext.addParameter("target", target);

        StringBuilder nameBuilder = new StringBuilder();
        if (configuration.isAdministrator()) {
            nameBuilder.append("[");
            nameBuilder.append(target.getResourceName());
            nameBuilder.append("] [");
            nameBuilder.append(renderContext.message("target.type." + target.getType()));
            nameBuilder.append("] ");
            nameBuilder.append(renderContext.message("reservation.type." + type));
            nameBuilder.append(" ");
            nameBuilder.append(renderContext.message("reservation"));
            nameBuilder.append(" ");
            nameBuilder.append(renderContext.formatInterval(slot));
        }
        else {
            nameBuilder.append(renderContext.message("reservation.type." + type));
            nameBuilder.append(" ");
            nameBuilder.append(renderContext.message("reservation"));
            nameBuilder.append(" - ");
            nameBuilder.append(renderContext.message("target.type." + target.getType()));
        }

        String templateFileName;
        if (configuration instanceof ReservationRequestNotification.Configuration) {
            templateFileName = "reservation-request-reservation.ftl";
        }
        else {
            templateFileName = "reservation.ftl";
        }
        return renderMessageFromTemplate(renderContext, nameBuilder.toString(), templateFileName);
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
            for (Person person : resourceReservation.getResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            for (Person person : roomReservation.getDeviceResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            for (Person person : aliasReservation.getAliasProviderCapability().getResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addAdministratorRecipientsForReservation(childReservation);
        }
    }

    /**
     * Type of the {@link ReservationNotification}.
     */
    public static enum Type
    {
        /**
         * {@link ReservationNotification} for new reservation.
         */
        NEW,

        /**
         * {@link ReservationNotification} for modified reservation.
         */
        MODIFIED,

        /**
         * {@link ReservationNotification} for deleted reservation.
         */
        DELETED
    }
}
