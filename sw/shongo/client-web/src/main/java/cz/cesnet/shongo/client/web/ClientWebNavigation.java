package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.support.NavigationPage;

import java.util.HashMap;
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
    USER_SETTINGS(HOME, ClientWebUrl.USER_SETTINGS,
            "navigation.userSettings"),
    REPORT(HOME, ClientWebUrl.REPORT,
            "navigation.report"),
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
    RESERVATION_REQUEST_CREATE_CONFIRM(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_CREATE_CONFIRM,
            "navigation.reservationRequest.create"),
    RESERVATION_REQUEST_MODIFY(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_MODIFY,
            "navigation.reservationRequest.modify"),
    RESERVATION_REQUEST_MODIFY_CONFIRM(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_MODIFY_CONFIRM,
            "navigation.reservationRequest.modify"),
    RESERVATION_REQUEST_DELETE(RESERVATION_REQUEST, ClientWebUrl.RESERVATION_REQUEST_DELETE,
            "navigation.reservationRequest.delete"),

    ROOM_MANAGEMENT(HOME, ClientWebUrl.ROOM_MANAGEMENT,
            "navigation.roomManagement");

    private final NavigationPage navigationPage;

    private ClientWebNavigation(ClientWebNavigation parentPage, String url, String titleCode)
    {
        navigationPage = new NavigationPage(url, titleCode);
        if (parentPage != null) {
            parentPage.navigationPage.addChildNode(navigationPage);
        }
    }

    public NavigationPage getNavigationPage()
    {
        return navigationPage;
    }

    public boolean isNavigationPage(NavigationPage navigationPage)
    {
        return navigationPage.equals(navigationPage);
    }

    private static Map<String, NavigationPage> cachedNavigationNodeByUrl = new HashMap<String, NavigationPage>();

    public static NavigationPage findByUrl(String url)
    {
        if (!cachedNavigationNodeByUrl.containsKey(url)) {
            NavigationPage navigationPage = HOME.navigationPage.findByUrl(url);
            cachedNavigationNodeByUrl.put(url, navigationPage);
        }
        return cachedNavigationNodeByUrl.get(url);
    }
}
