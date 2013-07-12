package cz.cesnet.shongo.client.web;

import java.util.HashMap;
import java.util.Map;

/**
 * Navigation for shongo web client.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebNavigation extends NavigationNode
{
    private Map<String, NavigationNode> cachedNavigationNodeByUrl = new HashMap<String, NavigationNode>();

    /**
     * Construct the navigation.
     */
    private ClientWebNavigation()
    {
        super(ClientWebUrl.HOME, "navigation.home");

        addChildNode(new NavigationNode(ClientWebUrl.DASHBOARD, "navigation.dashboard"));

        addChildNode(new NavigationNode(ClientWebUrl.WIZARD, "navigation.wizard"));

        NavigationNode reservationRequest = addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST, "navigation.reservationRequest"));
        reservationRequest.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_LIST));
        NavigationNode reservationRequestDetail = reservationRequest.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_DETAIL, "navigation.reservationRequest.detail", false));
        reservationRequest.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_CREATE, "navigation.reservationRequest.create"));
        reservationRequest.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_MODIFY, "navigation.reservationRequest.modify"));
        reservationRequest.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_DELETE, "navigation.reservationRequest.delete"));

        reservationRequestDetail.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_ACL_CREATE, "navigation.reservationRequest.detail.createAcl"));
        reservationRequestDetail.addChildNode(new NavigationNode(
                ClientWebUrl.RESERVATION_REQUEST_ACL_DELETE, "navigation.reservationRequest.detail.deleteAcl"));

        addChildNode(new NavigationNode(ClientWebUrl.CHANGELOG, "navigation.changelog"));
    }

    @Override
    public NavigationNode findByUrl(String url)
    {
        if (!cachedNavigationNodeByUrl.containsKey(url)) {
            NavigationNode navigationNode = super.findByUrl(url);
            cachedNavigationNodeByUrl.put(url, navigationNode);
        }
        return cachedNavigationNodeByUrl.get(url);
    }

    /**
     * Single instance of root {@link NavigationNode}.
     */
    private static ClientWebNavigation rootNavigationNode;

    /**
     * @return {@link #rootNavigationNode}
     */
    public static NavigationNode getInstance()
    {
        if (rootNavigationNode == null) {
            rootNavigationNode = new ClientWebNavigation();
        }
        return rootNavigationNode;
    }
}
