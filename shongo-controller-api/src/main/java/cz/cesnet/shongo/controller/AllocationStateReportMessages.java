package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for SchedulerReportSet.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class AllocationStateReportMessages
{
    public static final String USER_NOT_ALLOWED = "user-not-allowed";
    public static final String RESOURCE_NOT_FOUND = "resource-not-found";
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_NOT_ALLOCATABLE = "resource-not-allocatable";
    public static final String RESOURCE_ALREADY_ALLOCATED = "resource-already-allocated";
    public static final String RESOURCE_UNDER_MAINTENANCE = "resource-under-maintenance";
    public static final String RESOURCE_NOT_AVAILABLE = "resource-not-available";
    public static final String RESOURCE_ROOM_CAPACITY_EXCEEDED = "resource-room-capacity-exceeded";
    public static final String RESOURCE_SINGLE_ROOM_LIMIT_EXCEEDED = "resource-single-room-limit-exceeded";
    public static final String RESOURCE_RECORDING_CAPACITY_EXCEEDED = "resource-recording-capacity-exceeded";
    public static final String RESOURCE_NOT_ENDPOINT = "resource-not-endpoint";
    public static final String RESOURCE_MULTIPLE_REQUESTED = "resource-multiple-requested";
    public static final String ENDPOINT_NOT_FOUND = "endpoint-not-found";
    public static final String EXECUTABLE_REUSING = "executable-reusing";
    public static final String ROOM_EXECUTABLE_NOT_EXISTS = "room-executable-not-exists";
    public static final String EXECUTABLE_INVALID_SLOT = "executable-invalid-slot";
    public static final String EXECUTABLE_ALREADY_USED = "executable-already-used";
    public static final String COMPARTMENT_NOT_ENOUGH_ENDPOINT = "compartment-not-enough-endpoint";
    public static final String COMPARTMENT_ASSIGN_ALIAS_TO_EXTERNAL_ENDPOINT = "compartment-assign-alias-to-external-endpoint";
    public static final String CONNECTION_BETWEEN = "connection-between";
    public static final String CONNECTION_FROM_TO = "connection-from-to";
    public static final String CONNECTION_TO_MULTIPLE = "connection-to-multiple";
    public static final String RESERVATION_REQUEST_INVALID_SLOT = "reservation-request-invalid-slot";
    public static final String RESERVATION_REQUEST_DENIED = "reservation-request-denied";
    public static final String RESERVATION_REQUEST_DENIED_ALREADY_ALLOCATED = "reservation-request-denied-already-allocated";
    public static final String RESERVATION_WITHOUT_MANDATORY_USAGE = "reservation-without-mandatory-usage";
    public static final String RESERVATION_ALREADY_USED = "reservation-already-used";
    public static final String RESERVATION_REUSING = "reservation-reusing";
    public static final String VALUE_ALREADY_ALLOCATED = "value-already-allocated";
    public static final String VALUE_INVALID = "value-invalid";
    public static final String VALUE_NOT_AVAILABLE = "value-not-available";
    public static final String EXECUTABLE_SERVICE_INVALID_SLOT = "executable-service-invalid-slot";
    public static final String ROOM_ENDPOINT_ALWAYS_RECORDABLE = "room-endpoint-always-recordable";
    public static final String ALLOCATING_RESOURCE = "allocating-resource";
    public static final String ALLOCATING_ALIAS = "allocating-alias";
    public static final String ALLOCATING_VALUE = "allocating-value";
    public static final String ALLOCATING_ROOM = "allocating-room";
    public static final String ALLOCATING_RECORDING_SERVICE = "allocating-recording-service";
    public static final String ALLOCATING_COMPARTMENT = "allocating-compartment";
    public static final String ALLOCATING_EXECUTABLE = "allocating-executable";
    public static final String SPECIFICATION_CHECKING_AVAILABILITY = "specification-checking-availability";
    public static final String FINDING_AVAILABLE_RESOURCE = "finding-available-resource";
    public static final String SORTING_RESOURCES = "sorting-resources";
    public static final String COLLIDING_RESERVATIONS = "colliding-reservations";
    public static final String REALLOCATING_RESERVATION_REQUESTS = "reallocating-reservation-requests";
    public static final String REALLOCATING_RESERVATION_REQUEST = "reallocating-reservation-request";
    public static final String SPECIFICATION_NOT_READY = "specification-not-ready";
    public static final String SPECIFICATION_NOT_ALLOCATABLE = "specification-not-allocatable";
    public static final String MAXIMUM_DURATION_EXCEEDED = "maximum-duration-exceeded";
    public static final String USER_NOT_OWNER = "user-not-owner";

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage(USER_NOT_ALLOWED, new Report.UserType[]{}, Report.Language.ENGLISH, "User does not have permissions for the resource ${resource.id}.");
        addMessage(RESOURCE_NOT_FOUND, new Report.UserType[]{}, Report.Language.ENGLISH, "No resource was found.");
        addMessage(RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Resource ${resource.id}.");
        addMessage(RESOURCE_NOT_ALLOCATABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is disabled for allocation.");
        addMessage(RESOURCE_ALREADY_ALLOCATED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is already allocated in the time slot ${interval}.");
        addMessage(RESOURCE_UNDER_MAINTENANCE, new Report.UserType[]{}, Report.Language.ENGLISH, "There is no available capacity due to maintenance in the time slot ${interval}.");
        addMessage(RESOURCE_NOT_AVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is ${maxDateTime}.");
        addMessage(RESOURCE_ROOM_CAPACITY_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} has available only ${availableLicenseCount} from ${maxLicenseCount} licenses.");
        addMessage(RESOURCE_SINGLE_ROOM_LIMIT_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource has capacity limit per room of ${maxLicencesPerRoom} licences.");
        addMessage(RESOURCE_RECORDING_CAPACITY_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} doesn't have any available licenses for recording.");
        addMessage(RESOURCE_NOT_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is not endpoint.");
        addMessage(RESOURCE_MULTIPLE_REQUESTED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is requested multiple times.");
        addMessage(ENDPOINT_NOT_FOUND, new Report.UserType[]{}, Report.Language.ENGLISH, "No available endpoint was found for the following specification: Technologies: ${technologies}");
        addMessage(EXECUTABLE_REUSING, new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing existing ${executable}.");
        addMessage(ROOM_EXECUTABLE_NOT_EXISTS, new Report.UserType[]{}, Report.Language.ENGLISH, "Room executable doesn't exist.");
        addMessage(EXECUTABLE_INVALID_SLOT, new Report.UserType[]{}, Report.Language.ENGLISH, "Requested time slot doesn't correspond to ${interval} from reused executable ${executable}.");
        addMessage(EXECUTABLE_ALREADY_USED, new Report.UserType[]{}, Report.Language.ENGLISH, "Reused executable ${executable} is not available because it's already used in reservation request ${usageReservationRequest} for ${usageInterval}.");
        addMessage(COMPARTMENT_NOT_ENOUGH_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "Not enough endpoints are requested for the compartment.");
        addMessage(COMPARTMENT_ASSIGN_ALIAS_TO_EXTERNAL_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot assign alias to allocated external endpoint.");
        addMessage(CONNECTION_BETWEEN, new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection between ${endpointFrom} and ${endpointTo} in technology ${technology}.");
        addMessage(CONNECTION_FROM_TO, new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection from ${endpointFrom} to ${endpointTo}.");
        addMessage(CONNECTION_TO_MULTIPLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot create connection from ${endpointFrom} to ${endpointTo}, because the target represents multiple endpoints (not supported yet).");
        addMessage(RESERVATION_REQUEST_INVALID_SLOT, new Report.UserType[]{}, Report.Language.ENGLISH, "Requested time slot doesn't correspond to ${interval} from reused reservation request ${reservationRequest}.");
        addMessage(RESERVATION_REQUEST_DENIED, new Report.UserType[]{}, Report.Language.ENGLISH, "The reservation request has been denied by resource owner ${deniedBy}. Reason: ${reason}");
        addMessage(RESERVATION_REQUEST_DENIED_ALREADY_ALLOCATED, new Report.UserType[]{}, Report.Language.ENGLISH, "The reservation request has been denied. Reason: The resource ${resource.id} is already allocated in interval ${interval}.");
        addMessage(RESERVATION_WITHOUT_MANDATORY_USAGE, new Report.UserType[]{}, Report.Language.ENGLISH, "Reused reservation request ${reservationRequest} is mandatory but wasn't used.");
        addMessage(RESERVATION_ALREADY_USED, new Report.UserType[]{}, Report.Language.ENGLISH, "Reused reservation request ${reservationRequest} is not available because it's reservation ${reservation} is already used in reservation request ${usageReservationRequest} for ${usageInterval}.");
        addMessage(RESERVATION_REUSING, new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing ${reservation}.");
        addMessage(VALUE_ALREADY_ALLOCATED, new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is already allocated in interval ${interval}.");
        addMessage(VALUE_INVALID, new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is invalid.");
        addMessage(VALUE_NOT_AVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "No value is available.");
        addMessage(EXECUTABLE_SERVICE_INVALID_SLOT, new Report.UserType[]{}, Report.Language.ENGLISH, "Requested service slot ${serviceSlot} is outside the executable slot ${executableSlot}.");
        addMessage(ROOM_ENDPOINT_ALWAYS_RECORDABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Recording service cannot be allocated for the room endpoint ${roomEndpointId} because it is always recordable.");
        addMessage(ALLOCATING_RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating the resource ${resource.id}.");
        addMessage(ALLOCATING_ALIAS, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating alias for the following specification: \n  Technology: ${ifEmpty(technologies, \"Any\")} \n  Alias Type: ${ifEmpty(aliasTypes, \"Any\")} \n       Value: ${ifEmpty(value, \"Any\")}");
        addMessage(ALLOCATING_VALUE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating value in the resource ${resource.id}.");
        addMessage(ALLOCATING_ROOM, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating room for the following specification: \n    Technology: ${technologySets} \n  Participants: ${participantCount} \n      Resource: ${resource.id}");
        addMessage(ALLOCATING_RECORDING_SERVICE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating recording service for the following specification: \n    Enabled: ${enabled}");
        addMessage(ALLOCATING_COMPARTMENT, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating compartment.");
        addMessage(ALLOCATING_EXECUTABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating executable.");
        addMessage(SPECIFICATION_CHECKING_AVAILABILITY, new Report.UserType[]{}, Report.Language.ENGLISH, "Checking specification availability report.");
        addMessage(FINDING_AVAILABLE_RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Finding available resource.");
        addMessage(SORTING_RESOURCES, new Report.UserType[]{}, Report.Language.ENGLISH, "Sorting resources.");
        addMessage(COLLIDING_RESERVATIONS, new Report.UserType[]{}, Report.Language.ENGLISH, "The following reservations are colliding, trying to reallocate them: ${format(reservations, \"\n-$key ($value)\", \"\")}");
        addMessage(REALLOCATING_RESERVATION_REQUESTS, new Report.UserType[]{}, Report.Language.ENGLISH, "The following reservation requests will be reallocated: ${format(reservationRequests, \"\n-$value\", \"\")}");
        addMessage(REALLOCATING_RESERVATION_REQUEST, new Report.UserType[]{}, Report.Language.ENGLISH, "Reallocating reservation request ${reservationRequest}.");
        addMessage(SPECIFICATION_NOT_READY, new Report.UserType[]{}, Report.Language.ENGLISH, "Specification ${specification} is not ready.");
        addMessage(SPECIFICATION_NOT_ALLOCATABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The specification ${specification} is not supposed to be allocated.");
        addMessage(MAXIMUM_DURATION_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "Duration ${duration} is longer than maximum ${maxDuration}.");
        addMessage(USER_NOT_OWNER, new Report.UserType[]{}, Report.Language.ENGLISH, "User is not resource owner.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, org.joda.time.DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, timeZone, parameters);
    }
}