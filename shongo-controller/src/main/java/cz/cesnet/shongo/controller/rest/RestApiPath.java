package cz.cesnet.shongo.controller.rest;

/**
 * URL paths for REST endpoints.
 *
 * @author Filip Karnis
 */
public class RestApiPath
{

    public static final String API_PREFIX = "/api/v1";
    public static final String ID_SUFFIX = "/{id:.+}";
    public static final String ENTITY_SUFFIX = "/{entityId:.+}";

    public static final String REPORT = API_PREFIX + "/report";

    // Users and groups
    public static final String USERS_AND_GROUPS = API_PREFIX;
    public static final String SETTINGS = "/settings";
    public static final String USERS_LIST = "/users";
    public static final String USERS_DETAIL = "/users/{userId:.+}";
    public static final String GROUPS_LIST = "/groups";
    public static final String GROUPS_DETAIL = "/groups/{groupId:.+}";

    // Resources
    public static final String RESOURCES = API_PREFIX + "/resources";
    public static final String CAPACITY_UTILIZATION = "/capacity_utilization";
    public static final String CAPACITY_UTILIZATION_DETAIL = "/{id}/capacity_utilization";

    // Reservation requests
    public static final String RESERVATION_REQUESTS = API_PREFIX + "/reservation_requests";
    public static final String RESERVATION_REQUESTS_ACCEPT = ID_SUFFIX + "/accept";
    public static final String RESERVATION_REQUESTS_REJECT = ID_SUFFIX + "/reject";
    public static final String RESERVATION_REQUESTS_REVERT = ID_SUFFIX + "/revert";

    // Participants
    public static final String PARTICIPANTS = API_PREFIX + "/reservation_requests/{id:.+}/participants";
    public static final String PARTICIPANTS_ID_SUFFIX = "/{participantId:.+}";

    // Roles
    public static final String ROLES = API_PREFIX + "/reservation_requests/{id:.+}/roles";

    // Recordings
    public static final String RECORDINGS = API_PREFIX + "/reservation_requests/{id:.+}/recordings";
    public static final String RECORDINGS_ID_SUFFIX = "/{recordingId:.+}";

    // Rooms
    public static final String ROOMS = API_PREFIX + "/rooms";

    // Runtime management
    public static final String RUNTIME_MANAGEMENT = API_PREFIX + "/reservation_requests/{id:.+}/runtime_management";
    public static final String RUNTIME_MANAGEMENT_PARTICIPANTS = "/participants";
    public static final String RUNTIME_MANAGEMENT_PARTICIPANTS_MODIFY = "/participants/{participantId:.+}";
    public static final String RUNTIME_MANAGEMENT_PARTICIPANTS_DISCONNECT = "/participants/{participantId:.+}/disconnect";
    public static final String RUNTIME_MANAGEMENT_PARTICIPANTS_SNAPSHOT = "/participants/{participantId:.+}/snapshot";
    public static final String RUNTIME_MANAGEMENT_RECORDING_START = "/recording/start";
    public static final String RUNTIME_MANAGEMENT_RECORDING_STOP = "/recording/stop";
}
