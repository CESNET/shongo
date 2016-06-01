package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Locale;

/**
 * Confirmation {@link ConfigurableNotification} for {@link Resource} {@link ReservationRequest's} administrators ().
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ReservationRequestConfirmationNotification extends ConfigurableNotification
{
    private String reservationRequestId;

    private String reservationRequestDescription;

    private DateTime reservationRequestUpdatedAt;

    private String reservationRequestUpdatedBy;

    private Target target;

    private Interval requestedSlot;

    public ReservationRequestConfirmationNotification(ReservationRequest reservationRequest)
    {
        this.reservationRequestId = ObjectIdentifier.formatId(reservationRequest);
        this.requestedSlot = reservationRequest.getSlot();
        this.reservationRequestDescription = reservationRequest.getDescription();
        this.reservationRequestUpdatedAt = reservationRequest.getUpdatedAt();
        this.reservationRequestUpdatedBy = reservationRequest.getUpdatedBy();
        this.target = Target.createInstance(reservationRequest, null);
    }

    public Target getTarget()
    {
        return target;
    }

    public void setTarget(Target target)
    {
        this.target = target;
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    public String getReservationRequestDescription()
    {
        return reservationRequestDescription;
    }

    public void setReservationRequestDescription(String reservationRequestDescription)
    {
        this.reservationRequestDescription = reservationRequestDescription;
    }

    public DateTime getReservationRequestUpdatedAt()
    {
        return reservationRequestUpdatedAt;
    }

    public void setReservationRequestUpdatedAt(DateTime reservationRequestUpdatedAt)
    {
        this.reservationRequestUpdatedAt = reservationRequestUpdatedAt;
    }

    public String getReservationRequestUpdatedBy()
    {
        return reservationRequestUpdatedBy;
    }

    public void setReservationRequestUpdatedBy(String reservationRequestUpdatedBy)
    {
        this.reservationRequestUpdatedBy = reservationRequestUpdatedBy;
    }

    public Interval getRequestedSlot()
    {
        return requestedSlot;
    }

    public void setRequestedSlot(Interval requestedSlot)
    {
        this.requestedSlot = requestedSlot;
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);
        renderContext.addParameter("target", target);

        StringBuilder titleBuilder = new StringBuilder();
//        if (configuration.isAdministrator()) {
//            String reservationRequestId = getReservationRequestId();
//            if (reservationRequestId != null) {
//                titleBuilder.append("[failed] [req:");
//                titleBuilder.append(ObjectIdentifier.parseLocalId(reservationRequestId, ObjectType.RESERVATION_REQUEST));
//                titleBuilder.append("] ");
//            }
//
//        }
        titleBuilder.append(renderContext.message("reservationRequestConfirmation.title"));


        NotificationMessage message = renderTemplateMessage(
                renderContext, titleBuilder.toString(), "reservation-request-confirmation.ftl");
        return message;
    }
}
