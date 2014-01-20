package cz.cesnet.shongo.controller.notification.event;


import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Locale;

/**
 * {@link ConfigurableEvent} with {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequestEvent extends ConfigurableEvent
{
    private String reservationRequestId;

    private String reservationRequestUrl;

    private String reservationRequestDescription;

    private DateTime reservationRequestUpdatedAt;

    private String reservationRequestUpdatedBy;

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param configuration
     * @param userSettingsManager
     */
    public AbstractReservationRequestEvent(AbstractReservationRequest reservationRequest,
            ControllerConfiguration configuration, UserSettingsManager userSettingsManager)
    {
        super(userSettingsManager, configuration);

        if (reservationRequest != null) {
            this.reservationRequestId = ObjectIdentifier.formatId(reservationRequest);
            this.reservationRequestUrl = configuration.getNotificationReservationRequestUrl(this.reservationRequestId);
            this.reservationRequestDescription = reservationRequest.getDescription();
            this.reservationRequestUpdatedAt = reservationRequest.getUpdatedAt();
            this.reservationRequestUpdatedBy = reservationRequest.getUpdatedBy();
        }
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
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }
}
