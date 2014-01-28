package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Locale;

/**
 * {@link ConfigurableNotification} with {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequestNotification extends ConfigurableNotification
{
    private String reservationRequestId;

    private String reservationRequestDescription;

    private DateTime reservationRequestUpdatedAt;

    private String reservationRequestUpdatedBy;

    /**
     * Constructor.
     *
     * @param reservationRequest
     */
    public AbstractReservationRequestNotification(AbstractReservationRequest reservationRequest)
    {
        if (reservationRequest != null) {
            this.reservationRequestId = ObjectIdentifier.formatId(reservationRequest);
            this.reservationRequestDescription = reservationRequest.getDescription();
            this.reservationRequestUpdatedAt = reservationRequest.getUpdatedAt();
            this.reservationRequestUpdatedBy = reservationRequest.getUpdatedBy();
        }
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
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
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        if(!super.onBeforeAdded(notificationManager, entityManager)) {
            return false;
        }

        return true;
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        // Group notifications for reservation request
        if (!(this instanceof ReservationRequestNotification)) {
            Long reservationRequestId = ObjectIdentifier.parseId(
                    AbstractReservationRequest.class, this.reservationRequestId);
            if (reservationRequestId != null) {
                // Get top reservation request
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                AbstractReservationRequest abstractReservationRequest =
                        reservationRequestManager.get(reservationRequestId);
                if (abstractReservationRequest instanceof ReservationRequest) {
                    ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
                    Allocation parentAllocation = reservationRequest.getParentAllocation();
                    if (parentAllocation != null) {
                        AbstractReservationRequest parentReservationRequest = parentAllocation.getReservationRequest();
                        if (parentReservationRequest != null) {
                            abstractReservationRequest = parentReservationRequest;
                        }
                    }
                }

                // Create or reuse reservation request notification
                Long abstractReservationRequestId = abstractReservationRequest.getId();
                ReservationRequestNotification reservationRequestNotification =
                        notificationManager.reservationRequestNotificationsById.get(abstractReservationRequestId);
                if (reservationRequestNotification == null) {
                    AuthorizationManager authorizationManager = new AuthorizationManager(
                            entityManager, notificationManager.getAuthorization());
                    reservationRequestNotification =
                            new ReservationRequestNotification(abstractReservationRequest, authorizationManager);
                    notificationManager.addNotification(reservationRequestNotification, entityManager);
                }

                // Add reservation notification to reservation request notification
                reservationRequestNotification.addNotification(this);
            }
        }
    }
}
