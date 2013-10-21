package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.settings.UserSettingsProvider;
import org.joda.time.DateTime;

/**
 * {@link ConfigurableNotification} with {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequestNotification extends ConfigurableNotification
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
     * @param userSettingsProvider
     */
    public AbstractReservationRequestNotification(AbstractReservationRequest reservationRequest,
            cz.cesnet.shongo.controller.Configuration configuration, UserSettingsProvider userSettingsProvider)
    {
        super(userSettingsProvider, configuration);

        if (reservationRequest != null) {
            this.reservationRequestId = EntityIdentifier.formatId(reservationRequest);
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
}
