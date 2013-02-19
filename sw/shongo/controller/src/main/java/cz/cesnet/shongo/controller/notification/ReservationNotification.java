package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;

import javax.persistence.EntityManager;
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
     * @see Reservation
     */
    private Reservation reservation;

    /**
     * @see EntityManager
     */
    EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param type
     * @param reservation
     * @param notificationManager
     * @param entityManager
     */
    public ReservationNotification(Type type, Reservation reservation, NotificationManager notificationManager,
            EntityManager entityManager)
    {
        super(notificationManager);
        this.type = type;
        this.reservation = reservation;
        this.entityManager = entityManager;
        addUserRecipient(reservation.getUserId());
        addRecipientByReservation(reservation);
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
                addRecipient(person);
            }
        }
        if (reservation instanceof RoomReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            for (Person person : resourceReservation.getResource().getAdministrators()) {
                addRecipient(person);
            }
        }
        if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            for (Person person : aliasReservation.getAliasProviderCapability().getResource().getAdministrators()) {
                addRecipient(person);
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addRecipientByReservation(childReservation);
        }
    }

    @Override
    public String getName()
    {
        return type.getName() + " reservation " + IdentifierFormat.formatGlobalId(reservation);
    }

    @Override
    public String getContent()
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        AbstractReservationRequest reservationRequest =
                reservationRequestManager.getByReservation(reservation.getId());
        String content = null;
        try {
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi = null;
            if (reservationRequest != null) {
                reservationRequestApi = reservationRequest.toApi();
            }

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("type", type);
            parameters.put("reservationRequest", reservationRequestApi);
            parameters.put("reservation", reservation.toApi());

            List<cz.cesnet.shongo.controller.api.AliasReservation> aliasReservations =
                    new ArrayList<cz.cesnet.shongo.controller.api.AliasReservation>();
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
            parameters.put("aliasReservations", aliasReservations);

            content = renderTemplate("reservation-mail.ftl", parameters);
        }
        catch (Exception exception) {
            logger.error("Failed to notify about new reservations.", exception);
        }
        return content;
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
