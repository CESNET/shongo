package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

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
        for (Reservation childReservation : reservation.getChildReservations()) {
            addRecipientByReservation(childReservation);
        }
    }

    @Override
    public String getName()
    {
        return type.getName() + " reservation " + getNotificationManager().getDomain().formatId(reservation.getId());
    }

    @Override
    public String getContent()
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AbstractReservationRequest reservationRequest =
                reservationRequestManager.getByReservation(reservation.getId());
        String content = null;
        try {
            Domain domain = getNotificationManager().getDomain();
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi = null;
            if (reservationRequest != null) {
                reservationRequestApi = reservationRequest.toApi(domain);
            }
            cz.cesnet.shongo.controller.api.Specification specificationApi = getSpecification(reservationRequestApi);


            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("type", type);
            parameters.put("reservation", reservation.toApi(domain));
            parameters.put("reservationRequest", reservationRequestApi);
            parameters.put("specification", specificationApi);

            content = renderTemplate("reservation-mail.vm", parameters);
        }
        catch (Exception exception) {
            logger.error("Failed to notify about new reservations.", exception);
        }
        return content;
    }

    /**
     * @param reservationRequestApi
     * @return specification from given reservation request
     */
    private cz.cesnet.shongo.controller.api.Specification getSpecification(
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        if (reservationRequestApi instanceof cz.cesnet.shongo.controller.api.ReservationRequest) {
            cz.cesnet.shongo.controller.api.ReservationRequest singleReservationRequestApi =
                    (cz.cesnet.shongo.controller.api.ReservationRequest) reservationRequestApi;
            return singleReservationRequestApi.getSpecification();
        }
        else if (reservationRequestApi instanceof cz.cesnet.shongo.controller.api.ReservationRequestSet) {
            cz.cesnet.shongo.controller.api.ReservationRequestSet reservationRequestSetApi =
                    (cz.cesnet.shongo.controller.api.ReservationRequestSet) reservationRequestApi;
            if ( reservationRequestSetApi.getSpecifications().size() > 0) {
                return reservationRequestSetApi.getSpecifications().get(0);
            }
        }
        return null;
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
