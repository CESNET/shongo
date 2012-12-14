package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Notification} for a new {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NewReservationNotification extends Notification
{
    private Reservation reservation;

    public NewReservationNotification(Reservation reservation, NotificationManager notificationManager)
    {
        super(notificationManager);
        this.reservation = reservation;
        addUserRecipient(reservation.getUserId());
    }

    @Override
    public String getName()
    {
        return "New reservation " + getNotificationManager().getDomain().formatId(reservation.getId());
    }

    @Override
    public String getContent()
    {
        EntityManager entityManager = getNotificationManager().createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AbstractReservationRequest reservationRequest =
                reservationRequestManager.getByReservation(reservation.getId());
        String content = null;
        try {
            Domain domain = getNotificationManager().getDomain();
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi =
                    reservationRequest.toApi(domain);
            cz.cesnet.shongo.controller.api.Specification specificationApi = getSpecification(reservationRequestApi);


            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("reservation", reservation.toApi(domain));
            parameters.put("reservationRequest", reservationRequestApi);
            parameters.put("specification", specificationApi);

            content = renderTemplate("new-reservation-mail.vm", parameters);
        }
        catch (Exception exception) {
            logger.error("Failed to notify about new reservations.", exception);
        }
        entityManager.close();
        return content;
    }

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
}
