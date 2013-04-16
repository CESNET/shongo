package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.InternalErrorHandler;
import cz.cesnet.shongo.controller.report.InternalErrorType;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.fault.FaultException;

import java.util.*;

/**
 * {@link Notification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotification extends Notification
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * Parameters.
     */
    List<String> ownerUserIds = new LinkedList<String>();
    cz.cesnet.shongo.controller.api.Reservation reservation = null;
    cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequest = null;
    List<cz.cesnet.shongo.controller.api.AliasReservation> aliasReservations =
            new LinkedList<cz.cesnet.shongo.controller.api.AliasReservation>();

    /**
     * Constructor.
     *
     * @param type
     * @param reservation
     */
    public ReservationNotification(Type type, Reservation reservation, AuthorizationManager authorizationManager)
    {
        this.type = type;

        // Add recipients
        for (String userId : authorizationManager.getUserIdsWithRole(new EntityIdentifier(reservation), Role.OWNER)) {
            addUserRecipient(userId);
            ownerUserIds.add(userId);
        }
        addRecipientByReservation(reservation);

        AbstractReservationRequest reservationRequest = reservation.getTopReservationRequest();
        try {
            if (reservationRequest != null) {
                this.reservationRequest = reservationRequest.toApi();
            }
            this.reservation = reservation.toApi();

            if (reservation.getClass().equals(Reservation.class)) {
                Collection<AliasReservation> childAliasReservations =
                        reservation.getChildReservations(AliasReservation.class);
                if (childAliasReservations.size() > 0) {
                    for (AliasReservation aliasReservation : childAliasReservations) {
                        aliasReservations.add(aliasReservation.toApi());
                    }
                }
            }
            else if (reservation instanceof AliasReservation) {
                AliasReservation aliasReservation = (AliasReservation) reservation;
                aliasReservations.add(aliasReservation.toApi());
            }
        }
        catch (FaultException exception) {
            InternalErrorHandler.handle(InternalErrorType.NOTIFICATION,
                    "Failed to create reservation notification", exception);
        }
    }

    /**
     * Add recipients by given {@code reservation}.
     *
     * @param reservation
     */
    public void addRecipientByReservation(Reservation reservation)
    {
        if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            for (Person person : resourceReservation.getResource().getAdministrators()) {
                addRecipient(RecipientGroup.ADMINISTRATOR, person.getInformation());
            }
        }
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            for (Person person : roomReservation.getDeviceResource().getAdministrators()) {
                addRecipient(RecipientGroup.ADMINISTRATOR, person.getInformation());
            }
        }
        if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            for (Person person : aliasReservation.getAliasProviderCapability().getResource().getAdministrators()) {
                addRecipient(RecipientGroup.ADMINISTRATOR, person.getInformation());
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addRecipientByReservation(childReservation);
        }
    }

    @Override
    public String getName()
    {
        return type.getName() + " reservation " + reservation.getId();
    }

    @Override
    public String getContent()
    {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("type", type);
        parameters.put("owners", ownerUserIds);
        parameters.put("reservation", reservation);
        parameters.put("reservationRequest", reservationRequest);
        parameters.put("aliasReservations", aliasReservations);
        return renderTemplate("reservation-mail.ftl", parameters);
    }

    /**
     * Type of the {@link ReservationNotification}.
     */
    public static enum Type
    {
        /**
         * {@link ReservationNotification#reservation} is new.
         */
        NEW("New"),

        /**
         * {@link ReservationNotification#reservation} has been modified.
         */
        MODIFIED("Modified"),

        /**
         * {@link ReservationNotification#reservation} has been deleted.
         */
        DELETED("Deleted");

        /**
         * Name of the {@link Type}.
         */
        private String name;

        /**
         * Constructor.
         *
         * @param name sets the {@link #name}
         */
        private Type(String name)
        {
            this.name = name;
        }

        /**
         * @return {@link #name}
         */
        public String getName()
        {
            return name;
        }
    }
}
