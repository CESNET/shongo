package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Represents a model for error which has occurred.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ErrorModel
{
    private String requestUri;

    private Integer statusCode;

    private String message;

    private Throwable throwable;

    private HttpServletRequest request;

    public ErrorModel(String requestUri, Integer statusCode, String message, Throwable throwable,
            HttpServletRequest request)
    {
        this.requestUri = requestUri;
        this.statusCode = statusCode;
        this.message = message;
        this.throwable = throwable;
        this.request = request;
    }

    public String getRequestUri()
    {
        return requestUri;
    }

    public Integer getStatusCode()
    {
        return statusCode;
    }

    public String getMessage()
    {
        return message;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public String getDescription()
    {
        StringBuilder messageDescription = new StringBuilder();
        if (throwable != null) {
            if (message != null) {
                messageDescription.append(message);
                messageDescription.append(": ");
                messageDescription.append(throwable.getMessage());
            }
            else {
                messageDescription.append(throwable.getMessage());
            }
        }
        else if (message == null && statusCode != null) {
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
            if (httpStatus != null) {
                messageDescription.append(httpStatus.getReasonPhrase());
            }
        }
        else if (message != null) {
            messageDescription.append(message);
        }
        return messageDescription.toString();
    }

    public String getContent()
    {
        StringBuilder content = new StringBuilder();
        if (message != null) {
            content.append(message);
        }
        if (throwable != null) {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append("EXCEPTION\n\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            throwable.printStackTrace(printWriter);
            String stackTrace = result.toString();
            content.append(stackTrace);
        }
        return content.toString();
    }

    public String getEmailSubject()
    {
        StringBuilder subjectBuilder = new StringBuilder();
        subjectBuilder.append("Error");
        if (statusCode != null) {
            subjectBuilder.append(" ");
            subjectBuilder.append(statusCode);
        }
        subjectBuilder.append(" in ");
        subjectBuilder.append(requestUri);
        if (message != null) {
            subjectBuilder.append(": ");
            subjectBuilder.append(message);
        }
        return subjectBuilder.toString();
    }

    public String getEmailContent()
    {
        StringBuilder content = new StringBuilder();
        content.append(getContent());

        content.append("\n\nCONFIGURATION\n\n");

        content.append("  URL: ");
        content.append(requestUri);
        content.append("?" + request.getQueryString());
        content.append("\n");

        // User
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OpenIDConnectAuthenticationToken) {
            UserInformation userInformation = ((OpenIDConnectAuthenticationToken) authentication).getPrincipal();
            content.append("  User: ");
            content.append(userInformation.getFullName());
            content.append(" (");
            content.append(userInformation.getPrimaryEmail());
            content.append(")");
            content.append("\n");
        }

        // User address
        content.append("  User-IP: ");
        content.append(request.getRemoteAddr());
        content.append("\n");

        // User agent
        content.append("  User-Agent: ");
        content.append(request.getHeader(HttpHeaders.USER_AGENT));
        content.append("\n");

        return content.toString();
    }
}
