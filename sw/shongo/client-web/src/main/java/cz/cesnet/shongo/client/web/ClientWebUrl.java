package cz.cesnet.shongo.client.web;

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
    public static final String CHANGELOG =
            "/changelog";

    public static final String WIZARD =
            "/wizard";
    public static final String WIZARD_SELECT =
            "/wizard/select";
    public static final String WIZARD_RESERVATION_REQUEST_LIST =
            "/wizard/reservation-request";
    public static final String WIZARD_RESERVATION_REQUEST_DETAIL =
            "/wizard/reservation-request/{reservationRequestId:.+}";
    public static final String WIZARD_CREATE_ROOM =
            "/wizard/create";
    public static final String WIZARD_CREATE_ADHOC_ROOM =
            "/wizard/create/adhoc-room";
    public static final String WIZARD_CREATE_PERMANENT_ROOM =
            "/wizard/create/permanent-room";
    public static final String WIZARD_CREATE_ROOM_ATTRIBUTES =
            "/wizard/create/attributes";
    public static final String WIZARD_CREATE_ROOM_ROLES =
            "/wizard/create/roles";
    public static final String WIZARD_CREATE_ROOM_ROLE_CREATE =
            "/wizard/create/role/create";
    public static final String WIZARD_CREATE_ROOM_ROLE_DELETE =
            "/wizard/create/role/{userRoleId:.+}/delete";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY =
            "/wizard/create/permanent-room-capacity";
    public static final String WIZARD_CREATE_CONFIRM =
            "/wizard/create/confirm";
    public static final String WIZARD_CREATE_FINISH =
            "/wizard/create/finish";

    public static final String RESERVATION_REQUEST =
            "/reservation-request";
    public static final String RESERVATION_REQUEST_LIST =
            "/reservation-request/list";
    public static final String RESERVATION_REQUEST_LIST_DATA =
            "/reservation-request/list/data";
    public static final String RESERVATION_REQUEST_CREATE =
            "/reservation-request/create";
    public static final String RESERVATION_REQUEST_CREATE_CONFIRM =
            "/reservation-request/create/confirm";
    public static final String RESERVATION_REQUEST_DETAIL =
            "/reservation-request/{reservationRequestId:.+}/detail";
    public static final String RESERVATION_REQUEST_DETAIL_CHILDREN =
            "/reservation-request/{reservationRequestId:.+}/detail/children";
    public static final String RESERVATION_REQUEST_DETAIL_USAGES =
            "/reservation-request/{reservationRequestId:.+}/detail/usages";
    public static final String RESERVATION_REQUEST_MODIFY =
            "/reservation-request/{reservationRequestId:.+}/modify";
    public static final String RESERVATION_REQUEST_MODIFY_CONFIRM =
            "/reservation-request/{reservationRequestId:.+}/modify/confirm";
    public static final String RESERVATION_REQUEST_DELETE =
            "/reservation-request/{reservationRequestId:.+}/delete";
    public static final String RESERVATION_REQUEST_DELETE_CONFIRM =
            "/reservation-request/{reservationRequestId:.+}/delete/confirm";
    public static final String RESERVATION_REQUEST_ACL =
            "/reservation-request/{reservationRequestId:.+}/acl";
    public static final String RESERVATION_REQUEST_ACL_CREATE =
            "/reservation-request/{reservationRequestId:.+}/acl/create";
    public static final String RESERVATION_REQUEST_ACL_CREATE_CONFIRM =
            "/reservation-request/{reservationRequestId:.+}/acl/create/confirm";
    public static final String RESERVATION_REQUEST_ACL_DELETE =
            "/reservation-request/{reservationRequestId:.+}/acl/{aclRecordId}/delete";

    public static final String USER =
            "/user";
    public static final String USER_LIST =
            "/user/list";
    public static final String USER_DETAIL =
            "/user/{userId:.+}";

    public static final String ROOMS_DATA =
            "/rooms";
    public static final String ROOM_MANAGEMENT =
            "/room/{roomId:.+}";

    public static String format(String url, Object... variables)
    {
        for (Object variable : variables) {
            url = url.replaceFirst("\\{[^/]+\\}", variable != null ? variable.toString() : "");
        }
        return url;
    }

    public static String getReservationRequestDetail(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL, reservationRequestId);
    }

    public static String getReservationRequestDetailChildren(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL_CHILDREN, reservationRequestId);
    }

    public static String getReservationRequestDetailUsages(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL_USAGES, reservationRequestId);
    }

    public static String getReservationRequestModify(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_MODIFY, reservationRequestId);
    }

    public static String getReservationRequestDelete(String path, String reservationRequestI)
    {
        return path + format(RESERVATION_REQUEST_DELETE, reservationRequestI);
    }

    public static String getReservationRequestDeleteConfirm(String path, String reservationRequestI)
    {
        return path + format(RESERVATION_REQUEST_DELETE_CONFIRM, reservationRequestI);
    }

    public static String getReservationRequestAcl(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_ACL, reservationRequestId);
    }

    public static String getReservationRequestAclCreate(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_ACL_CREATE, reservationRequestId);
    }

    public static String getReservationRequestAclDelete(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_ACL_DELETE, reservationRequestId);
    }

    public static String getReservationRequestAclDelete(String path, String reservationRequestId, String aclRecordId)
    {
        return path + format(RESERVATION_REQUEST_ACL_DELETE, reservationRequestId, aclRecordId);
    }

    public static String getRoomManagement(String roomId)
    {
        return format(ROOM_MANAGEMENT, roomId);
    }

    public static String getRoomManagement(String path, String roomId)
    {
        return path + getRoomManagement(roomId);
    }
}
