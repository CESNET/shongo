package cz.cesnet.shongo.controller.notification;

        import cz.cesnet.shongo.api.UserInformation;
        import cz.cesnet.shongo.controller.ObjectRole;
        import cz.cesnet.shongo.controller.ObjectType;
        import cz.cesnet.shongo.controller.api.AllocationStateReport;
        import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
        import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
        import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
        import cz.cesnet.shongo.controller.booking.resource.Resource;
        import cz.cesnet.shongo.report.Report;
        import org.joda.time.DateTimeZone;

        import java.util.Locale;

/**
 * Confirmation {@link ConfigurableNotification} for {@link Resource} {@link ReservationRequest 's} administrators ().
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ReservationRequestDeniedNotification extends ReservationRequestConfirmationNotification
{
    private AllocationStateReport.UserError userError;

    private AllocationStateReport adminReport;

    public ReservationRequestDeniedNotification(ReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        super(reservationRequest);

        this.userError = reservationRequest.getAllocationStateReport(Report.UserType.USER).toUserError();
        this.adminReport = reservationRequest.getAllocationStateReport(Report.UserType.DOMAIN_ADMIN);

        if (this.userError instanceof AllocationStateReport.ReservationRequestDenied) {
            AllocationStateReport.ReservationRequestDenied reservationRequestDenied;
            reservationRequestDenied = (AllocationStateReport.ReservationRequestDenied) this.userError;
            String userId = reservationRequestDenied.getUserId();
            String userName = authorizationManager.getUserInformation(userId).getFullName();

            reservationRequestDenied.setUserName(userName);
        }

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER).getUserIds()) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);
        renderContext.addParameter("target", getTarget());

        StringBuilder titleBuilder = new StringBuilder();
        if (configuration.isAdministrator()) {
            String reservationRequestId = getReservationRequestId();
            if (reservationRequestId != null) {
                titleBuilder.append("[denied] [req:");
                titleBuilder.append(ObjectIdentifier.parseLocalId(reservationRequestId, ObjectType.RESERVATION_REQUEST));
                titleBuilder.append("] ");
            }

        }
        titleBuilder.append(renderContext.message("reservationRequestDenied.title"));

        renderContext.addParameter("userError",this.userError.getMessage(locale, timeZone));
        if (configuration.isAdministrator()) {
            renderContext.addParameter("adminReport", adminReport.toString(locale, timeZone));
        }

        NotificationMessage message = renderTemplateMessage(
                renderContext, titleBuilder.toString(), "reservation-request-denied.ftl");
        return message;
    }
}
