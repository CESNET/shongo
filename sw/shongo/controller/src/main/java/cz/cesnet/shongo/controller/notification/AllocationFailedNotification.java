package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.AllocationStateReport;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Locale;

/**
 * {@link cz.cesnet.shongo.controller.notification.ConfigurableNotification} for changes in allocation of {@link cz.cesnet.shongo.controller.booking.request.ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationFailedNotification extends AbstractReservationRequestNotification
{
    private Interval requestedSlot;

    private Target target;

    private AllocationStateReport.UserError userError;
    private AllocationStateReport adminReport;

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param configuration
     */
    public AllocationFailedNotification(ReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, ControllerConfiguration configuration)
    {
        super(reservationRequest, configuration, authorizationManager.getUserSettingsManager());

        EntityManager entityManager = authorizationManager.getEntityManager();

        this.requestedSlot = reservationRequest.getSlot();
        this.target = Target.createInstance(reservationRequest, entityManager);
        this.userError = reservationRequest.getAllocationStateReport(Report.UserType.USER).toUserError();
        this.adminReport = reservationRequest.getAllocationStateReport(Report.UserType.DOMAIN_ADMIN);
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

    @Override
    protected NotificationMessage renderMessageForConfiguration(Configuration configuration)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification",
                this.configuration.getNotificationUserSettingsUrl());
        renderContext.addParameter("target", target);
        renderContext.addParameter("userError", this.userError.getMessage(locale, timeZone));
        if (configuration.isAdministrator()) {
            renderContext.addParameter("adminReport", adminReport.toString(locale, timeZone));
        }

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
        if (configuration.isAdministrator() && this.userError.isUnknown()) {
            titleBuilder.append(" (");
            titleBuilder.append(renderContext.message("allocationFailed.userErrorUnknown"));
            titleBuilder.append(")");
        }

        NotificationMessage message = renderMessageFromTemplate(
                renderContext, titleBuilder.toString(), "allocation-failed.ftl");
        return message;
    }
}
