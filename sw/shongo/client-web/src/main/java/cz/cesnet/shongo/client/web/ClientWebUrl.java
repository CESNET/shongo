package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.models.ReservationRequestModel;

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

    public static final String REPORT =
            "/report";
    public static final String REPORT_SUBMIT =
            "/report/submit";

    public static final String WIZARD =
            "/wizard";
    public static final String WIZARD_SELECT =
            "/wizard/select";
    public static final String WIZARD_RESERVATION_REQUEST_LIST =
            "/wizard/reservation-request";
    public static final String WIZARD_RESERVATION_REQUEST_DETAIL =
            "/wizard/reservation-request/{reservationRequestId:.+}";
    public static final String WIZARD_RESERVATION_REQUEST_DELETE =
            "/wizard/reservation-request/{reservationRequestId:.+}/delete";
    public static final String WIZARD_RESERVATION_REQUEST_DELETE_CONFIRM =
            "/wizard/reservation-request/{reservationRequestId:.+}/delete/confirm";
    public static final String WIZARD_CREATE_ROOM =
            "/wizard/create";
    public static final String WIZARD_CREATE_ADHOC_ROOM =
            "/wizard/create/adhoc-room";
    public static final String WIZARD_CREATE_PERMANENT_ROOM =
            "/wizard/create/permanent-room";
    public static final String WIZARD_CREATE_ROOM_ATTRIBUTES =
            "/wizard/create/attributes";
    public static final String WIZARD_CREATE_ROOM_ATTRIBUTES_PROCESS =
            "/wizard/create/attributes/process";
    public static final String WIZARD_CREATE_ROOM_ROLES =
            "/wizard/create/roles";
    public static final String WIZARD_CREATE_ROOM_ROLE_CREATE =
            "/wizard/create/role/create";
    public static final String WIZARD_CREATE_ROOM_ROLE_DELETE =
            "/wizard/create/role/{userRoleId:.+}/delete";
    public static final String WIZARD_CREATE_ROOM_CONFIRM =
            "/wizard/create/confirm";
    public static final String WIZARD_CREATE_ROOM_CONFIRMED =
            "/wizard/create/confirmed";

    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY =
            "/wizard/create/permanent-room-capacity";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PROCESS =
            "/wizard/create/permanent-room-capacity/process";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM =
            "/wizard/create/permanent-room-capacity/confirm";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRMED =
            "/wizard/create/permanent-room-capacity/confirmed";

    public static final String RESERVATION_REQUEST =
            "/reservation-request";
    public static final String RESERVATION_REQUEST_LIST =
            "/reservation-request/list";
    public static final String RESERVATION_REQUEST_LIST_DATA =
            "/reservation-request/list/data";
    public static final String RESERVATION_REQUEST_CREATE =
            "/reservation-request/create/{type}";
    public static final String RESERVATION_REQUEST_CREATE_CONFIRM =
            "/reservation-request/create/confirm";
    public static final String RESERVATION_REQUEST_DETAIL =
            "/reservation-request/{reservationRequestId:.+}/detail";
    public static final String RESERVATION_REQUEST_DETAIL_CHILDREN =
            "/reservation-request/{reservationRequestId:.+}/detail/children";
    public static final String RESERVATION_REQUEST_DETAIL_USAGES =
            "/reservation-request/{reservationRequestId:.+}/detail/usages";
    public static final String RESERVATION_REQUEST_DETAIL_REVERT =
            "/reservation-request/{reservationRequestId:.+}/detail/revert";
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

    public static String getReservationRequestDetail(String reservationRequestId)
    {
        return format(RESERVATION_REQUEST_DETAIL, reservationRequestId);
    }

    public static String getReservationRequestDetail(String path, String reservationRequestId)
    {
        return path + getReservationRequestDetail(reservationRequestId);
    }

    public static String getReservationRequestDetailChildren(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL_CHILDREN, reservationRequestId);
    }

    public static String getReservationRequestDetailUsages(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL_USAGES, reservationRequestId);
    }

    public static String getReservationRequestDetailRevert(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_DETAIL_REVERT, reservationRequestId);
    }

    public static String getReservationRequestCreate(String path, ReservationRequestModel.SpecificationType type)
    {
        return path + format(RESERVATION_REQUEST_CREATE, type);
    }

    public static String getReservationRequestCreatePermanentRoomCapacity(String path, String permanentRoom)
    {
        return getReservationRequestCreate(path, ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY) +
                "?permanentRoom=" + permanentRoom;
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

    public static String getReservationRequestAclCreateConfirm(String path, String reservationRequestId)
    {
        return path + format(RESERVATION_REQUEST_ACL_CREATE_CONFIRM, reservationRequestId);
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

    public static String getWizardReservationRequestDelete(String path, String reservationRequestI)
    {
        return path + getWizardReservationRequestDelete(reservationRequestI);
    }

    public static String getWizardReservationRequestDelete(String reservationRequestI)
    {
        return format(WIZARD_RESERVATION_REQUEST_DELETE, reservationRequestI);
    }

    public static String getWizardReservationRequestDeleteConfirm(String reservationRequestI)
    {
        return format(WIZARD_RESERVATION_REQUEST_DELETE_CONFIRM, reservationRequestI);
    }

    public static String getWizardCreatePermanentRoomCapacity(String path, String permanentRoom)
    {
        return path + format(WIZARD_CREATE_PERMANENT_ROOM_CAPACITY) + "?permanentRoom=" + permanentRoom;
    }
}
