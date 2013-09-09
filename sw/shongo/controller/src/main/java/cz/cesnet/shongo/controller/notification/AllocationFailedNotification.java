package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.OtherPerson;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.report.Report;
import org.joda.time.Interval;

/**
 * {@link cz.cesnet.shongo.controller.notification.ConfigurableNotification} for changes in allocation of {@link cz.cesnet.shongo.controller.request.ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationFailedNotification extends AbstractReservationRequestNotification
{
    private Interval requestedSlot;

    private Target target;

    private String reason;

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param configuration
     */
    public AllocationFailedNotification(ReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, cz.cesnet.shongo.controller.Configuration configuration)
    {
        super(reservationRequest, configuration, authorizationManager.getUserSettingsProvider());

        this.requestedSlot = reservationRequest.getSlot();
        this.target = Target.createInstance(reservationRequest.getSpecification());
        this.reason = reservationRequest.getReportText(Report.MessageType.USER);
        for (PersonInformation administrator : configuration.getAdministrators()) {
            addRecipient(administrator, true);
        }
    }

    public Interval getRequestedSlot()
    {
        return requestedSlot;
    }

    public Target getTarget()
    {
        return target;
    }

    public String getReason()
    {
        return reason;
    }

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification");
        renderContext.addParameter("target", target);

        StringBuilder titleBuilder = new StringBuilder();
        if (configuration.isAdministrator()) {
            String reservationRequestId = getReservationRequestId();
            if (reservationRequestId != null) {
                titleBuilder.append("[");
                titleBuilder.append(reservationRequestId);
                titleBuilder.append("] ");
            }

        }
        titleBuilder.append(renderContext.message("allocationFailed"));

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "allocation-failed.ftl");
        return message;
    }
}
