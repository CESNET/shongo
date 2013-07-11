package cz.cesnet.shongo.client.web;

/**
 * Navigation for shongo web client.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebNavigation
{
    /**
     * {@link ClientWebNavigation} cannot be instanced.
     */
    private ClientWebNavigation()
    {
    }

    /**
     * Single instance of root {@link NavigationNode}.
     */
    private static NavigationNode rootNavigationNode;

    /**
     * @return {@link #rootNavigationNode}
     */
    public static NavigationNode getInstance()
    {
        if (rootNavigationNode == null) {
            rootNavigationNode = createRootNavigationNode();
        }
        return rootNavigationNode;
    }

    /**
     * @return new instance of root {@link NavigationNode} for {@link ClientWebNavigation}
     */
    private static NavigationNode createRootNavigationNode()
    {
        NavigationNode rootNavigationNode = new NavigationNode(ClientWebUrl.HOME, "navigation.home");

        rootNavigationNode.addChildNode(
                new NavigationNode(ClientWebUrl.DASHBOARD, "navigation.dashboard"));

        rootNavigationNode.addChildNode(
                new NavigationNode(ClientWebUrl.WIZARD, "navigation.wizard"));

        NavigationNode reservationRequest = rootNavigationNode.addChildNode(
                new NavigationNode(ClientWebUrl.RESERVATION_REQUEST, "navigation.home"));
        reservationRequest.addChildNode(
                new NavigationNode(ClientWebUrl.RESERVATION_REQUEST_LIST, "navigation.reservationRequest.list"));
        reservationRequest.addChildNode(
                new NavigationNode(ClientWebUrl.RESERVATION_REQUEST_DETAIL, "navigation.reservationRequest.detail"));

        rootNavigationNode.addChildNode(
                new NavigationNode(ClientWebUrl.CHANGELOG, "navigation.changelog"));

        return rootNavigationNode;
    }
}
