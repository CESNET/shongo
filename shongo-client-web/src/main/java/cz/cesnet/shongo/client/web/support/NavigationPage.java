package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.TodoImplementException;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node in navigation tree.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NavigationPage extends Page
{
    /**
     * URL transformed to regex pattern which can match URLs with filled attributes.
     */
    private Pattern urlPattern;

    /**
     * List of URL literals (text parts which are between attributes and in the start and in the end)
     */
    private List<String> urlLiterals;

    /**
     * List of URL attribute names in specified order.
     */
    private List<String> urlAttributes;

    /**
     * Parent {@link NavigationPage}.
     */
    private NavigationPage parentNavigationPage;

    /**
     * Child {@link NavigationPage}s.
     */
    private List<NavigationPage> childNavigationPages = new LinkedList<NavigationPage>();

    /**
     * Constructor.
     *
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     */
    public NavigationPage(String url, String titleCode)
    {
        super(url, titleCode);
    }

    /**
     * Constructor.
     *
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     * @param titleArguments sets the {@link #titleArguments}
     */
    public NavigationPage(String url, String titleCode, Object[] titleArguments)
    {
        super(url, titleCode, titleArguments);
    }

    /**
     * @param childNode to be added to the {@link #childNavigationPages}
     * @return {@code childNode}
     */
    public NavigationPage addChildNode(NavigationPage childNode)
    {
        childNavigationPages.add(childNode);
        childNode.parentNavigationPage = this;
        return childNode;
    }

    /**
     * @param attributes to be filled to URL
     * @return {@link #url} with filled attributes
     */
    public String getUrl(Map<String, String> attributes)
    {
        if (attributes == null || url == null) {
            return url;
        }
        if (urlAttributes == null) {
            buildUrl();
            if (urlAttributes == null) {
                throw new IllegalStateException("Url attributes should be initialized.");
            }
        }
        StringBuilder urlBuilder = new StringBuilder();
        Iterator<String> urlLiteralIterator = urlLiterals.iterator();
        Iterator<String> urlAttributeIterator = urlAttributes.iterator();
        urlBuilder.append(urlLiteralIterator.next());
        while (urlAttributeIterator.hasNext()) {
            String attributeName = urlAttributeIterator.next();
            String attributeValue = attributes.get(attributeName);
            if (attributeValue == null) {
                throw new RuntimeException("Attribute " + attributeName + " doesn't exist.");
            }
            urlBuilder.append(attributeValue);
            urlBuilder.append(urlLiteralIterator.next());
        }
        return urlBuilder.toString();
    }

    /**
     * @return {@link #parentNavigationPage}
     */
    public NavigationPage getParentNavigationPage()
    {
        return parentNavigationPage;
    }

    /**
     * @return {@link #childNavigationPages}
     */
    public List<NavigationPage> getChildNavigationPages()
    {
        return childNavigationPages;
    }

    /**
     * Find a {@link NavigationPage} in this and all child {@link NavigationPage}s (recursive)
     * which matches given {@code url}.
     *
     * @param url which the node should match
     * @return {@link NavigationPage} which matches given {@code url}
     */
    public NavigationPage findByUrl(String url)
    {
        boolean startsWith = this.url != null && url.startsWith(this.url);
        // If specified url matches this url, return this
        if (startsWith && url.length() == this.url.length()) {
            return this;
        }
        // If url starts with this url and none children are present, return this
        else if (startsWith && childNavigationPages.size() == 0) {
            return this;
        }
        // Else find matching child
        else {
            for (NavigationPage childNode : childNavigationPages) {
                NavigationPage navigationPage = childNode.findByUrl(url);
                if (navigationPage != null) {
                    return navigationPage;
                }
            }
        }
        return null;
    }

    /**
     * @param requestUrl    from which should be attributes parsed and which must match the {@link #url}
     * @param failUnmatched specifies whether exception should be thrown when given {@code requestUrl}
     *                      doesn't match the {@link #urlPattern}
     * @return attributes parsed from given {@code requestUrl} by the {@link #url} definition
     */
    public Map<String, String> parseUrlAttributes(String requestUrl, boolean failUnmatched)
    {
        if (urlPattern == null) {
            buildUrl();
        }
        Matcher matcher = urlPattern.matcher(requestUrl);
        if (matcher.find()) {
            if (matcher.groupCount() != urlAttributes.size()) {
                throw new RuntimeException("Pattern " + urlPattern + " should match " + urlAttributes.size() +
                        " in " + requestUrl + " but " + matcher.groupCount() + " has been matched.");
            }
            Map<String, String> attributes = new HashMap<String, String>();
            for (String attribute : urlAttributes) {
                String value = matcher.group(attributes.size() + 1);
                attributes.put(attribute, value);
            }
            return attributes;
        }
        else if (failUnmatched) {
            throw new RuntimeException("Pattern " + urlPattern + " should match " + requestUrl);
        }
        return null;
    }

    /**
     * @param requestUrl
     * @return true whether {@link #urlPattern} matches given {@code requestUrl}
     */
    public boolean matchesUrl(String requestUrl)
    {
        if (urlPattern == null) {
            buildUrl();
        }
        return urlPattern.matcher(requestUrl).find();
    }

    /**
     * Pattern for matching single url attribute. The attribute name is captured into group(1).
     */
    private static Pattern URL_ATTRIBUTE_PATTERN = Pattern.compile("\\{([^:]+)[^\\}]*\\}");

    /**
     * Build {@link #url} into {@link #urlPattern}, {@link #urlLiterals} and {@link #urlAttributes}.
     */
    private void buildUrl()
    {
        if (urlPattern == null) {
            String url = this.url;
            StringBuilder urlPatternBuilder = new StringBuilder();
            urlLiterals = new LinkedList<String>();
            urlAttributes = new LinkedList<String>();
            if (url != null) {
                Matcher matcher = URL_ATTRIBUTE_PATTERN.matcher(url);
                while (matcher.find()) {
                    MatchResult matchResult = matcher.toMatchResult();
                    if (matchResult.start() > 0) {
                        String urlLiteral = url.substring(0, matchResult.start());
                        urlPatternBuilder.append(Pattern.quote(urlLiteral));
                        urlLiterals.add(urlLiteral);
                    }
                    urlPatternBuilder.append("([^/?]+)");
                    String attribute = matchResult.group(1);
                    urlAttributes.add(attribute);
                    url = url.substring(matchResult.end());
                    matcher.reset(url);
                }
                urlPatternBuilder.append(Pattern.quote(url));
                urlLiterals.add(url);
            }
            urlPattern = Pattern.compile(urlPatternBuilder.toString());
        }
    }
}
