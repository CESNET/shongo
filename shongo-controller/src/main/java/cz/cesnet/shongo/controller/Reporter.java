package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.report.*;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Reporter implements ReporterCache.EntryCallback
{
    private static Logger logger = LoggerFactory.getLogger(Reporter.class);

    /**
     * {@link cz.cesnet.shongo.controller.EmailSender} to be used for sending emails.
     */
    private EmailSender emailSender;

    /**
     * To be used.
     */
    private Controller controller;

    /**
     * Specifies whether {@link #reportInternalError} should throw the error as {@link RuntimeException}s.
     */
    private boolean throwInternalErrorsForTesting = false;

    /**
     * @see ReporterCache
     */
    private ReporterCache cache = new ReporterCache();

    /**
     * Constructor.
     *
     * @param emailSender sets the {@link #emailSender}
     */
    protected Reporter(EmailSender emailSender, Controller controller)
    {
        this.emailSender = emailSender;
        this.controller = controller;
    }

    /**
     * Destroy {@link Reporter}.
     */
    public void destroy()
    {
        // Reset single instance of reporter
        instance = null;
    }

    /**
     * @param throwInternalErrorsForTesting sets the {@link #throwInternalErrorsForTesting}
     */
    public void setThrowInternalErrorsForTesting(boolean throwInternalErrorsForTesting)
    {
        this.throwInternalErrorsForTesting = throwInternalErrorsForTesting;
    }

    /**
     * @param expiration to be set to the {@link #cache}
     */
    public void setCacheExpiration(Duration expiration)
    {
        cache.setExpiration(expiration);
    }

    /**
     * Clear expired {@link #cache}.
     *
     * @param dateTime
     */
    public void clearCache(DateTime dateTime)
    {
        cache.clear(dateTime, this);
    }

    /**
     * @return list of system administrator emails
     */
    protected List<String> getAdministratorEmails()
    {
        if (controller == null) {
            logger.warn("Cannot get list of administrators, because controller isn't specified...");
            return Collections.emptyList();
        }
        ControllerConfiguration configuration = controller.getConfiguration();
        return configuration.getAdministratorEmails();
    }

    /**
     * Report given {@code report}.
     *
     * @param reportContext in which the {@code report} has been created
     * @param report to be reported
     */
    public void report(ReportContext reportContext, AbstractReport report)
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
    public void report(ReportContext reportContext, AbstractReport report, Throwable throwable)
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

        if (report.isVisible(AbstractReport.VISIBLE_TO_DOMAIN_ADMIN)
                || report.isVisible(AbstractReport.VISIBLE_TO_RESOURCE_ADMIN)) {
            if (controller == null) {
                logger.warn("Cannot send administrator report, because controller isn't specified...");
                return;
            }
            Authorization authorization = controller.getAuthorization();
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
                    administratorEmails.addAll(configuration.getAdministratorEmails());
                    String content = getAdministratorEmailContent(domainAdminMessage, reportContext, resource, throwable);
                    sendReportEmail(administratorEmails, name, content, false);
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
                        String message = report.getMessage(Report.UserType.RESOURCE_ADMIN, Report.Language.ENGLISH);
                        String content = getAdministratorEmailContent(message, reportContext, resource, throwable);
                        sendReportEmail(resourceAdministratorEmails, name, content, false);
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
    public void reportApiFault(ReportContext reportContext, ApiFault apiFault, Throwable throwable)
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
    public void reportInternalError(ReportContext reportContext, String message, Throwable throwable)
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
        if (throwInternalErrorsForTesting) {
            throw new RuntimeException(throwable);
        }
        else {
            List<String> administratorEmails = getAdministratorEmails();
            if (!administratorEmails.isEmpty()) {
                String content = getAdministratorEmailContent(message, reportContext, null, throwable);
                sendReportEmail(administratorEmails, name, content, true);
            }
        }
    }

    /**
     * Report internal error.
     *
     * @param reportContext
     * @param exception
     */
    public void reportInternalError(ReportContext reportContext, Exception exception)
    {
        reportInternalError(reportContext, null, exception);
    }

    /**
     * @param reportContext
     * @param message
     * @param throwable
     * @return email content
     */
    private String getAdministratorEmailContent(String message, ReportContext reportContext,
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
    private String getConfiguration()
    {
        StringBuilder configuration = new StringBuilder();
        configuration.append("CONFIGURATION\n\n");

        LocalDomain localDomain = LocalDomain.getLocalDomain();
        configuration.append("  Domain: ")
                .append(localDomain.getName())
                .append(" (")
                .append(localDomain.getOrganization())
                .append(")\n");

        String hostName = null;
        if (controller != null) {
            hostName = controller.getRpcHost();
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

    /**
     * Report error.
     *
     * @param title
     * @param content
     */
    private void sendReportEmail(Collection<String> recipients, String title, String content, boolean cache)
    {
        // If error email can't be sent, propagate runtime exception
        if (!emailSender.isInitialized()) {
            logger.warn("Email report can't be sent because email sender is not initialized.");
            return;
        }
        if (recipients.size() == 0) {
            logger.warn("Email report can't be sent because no recipients are specified.");
            return;
        }

        int count = 1;
        if (cache) {
            count = this.cache.apply(recipients, title, content);
        }
        sendEmail(recipients, title, content, count);
    }

    @Override
    public void sendEmail(Collection<String> recipients, String title, String content, int count)
    {
        switch (count) {
            case 0:
                // Email should not be sent
                return;
            case 1:
                // Email should be sent normally
                break;
            default:
                // Email should be sent as multiple events
                title += " (" + count + "x)";
                break;
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
     * Current single instance of {@link Reporter}.
     */
    private static Reporter instance;

    /**
     * Constructor.
     *
     * @return {@link #instance}
     */
    public static Reporter create()
    {
        return create(new Reporter(new EmailSender(null, null), null));
    }

    /**
     * Constructor.
     *
     * @param controller
     * @return {@link #instance}
     */
    public static Reporter create(Controller controller)
    {
        return create(new Reporter(controller.getEmailSender(), controller));
    }

    /**
     * Constructor.
     *
     * @return {@link #instance}
     */
    public static Reporter create(EmailSender emailSender)
    {
        return create(new Reporter(emailSender, null));
    }

    /**
     * Constructor.
     *
     * @return {@link #instance}
     */
    public static Reporter create(Reporter reporter)
    {
        if (instance != null) {
            throw new IllegalStateException("Another instance of reporter already exists.");
        }
        instance = reporter;
        return instance;
    }

    /**
     * @return {@link #instance}
     */
    public static Reporter getInstance()
    {
        if (instance == null) {
            throw new RuntimeException("No reporter was initialized.");
        }
        return instance;
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
         * @return name of the {@link Reporter.ReportContext}
         */
        public String getReportContextName();

        /**
         * @return detailed description of the {@link Reporter.ReportContext}
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
