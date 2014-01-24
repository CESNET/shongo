package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AllocationStateReport;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Locale;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationFailedNotification extends AbstractReservationRequestNotification
{
    private UserInformation user;

    private Interval requestedSlot;

    private Target target;

    private AllocationStateReport.UserError userError;

    private AllocationStateReport adminReport;

    public AllocationFailedNotification(ReservationRequest reservationRequest,
            AuthorizationManager authorizationManager, ControllerConfiguration configuration)
    {
        super(reservationRequest, authorizationManager.getUserSettingsManager());

        EntityManager entityManager = authorizationManager.getEntityManager();

        this.user = authorizationManager.getUserInformation(getReservationRequestUpdatedBy());
        this.requestedSlot = reservationRequest.getSlot();
        this.target = Target.createInstance(reservationRequest, entityManager);
        if (this.target instanceof Target.Room) {
            // We must compute the final time slot
            Target.Room room = (Target.Room) this.target;
            this.requestedSlot = new Interval(this.requestedSlot.getStart().minus(room.getSlotBefore()),
                    this.requestedSlot.getEnd().plus(room.getSlotAfter()));
        }
        this.userError = reservationRequest.getAllocationStateReport(Report.UserType.USER).toUserError();
        this.adminReport = reservationRequest.getAllocationStateReport(Report.UserType.DOMAIN_ADMIN);
        for (PersonInformation administrator : configuration.getAdministrators()) {
            addRecipient(administrator, true);
        }
        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER)) {
            addReplyTo(authorizationManager.getUserInformation(userId));
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
    protected NotificationMessage renderMessage(Configuration configuration,
            NotificationManager manager)
    {
        Locale locale = configuration.getLocale();
        DateTimeZone timeZone = configuration.getTimeZone();
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);
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

        NotificationMessage message = renderTemplateMessage(
                renderContext, titleBuilder.toString(), "allocation-failed.ftl");
        return message;
    }

    @Override
    protected NotificationMessage renderMessage(PersonInformation recipient, NotificationManager manager)
    {
        NotificationMessage notificationMessage = super.renderMessage(recipient, manager);
        notificationMessage.appendTitleAfter("] ", "(" + user.getFullName() + ") ");
        return notificationMessage;
    }
}
