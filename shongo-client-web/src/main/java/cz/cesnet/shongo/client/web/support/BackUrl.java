package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReportModel;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Object holding back URLs for user session.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class BackUrl
{
    /**
     * Current back URL.
     */
    private String url;

    /**
     * Current {@link Breadcrumb}.
     */
    private Breadcrumb breadcrumb;

    /**
     * Constructor.
     *
     * @param url
     * @param breadcrumb
     */
    public BackUrl(String url, Breadcrumb breadcrumb)
    {
        this.url = url;
        this.breadcrumb = breadcrumb;
    }

    /**
     * @param url sets the {@link #url}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param defaultUrl
     * @return current back URL or breadcrumb back URL or given {@code defaultUrl}
     */
    public String getUrl(String defaultUrl)
    {
        if (url != null) {
            return url;
        }
        else {
            if (breadcrumb != null) {
                return breadcrumb.getBackUrl();
            }
            else {
                return defaultUrl;
            }
        }
    }

    /**
     * @param defaultUrl
     * @return current back URL or given {@code defaultUrl}
     */
    public String getUrlNoBreadcrumb(String defaultUrl)
    {
        if (url != null) {
            return url;
        }
        else {
            return defaultUrl;
        }
    }

    /**
     * @param url
     * @return url with applied back-url
     */
    public String applyToUrl(String url)
    {
        if (this.url == null) {
            return url;
        }
        if (url.contains("?")) {
            return url + "&back-url=" + ClientWebUrl.encodeUrlParam(this.url);
        }
        else {
            return url + "?back-url=" + ClientWebUrl.encodeUrlParam(this.url);
        }
    }

    @Override
    public String toString()
    {
        return getUrl(ClientWebUrl.HOME);
    }

    /**
     * @param request
     * @param backUrl to be recorded for given {@code request}
     */
    public static void addUrl(HttpServletRequest request, String backUrl)
    {
        SessionData sessionData = SessionData.getInstance(request);
        sessionData.pushBackUrl(request.getRequestURI(), backUrl);
    }

    /**
     * Apply back-url for current page to another page ({@code applyToUrl}).
     *
     * @param request
     * @param applyToUrl
     */
    public static void applyTo(HttpServletRequest request, String applyToUrl)
    {
        SessionData sessionData = SessionData.getInstance(request);
        String backUrl = sessionData.getBackUrl(request.getRequestURI());
        if (backUrl != null) {
            sessionData.pushBackUrl(applyToUrl, backUrl);
        }
    }

    /**
     * @param request
     * @param breadcrumb
     * @return {@link BackUrl} for given {@code request} and {@code breadcrumb}
     */
    public static BackUrl getInstance(HttpServletRequest request, Breadcrumb breadcrumb)
    {
        SessionData sessionData = SessionData.getInstance(request);
        return new BackUrl(sessionData.getBackUrl(request.getRequestURI()), breadcrumb);
    }

    /**
     * @param request
     * @param requestUrl
     * @return {@link BackUrl} for given {@code requestUrl}
     */
    public static BackUrl getInstance(HttpServletRequest request, String requestUrl)
    {
        SessionData sessionData = SessionData.getInstance(request);
        return new BackUrl(sessionData.getBackUrl(requestUrl), null);
    }

    /**
     * @param request
     * @return {@link BackUrl} which is set as attribute in given {@code request}
     */
    public static BackUrl getInstance(HttpServletRequest request)
    {
        BackUrl backUrl = (BackUrl) request.getAttribute(NavigationInterceptor.BACK_URL_REQUEST_ATTRIBUTE);
        if (backUrl == null) {
            throw new IllegalStateException("Back url doesn't exist.");
        }
        return backUrl;
    }

    /**
     * Back URL data for a session.
     */
    private static class SessionData implements ReportModel.ContextSerializable
    {
        /**
         * Session attribute in which the back URLs are stored.
         */
        public final static String BACK_URL_SESSION_ATTRIBUTE = "SHONGO_BACK_URL";

        /**
         * Map of back URL by request URL.
         */
        private Map<String, String> backUrlByRequestUrl = new HashMap<String, String>();

        /**
         * Map of request URLs by back URL.
         */
        private Map<String, Set<String>> requestUrlsByBackUrl = new HashMap<String, Set<String>>();

        /**
         * @param requestUrl for which the back URL should be recorded
         * @param backUrl    to be recorded as new back URL for given {@code requestUrl}
         */
        public synchronized void pushBackUrl(String requestUrl, String backUrl)
        {
            backUrlByRequestUrl.put(requestUrl, backUrl);

            int position = backUrl.indexOf("?");
            if (position != -1) {
                backUrl = backUrl.substring(0, position);
            }
            Set<String> requestUrls = requestUrlsByBackUrl.get(backUrl);
            if (requestUrls == null) {
                requestUrls = new HashSet<String>();
                requestUrlsByBackUrl.put(backUrl, requestUrls);
            }
            requestUrls.add(requestUrl);
        }

        /**
         * @param requestUrl
         * @return recorded back URL for given {@code requestUrl} or null
         */
        public synchronized String getBackUrl(String requestUrl)
        {
            Set<String> requestUrlsToRemove = requestUrlsByBackUrl.get(requestUrl);
            if (requestUrlsToRemove != null) {
                for (String requestUrlToRemove : requestUrlsToRemove) {
                    backUrlByRequestUrl.remove(requestUrlToRemove);
                }
            }
            return backUrlByRequestUrl.get(requestUrl);
        }

        /**
         * @param requestUrl to be removed
         */
        public synchronized void removeBackUrl(String requestUrl)
        {
            backUrlByRequestUrl.remove(requestUrl);
        }

        @Override
        public String toContextString()
        {
            StringBuilder contextBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : backUrlByRequestUrl.entrySet()) {
                if (entry.getKey().equals(ClientWebUrl.REPORT)) {
                    continue;
                }
                if (contextBuilder.length() > 0) {
                    contextBuilder.append("\n");
                }
                contextBuilder.append(entry.getKey());
                contextBuilder.append(" => ");
                contextBuilder.append(entry.getValue());
            }
            return contextBuilder.toString();
        }

        /**
         * @param request
         * @return instance of {@link SessionData} for user session in given {@code request}
         */
        public static SessionData getInstance(HttpServletRequest request)
        {
            synchronized (request) {
                SessionData sessionData = (SessionData) WebUtils.getSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE);
                if (sessionData == null) {
                    sessionData = new SessionData();
                    WebUtils.setSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE, sessionData);
                }
                return sessionData;
            }
        }
    }


}
