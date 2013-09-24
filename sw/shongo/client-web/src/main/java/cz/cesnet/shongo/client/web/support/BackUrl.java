package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Object holding back URLs for user session.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class BackUrl
{
    public final static String BACK_URL_SESSION_ATTRIBUTE = "backUrl";

    /**
     * List of current back URLs.
     */
    private List<String> urls = new LinkedList<String>();

    /**
     * Last (current) back URL.
     */
    private String url;

    /**
     * @param url to be set as new back URL
     */
    public synchronized void add(String url)
    {
        if (!remove(url)) {
            this.urls.add(url);
            this.url = url;
        }
    }

    /**
     * @param url to be removed from back URLs
     */
    public synchronized boolean remove(String url)
    {
        int index = 0;
        for (String existingUrl : this.urls) {
            if (existingUrl.startsWith(url)) {
                break;
            }
            index++;
        }
        if (index < this.urls.size()) {
            List<String> retainedUrls = new ArrayList<String>(this.urls.subList(0, index));
            this.urls.clear();
            this.urls.addAll(retainedUrls);
            this.url = (retainedUrls.size() > 0 ? retainedUrls.get(index) : null);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * @param defaultUrl
     * @return current back URL or given {@code defaultUrl}
     */
    public synchronized String get(String defaultUrl)
    {
        if (url != null) {
            return url;
        }
        else {
            return defaultUrl;
        }
    }

    /**
     * @param breadcrumb
     * @param defaultUrl
     * @return current back URL or back URL from given {@code breadcrumb} or given {@code defaultUrl}
     */
    public String get(Breadcrumb breadcrumb, String defaultUrl)
    {
        if (breadcrumb != null) {
            defaultUrl = breadcrumb.getBackUrl();
        }
        return get(defaultUrl);
    }

    @Override
    public String toString()
    {
        return get(ClientWebUrl.HOME);
    }

    /**
     * @param request
     * @return instance of {@link BackUrl} for user session in given {@code request}
     */
    public static BackUrl getInstance(HttpServletRequest request)
    {
        BackUrl backUrl = (BackUrl) WebUtils.getSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE);
        if (backUrl == null) {
            backUrl = new BackUrl();
            WebUtils.setSessionAttribute(request, BACK_URL_SESSION_ATTRIBUTE, backUrl);
        }
        return backUrl;
    }
}
