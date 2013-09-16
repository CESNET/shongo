package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for SchedulerReportSet.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class AllocationStateReportMessages
{
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_NOT_ALLOCATABLE = "resource-not-allocatable";
    public static final String RESOURCE_ALREADY_ALLOCATED = "resource-already-allocated";
    public static final String RESOURCE_NOT_AVAILABLE = "resource-not-available";
    public static final String RESOURCE_NOT_ENDPOINT = "resource-not-endpoint";
    public static final String RESOURCE_MULTIPLE_REQUESTED = "resource-multiple-requested";
    public static final String RESOURCE_NOT_FOUND = "resource-not-found";
    public static final String EXECUTABLE_REUSING = "executable-reusing";
    public static final String COMPARTMENT_NOT_ENOUGH_ENDPOINT = "compartment-not-enough-endpoint";
    public static final String COMPARTMENT_ASSIGN_ALIAS_TO_EXTERNAL_ENDPOINT = "compartment-assign-alias-to-external-endpoint";
    public static final String CONNECTION_BETWEEN = "connection-between";
    public static final String CONNECTION_FROM_TO = "connection-from-to";
    public static final String CONNECTION_TO_MULTIPLE = "connection-to-multiple";
    public static final String RESERVATION_REQUEST_NOT_USABLE = "reservation-request-not-usable";
    public static final String RESERVATION_NOT_AVAILABLE = "reservation-not-available";
    public static final String RESERVATION_REUSING = "reservation-reusing";
    public static final String VALUE_ALREADY_ALLOCATED = "value-already-allocated";
    public static final String VALUE_INVALID = "value-invalid";
    public static final String VALUE_NOT_AVAILABLE = "value-not-available";
    public static final String ALLOCATING_RESOURCE = "allocating-resource";
    public static final String ALLOCATING_ALIAS = "allocating-alias";
    public static final String ALLOCATING_VALUE = "allocating-value";
    public static final String ALLOCATING_ROOM = "allocating-room";
    public static final String ALLOCATING_COMPARTMENT = "allocating-compartment";
    public static final String ALLOCATING_EXECUTABLE = "allocating-executable";
    public static final String SPECIFICATION_CHECKING_AVAILABILITY = "specification-checking-availability";
    public static final String FINDING_AVAILABLE_RESOURCE = "finding-available-resource";
    public static final String SORTING_RESOURCES = "sorting-resources";
    public static final String SPECIFICATION_NOT_READY = "specification-not-ready";
    public static final String DURATION_LONGER_THAN_MAXIMUM = "duration-longer-than-maximum";
    public static final String SPECIFICATION_NOT_ALLOCATABLE = "specification-not-allocatable";
    public static final String USER_NOT_OWNER = "user-not-owner";

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage(RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Resource ${resource.id}.");
        addMessage(RESOURCE, new Report.UserType[]{}, Report.Language.CZECH, "Zdroj ${resource.id}.");
        addMessage(RESOURCE_NOT_ALLOCATABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is disabled for allocation.");
        addMessage(RESOURCE_NOT_ALLOCATABLE, new Report.UserType[]{}, Report.Language.CZECH, "Rezervace zdroje ${resource.id} je zakázána.");
        addMessage(RESOURCE_ALREADY_ALLOCATED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is already allocated.");
        addMessage(RESOURCE_NOT_AVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is ${maxDateTime}.");
        addMessage(RESOURCE_NOT_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is not endpoint.");
        addMessage(RESOURCE_MULTIPLE_REQUESTED, new Report.UserType[]{}, Report.Language.ENGLISH, "The resource ${resource.id} is requested multiple times.");
        addMessage(RESOURCE_NOT_FOUND, new Report.UserType[]{}, Report.Language.ENGLISH, "No available resource was found for the following specification: Technologies: ${technologies}");
        addMessage(EXECUTABLE_REUSING, new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing existing ${executable}.");
        addMessage(COMPARTMENT_NOT_ENOUGH_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "Not enough endpoints are requested for the compartment.");
        addMessage(COMPARTMENT_ASSIGN_ALIAS_TO_EXTERNAL_ENDPOINT, new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot assign alias to allocated external endpoint.");
        addMessage(CONNECTION_BETWEEN, new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection between ${endpointFrom} and ${endpointTo} in technology ${technology}.");
        addMessage(CONNECTION_FROM_TO, new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection from ${endpointFrom} to ${endpointTo}.");
        addMessage(CONNECTION_TO_MULTIPLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot create connection from ${endpointFrom} to ${endpointTo}, because the target represents multiple endpoints (not supported yet).");
        addMessage(RESERVATION_REQUEST_NOT_USABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "No reservation is allocated for reused ${reservationRequest} which can be used in requested time slot.");
        addMessage(RESERVATION_NOT_AVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The ${reservation} from reused ${reusedReservationRequest} is not available because it is already allocated for another reservation request in requested time slot.");
        addMessage(RESERVATION_REUSING, new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing ${reservation}.");
        addMessage(VALUE_ALREADY_ALLOCATED, new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is already allocated in interval ${interval}.");
        addMessage(VALUE_INVALID, new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is invalid.");
        addMessage(VALUE_NOT_AVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "No value is available.");
        addMessage(ALLOCATING_RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating the resource ${resource.id}.");
        addMessage(ALLOCATING_ALIAS, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating alias for the following specification: \n  Technology: ${ifEmpty(technologies, \"Any\")} \n  Alias Type: ${ifEmpty(aliasTypes, \"Any\")} \n       Value: ${ifEmpty(value, \"Any\")}");
        addMessage(ALLOCATING_VALUE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating value in the resource ${resource.id}.");
        addMessage(ALLOCATING_ROOM, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating room for the following specification: \n    Technology: ${technologies} \n  Participants: ${participantCount}");
        addMessage(ALLOCATING_COMPARTMENT, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating compartment.");
        addMessage(ALLOCATING_EXECUTABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating executable.");
        addMessage(SPECIFICATION_CHECKING_AVAILABILITY, new Report.UserType[]{}, Report.Language.ENGLISH, "Checking specification availability report.");
        addMessage(FINDING_AVAILABLE_RESOURCE, new Report.UserType[]{}, Report.Language.ENGLISH, "Finding available resource.");
        addMessage(SORTING_RESOURCES, new Report.UserType[]{}, Report.Language.ENGLISH, "Sorting resources.");
        addMessage(SPECIFICATION_NOT_READY, new Report.UserType[]{}, Report.Language.ENGLISH, "Specification ${specification} is not ready.");
        addMessage(DURATION_LONGER_THAN_MAXIMUM, new Report.UserType[]{}, Report.Language.ENGLISH, "Duration ${duration} is longer than maximum ${maximumDuration}.");
        addMessage(SPECIFICATION_NOT_ALLOCATABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "The specification ${specification} is not supposed to be allocated.");
        addMessage(USER_NOT_OWNER, new Report.UserType[]{}, Report.Language.ENGLISH, "User is not resource owner.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }
}