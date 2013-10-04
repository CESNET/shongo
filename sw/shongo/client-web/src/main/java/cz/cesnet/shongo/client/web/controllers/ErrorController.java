package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.client.web.models.ReportModel;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import cz.cesnet.shongo.util.PasswordAuthenticator;
import net.tanesha.recaptcha.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Properties;

/**
 * Error controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"error", "report"})
public class ErrorController
{
    private static Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @Resource
    private ClientWebConfiguration configuration;

    @Resource
    private CommonService commonService;

    @Resource
    private ReCaptcha reCaptcha;

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.GET})
    public ModelAndView handleReport(HttpServletRequest request)
    {
        String requestUrl = BackUrl.getInstance(request).getUrl();
        ModelAndView modelAndView = new ModelAndView("report");
        modelAndView.addObject("report", new ReportModel(requestUrl, reCaptcha, commonService));
        return modelAndView;
    }

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.POST})
    public ModelAndView handleReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            return new ModelAndView("report");
        }
        else {
            boolean isSent = sendReport(reportModel, request);
            if (isSent) {
                sessionStatus.setComplete();
            }
            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSent", isSent);
            if (!isSent) {
                modelAndView.addObject("report", reportModel);
            }
            return modelAndView;
        }
    }

    /**
     * Handle error view.
     */
    @RequestMapping(value = "/error", method = {RequestMethod.GET})
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response)
    {
        response.setHeader("Content-Type", "text/html; charset=UTF-8");

        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

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
            Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            errorModel = new ErrorModel(requestUri, statusCode, message, throwable, request);
        }
        return handleError(errorModel, configuration, reCaptcha, commonService);
    }

    /**
     * Handle error report problem.
     */
    @RequestMapping(value = "/error", method = {RequestMethod.POST})
    public ModelAndView handleErrorReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            @ModelAttribute("error") ErrorModel errorModel,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("error");
            modelAndView.addObject("error", errorModel);
            modelAndView.addObject("report", reportModel);
            return modelAndView;
        }
        else {
            boolean isSent = sendReport(reportModel, request);
            if (isSent) {
                sessionStatus.setComplete();
            }
            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSent", isSent);
            if (!isSent) {
                modelAndView.addObject("report", reportModel);
            }
            return modelAndView;
        }
    }

    /**
     * Handle error not found.
     */
    @RequestMapping("/error-not-found")
    public String handleErrorNotFound()
    {
        return "errorNotFound";
    }

    /**
     * Handle login error view.
     */
    @RequestMapping("/login-error")
    public ModelAndView handleLoginErrorView(HttpServletRequest request)
    {
        // Update request url to login (we don't want to reference the login-error page itself anywhere)
        String requestUrl = (String) request.getAttribute(NavigationInterceptor.REQUEST_URL_REQUEST_ATTRIBUTE);
        requestUrl = requestUrl.replace("/login-error", ClientWebUrl.LOGIN);
        request.setAttribute(NavigationInterceptor.REQUEST_URL_REQUEST_ATTRIBUTE, requestUrl);

        Exception exception = (Exception) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception != null) {
            Throwable exceptionCause = exception.getCause();
            if (exceptionCause instanceof ControllerConnectException) {
                return new ModelAndView(handleControllerNotAvailableView());
            }
        }
        ErrorModel errorModel = new ErrorModel(request.getRequestURI(), null, "Login error", exception, request);
        return handleError(errorModel, configuration, reCaptcha, commonService);
    }

    /**
     * Handle controller not available view.
     */
    @RequestMapping(value = "/controller-not-available")
    public String handleControllerNotAvailableView()
    {
        return "controllerNotAvailable";
    }

    /**
     * Raise test error.
     */
    @RequestMapping(value = "/test-error")
    public String handleTestError()
    {
        throw new RuntimeException("Test error");
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.HOME + ".", exception);
        return "redirect:" + ClientWebUrl.HOME;
    }

    /**
     * Send report.
     *
     * @param reportModel
     * @param request
     */
    private boolean sendReport(ReportModel reportModel, HttpServletRequest request)
    {
        String emailSubject = reportModel.getEmailSubject();
        String emailContent = reportModel.getEmailContent(request);
        return sendEmailToAdministrator(emailSubject, emailContent, configuration);
    }

    /**
     * Handle error.
     *
     *
     *
     * @param errorModel
     * @param configuration
     * @param reCaptcha
     * @return error {@link ModelAndView}
     */
    public static ModelAndView handleError(ErrorModel errorModel, ClientWebConfiguration configuration,
            ReCaptcha reCaptcha, CommonService commonService)
    {
        String emailSubject = errorModel.getEmailSubject();
        logger.error(emailSubject, errorModel.getThrowable());
        sendEmailToAdministrator(emailSubject, errorModel.getEmailContent(), configuration);

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error", errorModel);
        modelAndView.addObject("report", new ReportModel(errorModel, reCaptcha, commonService));
        return modelAndView;
    }

    private static boolean sendEmailToAdministrator(String subject, String content, ClientWebConfiguration configuration)
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
            for (String administratorEmail : administratorEmails) {
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(administratorEmail));
            }
            mimeMessage.setSubject(subjectPrefix + subject);
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
}
