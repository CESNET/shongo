package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.resource.Resource;
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
     * Report given {@code report}.
     *
     * @param reportContext in which the {@code report} has been created
     * @param report to be reported
     */
    public static void report(ReportContext reportContext, Report report)
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
    public static void report(ReportContext reportContext, Report report, Throwable throwable)
    {
        String name = report.getName();
        if (reportContext != null) {
            name = reportContext.getReportName() + ": " + name;
        }
        String message = report.getMessage();

        if (report.getType().equals(Report.Type.ERROR)) {
            logger.error(name + ": " + message, throwable);
        }
        else if (report.getType().equals(Report.Type.WARNING)) {
            logger.warn(name + ": " + message, throwable);
        }
        else if (report.getType().equals(Report.Type.INFORMATION)) {
            logger.info(name + ": " + message, throwable);
        }
        else {
            logger.debug(name + ": " + message, throwable);
        }

        if (report.isVisibleToDomainAdminViaEmail() || report.isVisibleToResourceAdminViaEmail()) {
            // Get resource which is referenced by report
            Resource resource = null;
            EntityManager entityManager = null;
            if (report instanceof ResourceReport) {
                ResourceReport resourceReport = (ResourceReport) report;
                Controller controller = Controller.getInstance();
                entityManager = controller.getEntityManagerFactory().createEntityManager();
                try {
                    EntityIdentifier resourceId = EntityIdentifier.parse(
                            resourceReport.getResourceId(), EntityType.RESOURCE);
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

            String administratorEmailContent = getAdministratorEmailContent(message, reportContext, resource, throwable);

            Set<String> domainAdministratorEmails = new HashSet<String>();
            if (report.isVisibleToDomainAdminViaEmail()) {
                domainAdministratorEmails.addAll(getAdministratorEmails());
                sendReportEmail(domainAdministratorEmails, name, administratorEmailContent);
            }

            Set<String> resourceAdministratorEmails = new HashSet<String>();
            if (report.isVisibleToResourceAdminViaEmail() && resource != null) {
                for (Person resourceAdministrator : resource.getAdministrators()) {
                    String resourceAdministratorEmail = resourceAdministrator.getInformation().getPrimaryEmail();
                    if (!domainAdministratorEmails.contains(resourceAdministratorEmail)) {
                        resourceAdministratorEmails.add(resourceAdministratorEmail);
                    }
                }
                if (resourceAdministratorEmails.size() > 0) {
                    sendReportEmail(resourceAdministratorEmails, name, administratorEmailContent);
                }
            }
            if (entityManager != null) {
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
        Report apiFaultReport;
        if (apiFault instanceof Report) {
            apiFaultReport = (Report) apiFault;
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
            nameBuilder.append(reportContext.getReportName());
            nameBuilder.append(" Internal Error");
        }
        if (nameBuilder.length() == 0) {
            nameBuilder.append("Unknown Internal Error");
        }
        String name = nameBuilder.toString();
        logger.error(name + ": " + message, throwable);
        sendReportEmail(getAdministratorEmails(), name,
                getAdministratorEmailContent(message, reportContext, null, throwable));
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
            emailSender.sendEmail(recipients, title, content);
        }
        catch (MessagingException messagingException) {
            logger.error("Failed sending report email.", messagingException);
        }
    }

    /**
     * @return list of administrator email addresses
     */
    private static Set<String> getAdministratorEmails()
    {
        Set<String> administratorEmails = new HashSet<String>();
        Configuration configuration = Controller.getInstance().getConfiguration();
        for (Object item : configuration.getList(Configuration.ADMINISTRATOR_EMAIL)) {
            administratorEmails.add((String) item);
        }
        return administratorEmails;
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
        String reportDetail = (reportContext != null ? reportContext.getReportDetail() : null);
        if (reportDetail != null) {
            emailContent.append("\n\n");
            emailContent.append(reportDetail);
        }
        if (resource != null) {
            emailContent.append("\n\nRESOURCE\n\n");
            emailContent.append(" Identifier: ");
            emailContent.append(new EntityIdentifier(resource).toId());
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

        String hostName = Controller.getInstance().getRpcHost();
        if (hostName.isEmpty()) {
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

    public static final ReportContext AUTHORIZATION = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Authorization";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };
    public static final ReportContext WORKER = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Worker";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };
    public static final ReportContext PREPROCESSOR = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Preprocessor";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };
    public static final ReportContext SCHEDULER = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Scheduler";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };
    public static final ReportContext EXECUTOR = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Executor";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };
    public static final ReportContext NOTIFICATION = new ReportContext()
    {
        @Override
        public String getReportName()
        {
            return "Notification";
        }

        @Override
        public String getReportDetail()
        {
            return null;
        }
    };

    /**
     * Represents an context in which a {@link cz.cesnet.shongo.report.Report} was created.
     */
    public static interface ReportContext
    {
        /**
         * @return name of the {@link cz.cesnet.shongo.controller.Reporter.ReportContext}
         */
        public String getReportName();

        /**
         * @return detailed description of the {@link cz.cesnet.shongo.controller.Reporter.ReportContext}
         */
        public String getReportDetail();
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
