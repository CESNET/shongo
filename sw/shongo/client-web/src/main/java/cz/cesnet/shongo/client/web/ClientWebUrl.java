package cz.cesnet.shongo.client.web;

import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;

/**
 * Definition of URL constants in Shongo web client.
 */
public class ClientWebUrl
{
    public static final String HOME =
            "/";
    public static final String LOGIN =
            "/login";
    public static final String LOGOUT =
            "/logout";
    public static final String HELP =
            "/help";
    public static final String CHANGELOG =
            "/changelog";

    public static final String REPORT =
            "/report";

    public static final String WIZARD =
            "/wizard";
    public static final String WIZARD_ROOM =
            "/wizard/room";
    public static final String WIZARD_ROOM_ADHOC =
            "/wizard/room/adhoc";
    public static final String WIZARD_ROOM_PERMANENT =
            "/wizard/room/permanent";
    public static final String WIZARD_ROOM_DUPLICATE =
            "/wizard/room/{reservationRequestId:.+}/duplicate";
    public static final String WIZARD_ROOM_MODIFY =
            "/wizard/room/{reservationRequestId:.+}/modify";
    public static final String WIZARD_ROOM_ATTRIBUTES =
            "/wizard/room/attributes";
    public static final String WIZARD_ROOM_ROLES =
            "/wizard/room/roles";
    public static final String WIZARD_ROOM_PARTICIPANTS =
            "/wizard/room/participants";
    public static final String WIZARD_ROOM_PARTICIPANT_CREATE =
            "/wizard/room/participant/create";
    public static final String WIZARD_ROOM_PARTICIPANT_MODIFY =
            "/wizard/room/participant/{participantId}/modify";
    public static final String WIZARD_ROOM_PARTICIPANT_DELETE =
            "/wizard/room/participant/{participantId}/delete";
    public static final String WIZARD_ROOM_ROLE_CREATE =
            "/wizard/room/role/create";
    public static final String WIZARD_ROOM_ROLE_DELETE =
            "/wizard/room/role/{roleId}/delete";
    public static final String WIZARD_ROOM_CONFIRM =
            "/wizard/room/confirm";
    public static final String WIZARD_ROOM_CONFIRMED =
            "/wizard/create/confirmed";
    public static final String WIZARD_ROOM_CANCEL =
            "/wizard/room/cancel";
    public static final String WIZARD_ROOM_FINISH =
            "/wizard/room/finish";
    public static final String WIZARD_ROOM_FINISH_WITH_CAPACITY =
            "/wizard/room/finish?create-capacity=true";

    public static final String WIZARD_PERMANENT_ROOM_CAPACITY =
            "/wizard/permanent-room-capacity/create";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_DUPLICATE =
            "/wizard/permanent-room-capacity/{reservationRequestId:.+}/duplicate";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_MODIFY =
            "/wizard/permanent-room-capacity/{reservationRequestId:.+}/modify";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS =
            "/wizard/permanent-room-capacity/participants";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE =
            "/wizard/permanent-room-capacity/participant/create";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY =
            "/wizard/permanent-room-capacity/participant/{participantId}/modify";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE =
            "/wizard/permanent-room-capacity/participant/{participantId}/delete";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM =
            "/wizard/permanent-room-capacity/confirm";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRMED =
            "/wizard/permanent-room-capacity/confirmed";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_CANCEL =
            "/wizard/permanent-room-capacity/cancel";
    public static final String WIZARD_PERMANENT_ROOM_CAPACITY_FINISH =
            "/wizard/permanent-room-capacity/finish";

    public static final String WIZARD_DUPLICATE =
            "/wizard/{reservationRequestId:.+}/duplicate";
    public static final String WIZARD_MODIFY =
            "/wizard/{reservationRequestId:.+}/modify";
    public static final String WIZARD_UPDATE =
            "/wizard/update";

    public static final String DETAIL_VIEW = "/detail/{objectId:.+}";
    public static final String DETAIL_USER_ROLES_VIEW = DETAIL_VIEW + "?tab=userRoles";
    public static final String DETAIL_PARTICIPANTS_VIEW = DETAIL_VIEW + "?tab=participants";
    public static final String DETAIL_RUNTIME_MANAGEMENT_VIEW = DETAIL_VIEW + "?tab=runtimeManagement";

    public static final String DETAIL_RESERVATION_REQUEST_TAB =
            "/detail/{objectId:.+}/reservation-request";
    public static final String DETAIL_RESERVATION_REQUEST_STATE =
            "/detail/{objectId:.+}/reservation-request/state";
    public static final String DETAIL_RESERVATION_REQUEST_CHILDREN =
            "/detail/{objectId:.+}/reservation-request/children";
    public static final String DETAIL_RESERVATION_REQUEST_USAGES =
            "/detail/{objectId:.+}/reservation-request/usages";

