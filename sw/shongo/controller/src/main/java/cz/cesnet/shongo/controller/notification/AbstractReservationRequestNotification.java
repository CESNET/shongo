package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import org.joda.time.DateTime;
import org.joda.time.Interval;

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

        // Group notifications for reservation request
        if (!(this instanceof ReservationRequestNotification)) {
            Long reservationRequestId = ObjectIdentifier.parseId(
                    AbstractReservationRequest.class, this.reservationRequestId);
            if (reservationRequestId != null) {
                // Get top reservation request
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                AbstractReservationRequest reservationRequest = reservationRequestManager.get(reservationRequestId);
                if (reservationRequest instanceof ReservationRequest) {
                    Allocation parentAllocation =
                            ((ReservationRequest) reservationRequest).getParentAllocation();
                    if (parentAllocation != null) {
                        reservationRequest = parentAllocation.getReservationRequest();
                    }
                }
                // Add reservation notification to reservation request notification
                ReservationRequestNotification reservationRequestNotification =
                        notificationManager.getReservationRequestNotification(reservationRequest, entityManager);
                reservationRequestNotification.addNotification(this);
            }
        }

        return true;
    }

    /**
     * @return {@link Interval} of the reservation request
     */
    public abstract Interval getSlot();

    /**
     * @return {@link org.joda.time.Interval#getStart()} for the {@link #getSlot()}
     */
    public final DateTime getSlotStart()
    {
        return getSlot().getStart();
    }
}
