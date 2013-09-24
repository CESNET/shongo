package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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
     * @param defaultUrl
     * @return current back URL or given {@code defaultUrl}
     */
    public String get(String defaultUrl)
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

    @Override
    public String toString()
    {
        return get(ClientWebUrl.HOME);
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
     *
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
     *
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
    private static class SessionData
    {
        /**
         * Session attribute in which the back URLs are stored.
         */
        public final static String BACK_URL_SESSION_ATTRIBUTE = "backUrl";

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
         * @param backUrl to be recorded as new back URL for given {@code requestUrl}
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
                for(String requestUrlToRemove : requestUrlsToRemove) {
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

        /**
         *
         * @param request
         * @return instance of {@link SessionData} for user session in given {@code request}
         */
        public static SessionData getInstance(HttpServletRequest request)
        {
            SessionData sessionData = (SessionData) WebUtils.getSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE);
            if (sessionData == null) {
                sessionData = new SessionData();
                WebUtils.setSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE, sessionData);
            }
            return sessionData;
        }
    }


}
