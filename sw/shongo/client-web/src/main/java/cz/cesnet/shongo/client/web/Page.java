package cz.cesnet.shongo.client.web;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node in navigation tree.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Page
{
    /**
     * Node URL (with attributes).
     */
    private String url;

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
     * Node title message code for translation.
     */
    private String titleCode;

    /**
     * Parent {@link Page}.
     */
    private Page parentPage;

    /**
     * Child {@link Page}s.
     */
    private List<Page> childPages = new LinkedList<Page>();

    /**
     * Constructor.
     *
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     */
    public Page(String url, String titleCode)
    {
        this.url = url;
        this.titleCode = titleCode;
    }

    /**
     * @param childNode to be added to the {@link #childPages}
     * @return {@code childNode}
     */
    public Page addChildNode(Page childNode)
    {
        childPages.add(childNode);
        childNode.parentPage = this;
        return childNode;
    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param attributes to be filled to URL
     * @return {@link #url} with filled attributes
     */
    public String getUrl(Map<String, String> attributes)
    {
        if (urlAttributes == null) {
            buildUrl();
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
     * @return {@link #titleCode}
     */
    public String getTitleCode()
    {
        return titleCode;
    }

    /**
     * @return {@link #parentPage}
     */
    public Page getParentPage()
    {
        return parentPage;
    }

    /**
     * @return {@link #childPages}
     */
    public List<Page> getChildPages()
    {
        return childPages;
    }

    /**
     * Find a {@link Page} in this and all child {@link Page}s (recursive)
     * which matches given {@code url}.
     *
     * @param url which the node should match
     * @return {@link Page} which matches given {@code url}
     */
    public Page findByUrl(String url)
    {
        boolean startsWith = url.startsWith(this.url);
        // If specified url matches this url, return this
        if (startsWith && url.length() == this.url.length()) {
            return this;
        }
        // If url starts with this url and none children are present, return this
        else if (startsWith && childPages.size() == 0) {
            return this;
        }
        // Else find matching child
        else {
            for (Page childNode : childPages) {
                Page page = childNode.findByUrl(url);
                if (page != null) {
                    return page;
                }
            }
        }
        return null;
    }

    /**
     * @param requestUrl from which should be attributes parsed and which must match the {@link #url}
     * @return attributes parsed from given {@code requestUrl} by the {@link #url} definition
     */
    public Map<String, String> parseUrlAttributes(String requestUrl)
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
        else {
            throw new RuntimeException("Pattern " + urlPattern + " should match " + requestUrl);
        }
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
            urlPattern = Pattern.compile(urlPatternBuilder.toString());
        }
    }
}
