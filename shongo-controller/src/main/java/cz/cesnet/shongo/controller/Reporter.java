package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Handler for internal errors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Reporter
{
    private static Logger logger = LoggerFactory.getLogger(Reporter.class);

    /**
     * Specifies whether {@link #reportInternalError} should throw the error as {@link RuntimeException}s.
     */
    private static boolean throwInternalErrorsForTesting = false;

    /**
     * @param throwInternalErrorsForTesting sets the {@link #throwInternalErrorsForTesting}
     */
    public static void setThrowInternalErrorsForTesting(boolean throwInternalErrorsForTesting)
    {
        Reporter.throwInternalErrorsForTesting = throwInternalErrorsForTesting;
    }

    /**
     * Report given {@code report}.
     *
     * @param reportContext in which the {@code report} has been created
     * @param report to be reported
     */
    public static void report(ReportContext reportContext, AbstractReport report)
    {
        report(reportContext, report, null);
    }

    /**
     * Report given {@code report}.
     *
     * @param reportContext in which the {@code report} has been created
     * @param report    to be reported
     * @param throwable to be reported with the {@code report}
     */
    public static void report(ReportContext reportContext, AbstractReport report, Throwable throwable)
    {
        String name = report.getName();
        if (reportContext != null) {
            name = reportContext.getReportContextName() + ": " + name;
        }
        String domainAdminMessage = report.getMessage(Report.UserType.DOMAIN_ADMIN, Report.Language.ENGLISH);
        if (report.getType().equals(AbstractReport.Type.ERROR)) {
            logger.error(name + ": " + domainAdminMessage, throwable);
        }
        else if (report.getType().equals(AbstractReport.Type.WARNING)) {
            logger.warn(name + ": " + domainAdminMessage, throwable);
        }
        else if (report.getType().equals(AbstractReport.Type.INFORMATION)) {
            logger.info(name + ": " + domainAdminMessage, throwable);
        }
        else {
            logger.debug(name + ": " + domainAdminMessage, throwable);
        }

        if (report.isVisible(AbstractReport.VISIBLE_TO_DOMAIN_ADMIN) || report.isVisible(AbstractReport.VISIBLE_TO_RESOURCE_ADMIN)) {
            Authorization authorization = Authorization.getInstance();
            Controller controller = Controller.getInstance();
            EntityManager entityManager = controller.getEntityManagerFactory().createEntityManager();
            try {
                // Get resource which is referenced by report
                Resource resource = null;
                if (report instanceof ResourceReport) {
                    ResourceReport resourceReport = (ResourceReport) report;
                    try {
                        ObjectIdentifier resourceId =
                                ObjectIdentifier.parse(resourceReport.getResourceId(), ObjectType.RESOURCE);
                        resource = entityManager.find(Resource.class, resourceId.getPersistenceId());
                    }
                    catch (Exception exception) {
                        logger.error("Failed to get resource " + resourceReport.getResourceId() + ".", exception);
                    }
                }
                else if (reportContext instanceof ResourceContext) {
                    ResourceContext resourceContext = (ResourceContext) reportContext;
                    resource = resourceContext.getResource();
                }

                Set<String> administratorEmails = new HashSet<String>();
                if (report.isVisible(AbstractReport.VISIBLE_TO_DOMAIN_ADMIN)) {
                    ControllerConfiguration configuration = controller.getConfiguration();
                    administratorEmails.addAll(configuration.getAdministratorEmails(entityManager, authorization));
                    sendReportEmail(administratorEmails, name,
                            getAdministratorEmailContent(domainAdminMessage, reportContext, resource, throwable));
                }

                if (report.isVisible(AbstractReport.VISIBLE_TO_RESOURCE_ADMIN) && resource != null) {
                    Set<String> resourceAdministratorEmails = new HashSet<String>();
                    for (PersonInformation resourceAdmin : resource.getAdministrators(entityManager, authorization)) {
                        String administratorEmail = resourceAdmin.getPrimaryEmail();
                        if (!administratorEmails.contains(administratorEmail)) {
                            resourceAdministratorEmails.add(administratorEmail);
                        }
                    }
                    if (resourceAdministratorEmails.size() > 0) {
                        String resourceAdminMessage = report.getMessage(
                                Report.UserType.RESOURCE_ADMIN, Report.Language.ENGLISH);
                        sendReportEmail(resourceAdministratorEmails, name,
                                getAdministratorEmailContent(resourceAdminMessage, reportContext, resource, throwable));
                    }
                }
            }
            finally {
                entityManager.close();
            }
        }
    }

    /**
     * Report given {@code apiFault},
     *
     * @param reportContext in which the {@code apiFault} has been created
     * @param apiFault      to be reported
     * @param throwable     to be reported with the {@code apiFault}
     */
    public static void reportApiFault(ReportContext reportContext, ApiFault apiFault, Throwable throwable)
    {
        // Get report for the API fault
        AbstractReport apiFaultReport;
        if (apiFault instanceof AbstractReport) {
            apiFaultReport = (AbstractReport) apiFault;
        }
        else if (apiFault instanceof ReportException) {
            ReportException reportException = (ReportException) apiFault;
            apiFaultReport = reportException.getReport();
        }
        else if (apiFault instanceof ReportRuntimeException) {
            ReportRuntimeException reportRuntimeException = (ReportRuntimeException) apiFault;
            apiFaultReport = reportRuntimeException.getReport();
        }
        else {
            apiFaultReport = new CommonReportSet.UnknownErrorReport(apiFault.getFaultString());
        }
        // Report it
        report(reportContext, apiFaultReport, throwable);
    }

    /**
     * Report internal error.
     *
     * @param reportContext
     * @param message
     * @param throwable
     */
    public static void reportInternalError(ReportContext reportContext, String message, Throwable throwable)
    {
        StringBuilder nameBuilder = new StringBuilder();
        if (reportContext != null) {
            nameBuilder.append(reportContext.getReportContextName());
            nameBuilder.append(" Internal Error");
        }
        if (nameBuilder.length() == 0) {
            nameBuilder.append("Unknown Internal Error");
        }
        String name = nameBuilder.toString();
        if (message != null) {
            logger.error(name + ": " + message, throwable);
        }
        else {
            logger.error(name, throwable);
        }
        if (Reporter.throwInternalErrorsForTesting) {
            throw new RuntimeException(throwable);
        }
        else {
            Authorization authorization = Authorization.getInstance();
            Controller controller = Controller.getInstance();
            EntityManager entityManager = controller.getEntityManagerFactory().createEntityManager();
            try {
                ControllerConfiguration configuration = controller.getConfiguration();
                sendReportEmail(configuration.getAdministratorEmails(entityManager, authorization), name,
                        getAdministratorEmailContent(message, reportContext, null, throwable));
            }
            finally {
                entityManager.close();
            }
        }
    }

    /**
     * Report internal error.
     *
     * @param reportContext
     * @param exception
     */
    public static void reportInternalError(ReportContext reportContext, Exception exception)
    {
        reportInternalError(reportContext, null, exception);
    }

    /**
     * Report error.
     *
     * @param title
     * @param content
     */
    private static void sendReportEmail(Collection<String> recipients, String title, String content)
    {
        if (!Controller.hasInstance()) {
            logger.warn("Cannot send email because controller doesn't exist.");
            return;
        }
        EmailSender emailSender = Controller.getInstance().getEmailSender();

        // If error email can't be sent, propagate runtime exception
        if (!emailSender.isInitialized()) {
            logger.warn("Email report can't be sent because email sender is not initialized.");
            return;
        }
        if (recipients.size() == 0) {
            logger.warn("Email report can't be sent because no recipients are specified.");
            return;
        }

        // Send error email to administrators
        try {
            emailSender.sendEmail(new EmailSender.Email(recipients, title, content));
        }
        catch (Exception exception) {
            logger.error("Failed sending report email.", exception);
        }
    }

    /**
     * @param reportContext
     * @param message
     * @param throwable
     * @return email content
     */
    private static String getAdministratorEmailContent(String message, ReportContext reportContext,
            Resource resource, Throwable throwable)
    {
        // Prepare error email content
        StringBuilder emailContent = new StringBuilder();
        if (message != null) {
            emailContent.append(message);
        }
        if (throwable != null) {
            emailContent.append("\n\nEXCEPTION\n\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            throwable.printStackTrace(printWriter);
            String stackTrace = result.toString();
            emailContent.append(stackTrace);
        }
        String reportDetail = (reportContext != null ? reportContext.getReportContextDetail() : null);
        if (reportDetail != null) {
            emailContent.append("\n\n");
            emailContent.append(reportDetail);
        }
        if (resource != null) {
            emailContent.append("\n\nRESOURCE\n\n");
            emailContent.append(" Identifier: ");
            emailContent.append(new ObjectIdentifier(resource).toId());
            emailContent.append("\n");
            emailContent.append("       Name: ");
            emailContent.append(resource.getName());
            emailContent.append("\n");
        }
        emailContent.append("\n\n");
        emailContent.append(getConfiguration());
        return emailContent.toString();
    }

    /**
     * @return string describing controller configuration
     */
    private static String getConfiguration()
    {
        StringBuilder configuration = new StringBuilder();
        configuration.append("CONFIGURATION\n\n");

        Domain domain = Domain.getLocalDomain();
        configuration.append("  Domain: ")
                .append(domain.getName())
                .append(" (")
                .append(domain.getOrganization())
                .append(")\n");

        String hostName = null;
        if (Controller.hasInstance()) {
            hostName = Controller.getInstance().getRpcHost();
        }
        if (hostName == null || hostName.isEmpty()) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException unknownHostException) {
                logger.error("Cannot get local hostname.", unknownHostException);
            }
        }
        configuration.append("    Host: ")
                .append(hostName)
                .append("\n");

        return configuration.toString();
    }

    public static final ReportContext WORKER = new ReportContext()
    {
        @Override
        public String getReportContextName()
        {
            return "Worker";
        }

        @Override
        public String getReportContextDetail()
        {
            return null;
        }
    };
    public static final ReportContext PREPROCESSOR = new ReportContext()
    {
        @Override
        public String getReportContextName()
        {
            return "Preprocessor";
        }

        @Override
        public String getReportContextDetail()
        {
            return null;
        }
    };
    public static final ReportContext SCHEDULER = new ReportContext()
    {
        @Override
        public String getReportContextName()
        {
            return "Scheduler";
        }

        @Override
        public String getReportContextDetail()
        {
            return null;
        }
    };
    public static final ReportContext EXECUTOR = new ReportContext()
    {
        @Override
        public String getReportContextName()
        {
            return "Executor";
        }

        @Override
        public String getReportContextDetail()
        {
            return null;
        }
    };
    public static final ReportContext NOTIFICATION = new ReportContext()
    {
        @Override
        public String getReportContextName()
        {
            return "Notification";
        }

        @Override
        public String getReportContextDetail()
        {
            return null;
        }
    };

    /**
     * Represents an context in which a {@link cz.cesnet.shongo.report.AbstractReport} was created.
     */
    public static interface ReportContext
    {
        /**
         * @return name of the {@link cz.cesnet.shongo.controller.Reporter.ReportContext}
         */
        public String getReportContextName();

        /**
         * @return detailed description of the {@link cz.cesnet.shongo.controller.Reporter.ReportContext}
         */
        public String getReportContextDetail();
    }

    /**
     * Represents a {@link ReportContext} with {@link Resource}.
     */
    public static interface ResourceContext
    {
        /**
         * @return {@link Resource}
         */
        public Resource getResource();
    }
}
