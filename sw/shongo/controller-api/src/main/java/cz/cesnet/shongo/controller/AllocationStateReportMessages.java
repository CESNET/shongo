package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for SchedulerReportSet.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class AllocationStateReportMessages
{
    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage("resource", new Report.UserType[]{}, Report.Language.ENGLISH, "Resource ${resource}.");
        addMessage("resource", new Report.UserType[]{}, Report.Language.CZECH, "Zdroj ${resource}.");
        addMessage("resource-not-allocatable", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${resource} is disabled for allocation.");
        addMessage("resource-not-allocatable", new Report.UserType[]{}, Report.Language.CZECH, "Rezervace zdroje ${resource} je zakázána.");
        addMessage("resource-already-allocated", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${resource} is already allocated.");
        addMessage("resource-not-available", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${resource} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is ${maxDateTime}.");
        addMessage("resource-not-endpoint", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${resource} is not endpoint.");
        addMessage("resource-multiple-requested", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${resource} is requested multiple times.");
        addMessage("resource-not-found", new Report.UserType[]{}, Report.Language.ENGLISH, "No available resource was found for the following specification: Technologies: ${technologies}");
        addMessage("executable-reusing", new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing existing ${executable}.");
        addMessage("compartment-not-enough-endpoint", new Report.UserType[]{}, Report.Language.ENGLISH, "Not enough endpoints are requested for the compartment.");
        addMessage("compartment-assign-alias-to-external-endpoint", new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot assign alias to allocated external endpoint.");
        addMessage("connection-between", new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection between ${endpointFrom} and ${endpointTo} in technology ${technology}.");
        addMessage("connection-from-to", new Report.UserType[]{}, Report.Language.ENGLISH, "Creating connection from ${endpointFrom} to ${endpointTo}.");
        addMessage("connection-to-multiple", new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot create connection from ${endpointFrom} to ${endpointTo}, because the target represents multiple endpoints (not supported yet).");
        addMessage("reservation-request-not-usable", new Report.UserType[]{}, Report.Language.ENGLISH, "No reservation is allocated for reused ${reservationRequest} which can be used in requested time slot.");
        addMessage("reservation-not-available", new Report.UserType[]{}, Report.Language.ENGLISH, "The ${reservation} from reused ${reusedReservationRequest} is not available because it is already allocated for another reservation request in requested time slot.");
        addMessage("reservation-reusing", new Report.UserType[]{}, Report.Language.ENGLISH, "Reusing ${reservation}.");
        addMessage("value-already-allocated", new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is already allocated.");
        addMessage("value-invalid", new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is invalid.");
        addMessage("value-not-available", new Report.UserType[]{}, Report.Language.ENGLISH, "No value is available.");
        addMessage("allocating-resource", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating the ${resource}.");
        addMessage("allocating-alias", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating alias for the following specification: \n  Technology: ${ifEmpty(technologies, \"Any\")} \n  Alias Type: ${ifEmpty(aliasTypes, \"Any\")} \n       Value: ${ifEmpty(value, \"Any\")}");
        addMessage("allocating-value", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating value in the ${resource}.");
        addMessage("allocating-room", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating room for the following specification: \n    Technology: ${technologies} \n  Participants: ${participantCount}");
        addMessage("allocating-compartment", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating compartment.");
        addMessage("allocating-executable", new Report.UserType[]{}, Report.Language.ENGLISH, "Allocating executable.");
        addMessage("specification-checking-availability", new Report.UserType[]{}, Report.Language.ENGLISH, "Checking specification availability report.");
        addMessage("finding-available-resource", new Report.UserType[]{}, Report.Language.ENGLISH, "Finding available resource.");
        addMessage("sorting-resources", new Report.UserType[]{}, Report.Language.ENGLISH, "Sorting resources.");
        addMessage("specification-not-ready", new Report.UserType[]{}, Report.Language.ENGLISH, "Specification ${specification} is not ready.");
        addMessage("duration-longer-than-maximum", new Report.UserType[]{}, Report.Language.ENGLISH, "Duration ${duration} is longer than maximum ${maximumDuration}.");
        addMessage("specification-not-allocatable", new Report.UserType[]{}, Report.Language.ENGLISH, "The specification ${specification} is not supposed to be allocated.");
        addMessage("user-not-owner", new Report.UserType[]{}, Report.Language.ENGLISH, "User is not resource owner.");
    }};

    /**
     * @param reportId
     * @param userType
     * @param language
     * @param parameters
     * @return message for the report with given {@code uniqueId}
     */
    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }
}