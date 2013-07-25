package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

/**
 * Error controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ErrorController
{
    private static Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @Resource
    private ClientWebConfiguration configuration;

    /**
     * Handle error view.
     */
    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request)
    {
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return handleError(requestUri, statusCode, message, throwable, request, configuration);
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
    public ModelAndView handleLoginErrorView(HttpServletRequest request, Model model)
    {
        Exception exception = (Exception) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        return handleError(request.getRequestURI(), null, "Login error.", exception,
                request, configuration);
    }

    /**
     * Handle controller not available view.
     */
    @RequestMapping(value = "/controller-not-available")
    public String handleControllerNotAvailableView()
    {
        return "controllerNotAvailable";
    }

    public static ModelAndView handleError(String requestUri, Integer statusCode, String message, Throwable throwable,
            HttpServletRequest request, ClientWebConfiguration configuration)
    {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Error");
        if (statusCode != null) {
            messageBuilder.append(" ");
            messageBuilder.append(statusCode);
        }
        messageBuilder.append(" in ");
        messageBuilder.append(requestUri);
        if (message != null) {
            messageBuilder.append(": ");
            messageBuilder.append(message);
        }
        reportError(messageBuilder.toString(), throwable, request, configuration);

        if (throwable != null) {
            if (message != null) {
                message = message + ": " + throwable.getMessage();
            }
            else {
                message = throwable.getMessage();
            }
        }
        else if (message == null && statusCode != null) {
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
            if (httpStatus != null) {
                message = httpStatus.getReasonPhrase();
            }
        }
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("url", requestUri);
        modelAndView.addObject("code", statusCode);
        modelAndView.addObject("message", message);
        return modelAndView;
    }

    /**
     * Report error.
     *
     * @param message
     * @param throwable
     * @param request
     */
    private static void reportError(String message, Throwable throwable, HttpServletRequest request,
            ClientWebConfiguration configuration)
    {
        logger.error(message, throwable);

        String administratorEmail = configuration.getAdministratorEmail();
        if (Strings.isNullOrEmpty(administratorEmail)) {
            logger.warn("Administrator email for sending error reports is not configured.");
            return;
        }
        if (Strings.isNullOrEmpty(configuration.getSmtpHost())) {
            logger.warn("SMTP host for sending error reports is not configured.");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getSmtpHost());
        properties.setProperty("mail.smtp.port", configuration.getSmtpPort());
        if (!configuration.getSmtpPort().equals("25")) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }
        Session session = Session.getDefaultInstance(properties);

        StringBuilder content = new StringBuilder();
        if (message != null) {
            content.append(message);
        }
        if (throwable != null) {
            content.append("\n\nEXCEPTION\n\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            throwable.printStackTrace(printWriter);
            String stackTrace = result.toString();
            content.append(stackTrace);
        }

        content.append("\n\nCONFIGURATION\n\n");
        content.append("  User-Agent: ");
        content.append(request.getHeader(HttpHeaders.USER_AGENT));
        content.append("\n");

        String sender = configuration.getSmtpSender();
        String subjectPrefix = configuration.getSmtpSubjectPrefix();
        try {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content.toString(), "text/plain; charset=utf-8");

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
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(administratorEmail));
            mimeMessage.setSubject(subjectPrefix + message);
            mimeMessage.setContent(multipart);

            logger.debug("Sending error report from '{}' to '{}'...", new Object[]{sender, administratorEmail});
            Transport.send(mimeMessage);
        }
        catch (MessagingException exception) {
            logger.error("Failed to send error report.", exception);
        }
    }
}
