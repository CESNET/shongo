package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * {@link cz.cesnet.shongo.controller.notification.ConfigurableNotification} for changes in allocation of {@link cz.cesnet.shongo.controller.request.ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationFailedNotification extends ConfigurableNotification
{
    private Interval requestedSlot;

    private Target target;

    private String reason;

    /**
     * Constructor.
     *
     * @param reservationRequest
     */
    public AllocationFailedNotification(ReservationRequest reservationRequest)
    {
        super();

        this.requestedSlot = reservationRequest.getSlot();
        this.target = Target.createInstance(reservationRequest.getSpecification());
        this.reason = reservationRequest.getReportText(Report.MessageType.USER);
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
        titleBuilder.append(renderContext.message("allocationFailed"));

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "allocation-failed.ftl");
        return message;
    }
}
