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

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
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
    public String handleErrorView(HttpServletRequest request, Model model)
    {
        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        String message = (String) request.getAttribute("javax.servlet.error.message");
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");

        reportError("Error " + statusCode + " in " + requestUri + ": " + message, throwable, request);

        if (throwable != null) {
            if (message != null) {
                message = message + ": " + throwable.getMessage();
            }
            else {
                message = throwable.getMessage();
            }
        }
        else if (message == null) {
            message = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        model.addAttribute("url", requestUri);
        model.addAttribute("code", statusCode);
        model.addAttribute("message", message);
        return "error";
    }

    /**
     * Handle login error view.
     */
    @RequestMapping("/login-error")
    public String handleLoginErrorView(HttpServletRequest request, Model model)
    {
        Exception exception = (Exception) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        reportError("Login error.", exception, request);

        model.addAttribute("exception", exception);
        return "errorLogin";
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
     * Report error.
     *
     * @param message
     * @param throwable
     * @param request
     */
    private void reportError(String message, Throwable throwable, HttpServletRequest request)
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
