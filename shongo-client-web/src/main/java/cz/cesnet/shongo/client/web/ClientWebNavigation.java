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
    HELP(HOME, ClientWebUrl.HELP,
            "navigation.help"),
    DOCUMENTATION(HOME, ClientWebUrl.DOCUMENTATION,
            "navigation.doc"),
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
    RESERVATION_REQUEST_DELETE(HOME, ClientWebUrl.RESERVATION_REQUEST_DELETE,
            "navigation.reservationRequest.delete"),

    DETAIL(HOME, ClientWebUrl.DETAIL_VIEW,
            "navigation.detail"),
    DETAIL_USER_ROLE_CREATE(DETAIL, ClientWebUrl.DETAIL_USER_ROLE_CREATE,
            "navigation.detail.userRole.create"),
    DETAIL_USER_ROLE_DELETE(DETAIL, ClientWebUrl.DETAIL_USER_ROLE_DELETE,
            "navigation.detail.userRole.delete"),
    DETAIL_PARTICIPANT_CREATE(DETAIL,
            ClientWebUrl.DETAIL_PARTICIPANT_CREATE,
            "navigation.detail.participant.create"),
    DETAIL_PARTICIPANT_MODIFY(DETAIL,
            ClientWebUrl.DETAIL_PARTICIPANT_MODIFY,
            "navigation.detail.participant.modify"),
    DETAIL_PARTICIPANT_DELETE(DETAIL,
            ClientWebUrl.DETAIL_PARTICIPANT_DELETE,
            "navigation.detail.participant.delete"),

    RESOURCE_MANAGEMENT(HOME, null,
            "navigation.resourceManagement"),
    RESOURCE_RESERVATIONS(RESOURCE_MANAGEMENT, ClientWebUrl.RESOURCE_RESERVATIONS_VIEW,
            "navigation.resourceReservations"),
    RESOURCE_CAPACITY_UTILIZATION(RESOURCE_MANAGEMENT, ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION,
            "navigation.resourceCapacityUtilization"),

    RESOURCE_RESERVATIONS_CONFIRMATION(HOME, ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION,
                                  "navigation.resourceReservationsConfirmation");

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