    public static final String DETAIL_USER_ROLES_TAB =
            "/detail/{objectId:.+}/user-roles";
    public static final String DETAIL_USER_ROLES_DATA =
            "/detail/{objectId:.+}/user-roles/data";
    public static final String DETAIL_USER_ROLE_CREATE =
            "/detail/{objectId:.+}/user-role/create";
    public static final String DETAIL_USER_ROLE_DELETE =
            "/detail/{objectId:.+}/user-role/{roleId}/delete";

    public static final String DETAIL_PARTICIPANTS_TAB =
            "/detail/{objectId:.+}/participants";
    public static final String DETAIL_PARTICIPANTS_DATA =
            "/detail/{objectId:.+}/participants/data";
    public static final String DETAIL_PARTICIPANT_CREATE =
            "/detail/{objectId:.+}/participant/create";
    public static final String DETAIL_PARTICIPANT_MODIFY =
            "/detail/{objectId:.+}/participant/{participantId}/modify";
    public static final String DETAIL_PARTICIPANT_DELETE =
            "/detail/{objectId:.+}/participant/{participantId}/delete";

    public static final String DETAIL_RUNTIME_MANAGEMENT_TAB =
            "/detail/{objectId:.+}/runtime-management";
    public static final String DETAIL_RUNTIME_MANAGEMENT_MODIFY =
            "/detail/{objectId:.+}/runtime-management/modify";
    public static final String DETAIL_RUNTIME_MANAGEMENT_PARTICIPANTS_DATA =
            "/detail/{objectId:.+}/runtime-management/participants/data";
    public static final String DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_VIDEO_SNAPSHOT =
            "/detail/{objectId:.+}/runtime-management/participant/{participantId}/video-snapshot";
    public static final String DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_MODIFY =
            "/detail/{objectId:.+}/runtime-management/participant/{participantId}/modify";
    public static final String DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_DISCONNECT =
            "/detail/{objectId:.+}/runtime-management/participant/{participantId}/disconnect";
    public static final String DETAIL_RUNTIME_MANAGEMENT_RECORDING_START =
            "/detail/{objectId:.+}/runtime-management/recording/start";
    public static final String DETAIL_RUNTIME_MANAGEMENT_RECORDING_STOP =
            "/detail/{objectId:.+}/runtime-management/recording/stop";
    public static final String DETAIL_RUNTIME_MANAGEMENT_ENTER =
            "/detail/{objectId:.+}/runtime-management/enter";

    public static final String DETAIL_RECORDINGS_TAB =
            "/detail/{objectId:.+}/recordings";
    public static final String DETAIL_RECORDINGS_DATA =
            "/detail/{objectId:.+}/recordings/data";
    public static final String DETAIL_RECORDING_DELETE =
            "/detail/{objectId:.+}/recording/{resourceId:.+}/{recordingId:.+}/delete";

    public static final String RESERVATION_REQUEST =
            "/reservation-request";
    public static final String RESERVATION_REQUEST_LIST =
            "/reservation-request/list";
    public static final String RESERVATION_REQUEST_LIST_DATA =
            "/reservation-request/list/data";
    public static final String RESERVATION_REQUEST_REVERT =
            "/reservation-request/{reservationRequestId:.+}/revert";
    public static final String RESERVATION_REQUEST_DELETE =
            "/reservation-request/{reservationRequestId:.+}/delete";

    public static final String USER_SETTINGS =
            "/user/settings";
    public static final String USER_SETTINGS_ATTRIBUTE =
            USER_SETTINGS + "/{name}/{value}";
    public static final String USER_LIST_DATA =
            "/user/list/data";
    public static final String GROUP_LIST_DATA =
            "/group/list/data";

    public static final String ROOM_LIST_VIEW =
            "/room/list";
    public static final String ROOM_LIST_DATA =
            "/room/list/data";
    public static final String ROOM_DATA =
            "/room/{roomId:.+}/data";

    public static String format(String url, Object... variables)
    {
        for (Object variable : variables) {
            url = url.replaceFirst("\\{[^/]+\\}", variable != null ? variable.toString() : "");
        }
        return url;
    }

    public static String encodeUrlParam(String url)
    {
        try {
            return UriUtils.encodeQueryParam(url, "UTF-8");
        }
        catch (UnsupportedEncodingException exception) {
            throw new RuntimeException("Cannot encode URL to UTF-8.", exception);
        }
    }
}
