package cz.cesnet.shongo.client.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Navigation for shongo web client.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ClientWebNavigation
{
    // Site navigation
    HOME(null, ClientWebUrl.HOME,
            "navigation.home"),

    CHANGELOG(HOME, ClientWebUrl.CHANGELOG,
            "navigation.changelog"),
    WIZARD(HOME, ClientWebUrl.WIZARD,
            "navigation.wizard"),

    RESERVATION_REQUEST(HOME, ClientWebUrl.RESERVATION_REQUEST,
            "navigation.reservationRequest"),
    RESERVATION_REQUEST_LIST(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_LIST,
            null),
    RESERVATION_REQUEST_DETAIL(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_DETAIL,
            "navigation.reservationRequest.detail"),
    RESERVATION_REQUEST_ACL_CREATE(RESERVATION_REQUEST_DETAIL, ClientWebUrl.RESERVATION_REQUEST_ACL_CREATE,
            "navigation.reservationRequest.detail.createAcl"),
    RESERVATION_REQUEST_ACL_DELETE(RESERVATION_REQUEST_DETAIL, ClientWebUrl.RESERVATION_REQUEST_ACL_DELETE,
            "navigation.reservationRequest.detail.deleteAcl"),
    RESERVATION_REQUEST_CREATE(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_CREATE,
            "navigation.reservationRequest.create"),
    RESERVATION_REQUEST_MODIFY(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_MODIFY,
            "navigation.reservationRequest.modify"),
    RESERVATION_REQUEST_DELETE(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_DELETE,
            "navigation.reservationRequest.delete"),

    ROOM_MANAGEMENT(HOME, ClientWebUrl.ROOM_MANAGEMENT,
            "navigation.roomManagement");

    private final Page page;

    private ClientWebNavigation(ClientWebNavigation parentPage, String url, String titleCode)
    {
        page = new Page(url, titleCode);
        if (parentPage != null) {
            parentPage.page.addChildNode(page);
        }
    }

    public Page getPage()
    {
        return page;
    }

    private static Map<String, Page> cachedNavigationNodeByUrl = new HashMap<String, Page>();

    public static Page findByUrl(String url)
    {
        if (!cachedNavigationNodeByUrl.containsKey(url)) {
            Page page = HOME.page.findByUrl(url);
            cachedNavigationNodeByUrl.put(url, page);
        }
        return cachedNavigationNodeByUrl.get(url);
    }
}
