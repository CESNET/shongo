package cz.cesnet.shongo.client.web;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.auth.AjaxRequestMatcher;
import cz.cesnet.shongo.client.web.auth.AuthorizationCodeExpiredException;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.client.web.models.ReportModel;
import cz.cesnet.shongo.client.web.models.UserSession;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.ApiFaultException;
import cz.cesnet.shongo.report.ReportRuntimeException;
import cz.cesnet.shongo.util.PasswordAuthenticator;
import net.tanesha.recaptcha.ReCaptcha;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Error handler for {@link cz.cesnet.shongo.client.web.ClientWeb}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ErrorHandler
{
    private static Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    private static final Pattern OBJECT_INACCESSIBLE_PATTERN =
            Pattern.compile("read .+ (shongo(:[-\\.a-zA-Z0-9]+)+:[0-9]+)");

    @Resource
    private ClientWebConfiguration configuration;

    @Resource
    private CommonService commonService;

    @Resource
    private ReCaptcha reCaptcha;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle error.
     *
     * @param request
     * @param response
     * @param requestUri
     * @param statusCode
     * @param message
     * @param throwable
     * @return {@link ModelAndView}
     */
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response,
            String requestUri, Integer statusCode, String message, Throwable throwable)
    {
        // Prepare response
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setStatus((statusCode != null ? statusCode : HttpServletResponse.SC_INTERNAL_SERVER_ERROR));

        if (throwable instanceof org.eclipse.jetty.io.EofException) {
            // Just log that exceptions and do not report it
            logger.warn("Not reported exception.", throwable);
            return null;
        }

        Throwable cause = throwable;
        ModelAndView modelAndView = null;
        while (cause != null) {
            ModelAndView causeModelAndView = handleCause(request, response, cause, message, modelAndView);
            if (causeModelAndView != null) {
                modelAndView = causeModelAndView;
            }
            if (cause instanceof ApiFaultException) {
                throwable = cause;
            }
            cause = cause.getCause();
        }
        if (modelAndView != null) {
            return modelAndView;
        }

        ErrorModel errorModel = null;
        if (requestUri == null) {
            Object error = WebUtils.getSessionAttribute(request, "error");
            if (error instanceof ErrorModel) {
                errorModel = (ErrorModel) error;
            }
            else {
                requestUri = "unknown";
                if (message == null) {
                    message = "unknown";
                }
            }
        }
        if (errorModel == null) {
            errorModel = new ErrorModel(requestUri, statusCode, message, throwable, request);
        }

        // Do not report AJAX 401 errors
        if (AjaxRequestMatcher.isAjaxRequest(request) &&
                Integer.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(errorModel.getStatusCode())) {
            logger.warn("AJAX ", errorModel.getEmailSubject(), errorModel.getThrowable());
            return null;
        }
        else {
            modelAndView = handleErrorView(errorModel, reCaptcha, commonService);
            HttpSession httpSession = request.getSession();
            for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
                httpSession.setAttribute(entry.getKey(), entry.getValue());
            }
            return modelAndView;
        }
    }

    /**
     * Handle error.
     *
     * @param errorModel
     * @param reCaptcha
     * @return error {@link ModelAndView}
     */
    public ModelAndView handleErrorView(ErrorModel errorModel, ReCaptcha reCaptcha, CommonService commonService)
    {
        ReportModel reportModel = new ReportModel(errorModel, reCaptcha, commonService);
        String emailReplyTo = reportModel.getEmail();
        String emailSubject = errorModel.getEmailSubject();
        logger.error(emailSubject, errorModel.getThrowable());
        sendEmailToAdministrator(emailReplyTo, emailSubject, errorModel.getEmailContent());

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error", errorModel);
        modelAndView.addObject("report", reportModel);
        return modelAndView;
    }

    /**
     * Send email to administrators.
     *
     * @param replyTo
     * @param subject
     * @param content
     * @return result
     */
    public boolean sendEmailToAdministrator(String replyTo, String subject, String content)
    {
        Collection<String> administratorEmails = configuration.getAdministratorEmails();
        if (administratorEmails.size() == 0) {
            logger.warn("Administrator email for sending error reports is not configured.");
            return false;
        }
        if (Strings.isNullOrEmpty(configuration.getSmtpHost())) {
            logger.warn("SMTP host for sending error reports is not configured.");
            return false;
        }

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getSmtpHost());
        properties.setProperty("mail.smtp.port", configuration.getSmtpPort());
        if (!configuration.getSmtpPort().equals("25")) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }

        Authenticator authenticator = null;
        String smtpUserName = configuration.getSmtpUserName();
        if (!Strings.isNullOrEmpty(smtpUserName)) {
            properties.setProperty("mail.smtp.auth", "true");
            authenticator = new PasswordAuthenticator(smtpUserName, configuration.getSmtpPassword());
        }

        Session session = Session.getDefaultInstance(properties, authenticator);

        String sender = configuration.getSmtpSender();
        String subjectPrefix = configuration.getSmtpSubjectPrefix();
        try {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content, "text/plain; charset=utf-8");

            StringBuilder html = new StringBuilder();
            html.append("<html><body><pre>");
            html.append(content);
            html.append("</pre></body></html>");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html.toString(), "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(sender));
            if (replyTo != null) {
                mimeMessage.setReplyTo(new Address[]{new InternetAddress(replyTo)});
            }
            for (String administratorEmail : administratorEmails) {
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(administratorEmail));
            }
            mimeMessage.setSubject(subjectPrefix + subject, "utf-8");
            mimeMessage.setContent(multipart);

            logger.debug("Sending email from '{}' to '{}'...", new Object[]{sender, administratorEmails});
            Transport.send(mimeMessage);
            return true;
        }
        catch (MessagingException exception) {
            logger.error("Failed to send email '" + subject + "':\n" + content, exception);
            return false;
        }
    }

    /**
     * Try to handle cause by {@link ModelAndView}.
     *
     * @param request
     * @param response
     * @param cause
     * @param message
     * @param oldModelAndView  already prepared ModelAndView for handling cause
     * @return {@link ModelAndView} or {@code null}
     */
    private ModelAndView handleCause(HttpServletRequest request, HttpServletResponse response, Throwable cause,
            String message, ModelAndView oldModelAndView)
    {
        if (cause instanceof UserMessageException) {
            UserMessageException userMessageException = (UserMessageException) cause;
            ModelAndView modelAndView = new ModelAndView("userMessage");
            modelAndView.addObject("titleCode", userMessageException.getTitleCode());
            modelAndView.addObject("messageCode", userMessageException.getMessageCode());
            return modelAndView;
        }
        else if (cause instanceof AuthenticationServiceException) {
            logger.warn("Authentication failed", cause);
            Locale locale = RequestContextUtils.getLocale(request);
            ModelAndView modelAndView = new ModelAndView("errorAuthentication");
            String reason = null;
            if (cause instanceof AuthorizationCodeExpiredException) {
                message = messageSource.getMessage("views.errorAuthentication.expired", null, locale);
            }
            else {
                message = messageSource.getMessage("views.errorAuthentication.unknown", null, locale);
                reason = cause.getMessage();
            }
            modelAndView.addObject("message", message);
            modelAndView.addObject("reason", reason);
            return modelAndView;
        }
        else if (cause instanceof ControllerConnectException) {
            return new ModelAndView("errorControllerNotAvailable");
        }
        else if (cause instanceof PageNotAuthorizedException) {
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return new ModelAndView("errorPageInaccessible");
        }
        else if (cause instanceof ControllerReportSet.SecurityNotAuthorizedException) {
            ControllerReportSet.SecurityNotAuthorizedException securityNotAuthorizedException =
                    (ControllerReportSet.SecurityNotAuthorizedException) cause;
            String action = securityNotAuthorizedException.getReport().getAction();
            Matcher objectInaccessibleMatcher = OBJECT_INACCESSIBLE_PATTERN.matcher(action);
            if (objectInaccessibleMatcher.find()) {
                ModelAndView modelAndView = new ModelAndView("errorObjectInaccessible");
                modelAndView.addObject("objectId", objectInaccessibleMatcher.group(1));
                return modelAndView;
            }
        }
        else if (cause instanceof ObjectInaccessibleException) {
            ObjectInaccessibleException objectInaccessibleException = (ObjectInaccessibleException) cause;
            ModelAndView modelAndView = new ModelAndView("errorObjectInaccessible");
            modelAndView.addObject("objectId", objectInaccessibleException.getObjectId());
            return modelAndView;
        }
        else if (cause instanceof ControllerReportSet.SecurityInvalidTokenException) {
            UserSession userSession = UserSession.getInstance(request);
            if (userSession.attemptLogin() && !AjaxRequestMatcher.isAjaxRequest(request)) {
                logger.warn("Invalid security token, redirecting to login page...", cause);
                return new ModelAndView("redirect:" + ClientWebUrl.LOGIN);
            }
            else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return handleCause(request, response, new AuthenticationServiceException(cause.getMessage()), message, oldModelAndView);
            }
        }
        // Log AJAX error (if the error wasn't handled in previous invocation of this method)
        else if (AjaxRequestMatcher.isAjaxRequest(request) && oldModelAndView == null) {
            String error;
            String description;
            if (cause instanceof ReportRuntimeException) {
                ReportRuntimeException reportException = (ReportRuntimeException) cause;
                AbstractReport report = reportException.getReport();
                error = report.getUniqueId();
                description = report.getMessage();
            }
            else {
                error = cause.getClass().getCanonicalName();
                description = cause.getMessage();
            }
            logger.warn("AJAX request failed", cause);
            ModelAndView modelAndView = new ModelAndView(new MappingJackson2JsonView());
            modelAndView.addObject("error", error);
            modelAndView.addObject("description", description);
            return modelAndView;
        }
        return null;
    }
}
