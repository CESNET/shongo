package cz.cesnet.shongo.client.web;

/**
 * Node in navigation tree.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NavigationNode
{
    private String url;

    private String titleCode;

    public NavigationNode(String url, String titleCode)
    {
        this.url = url;
        this.titleCode = titleCode;
    }

    public NavigationNode addChildNode(NavigationNode childNode)
    {
        return childNode;
    }

}
