package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.PageNotAuthorizedException;
import cz.cesnet.shongo.client.web.resource.ResourceCapacity;
import cz.cesnet.shongo.client.web.resource.ResourceCapacityUtilization;
import cz.cesnet.shongo.client.web.resource.ResourcesUtilization;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.IntervalEditor;
import cz.cesnet.shongo.client.web.support.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.support.editors.PeriodEditor;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Controller for resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ResourceController
{
    @javax.annotation.Resource
    protected ResourceService resourceService;

    @javax.annotation.Resource
    protected ReservationService reservationService;

    @javax.annotation.Resource
    protected AuthorizationService authorizationService;

    @javax.annotation.Resource
    protected Cache cache;

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
        binder.registerCustomEditor(Interval.class, new IntervalEditor());
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    /**
     * Handle reservable resource list request
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> handleResourceListData(
            SecurityToken securityToken,
            @RequestParam(value = "capabilityClass", required = false) String capabilityClassName,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "tag", required = false) String tag)
            throws ClassNotFoundException
    {
        ResourceListRequest resourceListRequest = new ResourceListRequest();
        resourceListRequest.setSecurityToken(securityToken);
        resourceListRequest.setAllocatable(true);
        if (capabilityClassName != null) {
            Class<? extends Capability> capabilityType = ClassHelper.getClassFromShortName(capabilityClassName);
            resourceListRequest.addCapabilityClass(capabilityType);
        }
        if (technology != null) {
            resourceListRequest.setTechnologies(technology.getTechnologies());
        }
        if (tag != null) {
            resourceListRequest.setTagName(tag);
        }

        // Filter only reservable resources
        List<Map<String, Object>> resources = new LinkedList<Map<String, Object>>();
        resourceListRequest.setPermission(ObjectPermission.RESERVE_RESOURCE);
        ListResponse<ResourceSummary> accessibleResources = resourceService.listResources(resourceListRequest);
        for (ResourceSummary resourceSummary : accessibleResources) {
            Map<String, Object> resource = new HashMap<String, Object>();
            resource.put("id", resourceSummary.getId());
            resource.put("name", resourceSummary.getName());
            resource.put("technology", TechnologyModel.find(resourceSummary.getTechnologies()));
            resource.put("description", resourceSummary.getDescription());
            resource.put("domainName", resourceSummary.getDomainName());
            resource.put("allocatable", resourceSummary.getAllocatable());
            resources.add(resource);
        }
        return resources;
    }

    /**
     * Handle resource reservations view
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_RESERVATIONS_VIEW, method = RequestMethod.GET)
    public ModelAndView handleReservationsView(SecurityToken securityToken)
    {
        Map<String, String> resources = new LinkedHashMap<String, String>();
        for (ResourceSummary resourceSummary : resourceService.listResources(new ResourceListRequest(securityToken))) {
            String resourceId = resourceSummary.getId();
            StringBuilder resourceTitle = new StringBuilder();
            resourceTitle.append("<b>");
            resourceTitle.append(resourceSummary.getName());
            resourceTitle.append("</b>");
            Set<Technology> resourceTechnologies = resourceSummary.getTechnologies();
            if (!resourceTechnologies.isEmpty()) {
                resourceTitle.append(" (");
                for (Technology technology : resourceTechnologies) {
                    resourceTitle.append(technology.getName());
                }
                resourceTitle.append(")");
            }
            resources.put(resourceId, resourceTitle.toString());
        }
        ModelAndView modelAndView = new ModelAndView("resourceReservations");
        modelAndView.addObject("resources", resources);
        return modelAndView;
    }

    /**
     * Handle resource reservations data
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_RESERVATIONS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Object handleReservationsData(
            SecurityToken securityToken,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "type", required = false) ReservationSummary.Type type,
            @RequestParam(value = "start") DateTime start,
            @RequestParam(value = "end") DateTime end)
    {
        ReservationListRequest request = new ReservationListRequest(securityToken);
        request.setSort(ReservationListRequest.Sort.SLOT);
        if (resourceId != null) {
            request.addResourceId(resourceId);
        }
        if (type != null) {
            request.addReservationType(type);
        }
        request.setInterval(new Interval(start, end));
        ListResponse<ReservationSummary> listResponse = reservationService.listReservations(request);
        List<Map> reservations = new LinkedList<Map>();
        for (ReservationSummary reservationSummary : listResponse.getItems()) {
            Map<String, Object> reservation = new HashMap<String, Object>();
            reservation.put("id", reservationSummary.getId());
            reservation.put("type", reservationSummary.getType());
            reservation.put("slotStart", reservationSummary.getSlot().getStart().toString());
            reservation.put("slotEnd", reservationSummary.getSlot().getEnd().toString());
            reservation.put("resourceId", reservationSummary.getResourceId());
            reservation.put("roomLicenseCount", reservationSummary.getRoomLicenseCount());
            reservation.put("roomName", reservationSummary.getRoomName());
            reservation.put("aliasTypes", reservationSummary.getAliasTypesSet());
            reservation.put("value", reservationSummary.getValue());
            reservations.add(reservation);
        }
        return reservations;
    }

    /**
     * Handle resource capacity utilization view.
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION, method = RequestMethod.GET)
    public String handleCapacityUtilizationView(SecurityToken securityToken)
    {
        return "resourceCapacityUtilization";
    }

    /**
     * Handle table of {@link ResourceCapacityUtilization}s.
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_TABLE, method = RequestMethod.GET)
    public ModelAndView handleCapacityUtilizationTable(
            SecurityToken securityToken,
            @RequestParam(value = "period") Period period,
            @RequestParam(value = "start") DateTime start,
            @RequestParam(value = "end") DateTime end,
            @RequestParam(value = "style") ResourceCapacity.FormatStyle style,
            @RequestParam(value = "refresh", required = false) boolean refresh)
    {
        ResourcesUtilization resourcesUtilization =
                cache.getResourcesUtilization(securityToken, refresh);

        Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> utilization =
                resourcesUtilization.getUtilization(new Interval(start, end), period);
        ModelAndView modelAndView = new ModelAndView("resourceCapacityUtilizationTable");
        modelAndView.addObject("resourceCapacitySet", resourcesUtilization.getResourceCapacities());
        modelAndView.addObject("resourceCapacityUtilization", utilization);
        modelAndView.addObject("style", style);
        return modelAndView;
    }

    /**
     * Handle single {@link ResourceCapacityUtilization}.
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_DESCRIPTION, method = RequestMethod.GET)
    public ModelAndView handleCapacityUtilizationDescription(
            SecurityToken securityToken,
            @RequestParam(value = "interval") Interval interval,
            @RequestParam(value = "resourceCapacityClass") String resourceCapacityClassName,
            @RequestParam(value = "resourceId") String resourceId) throws ClassNotFoundException
    {
        @SuppressWarnings("unchecked")
        Class<? extends ResourceCapacity> resourceCapacityClass = (Class<? extends ResourceCapacity>)
                Class.forName(ResourceCapacity.class.getCanonicalName() + "$" + resourceCapacityClassName);
        ResourcesUtilization resourcesUtilization =
                cache.getResourcesUtilization(securityToken, false);
        ResourceCapacity resourceCapacity =
                resourcesUtilization.getResourceCapacity(resourceId, resourceCapacityClass);
        ResourceCapacityUtilization resourceCapacityUtilization =
                resourcesUtilization.getUtilization(resourceCapacity, interval);
        ModelAndView modelAndView = new ModelAndView("resourceCapacityUtilizationDescription");

        Map<String, UserInformation> users = new HashMap<String, UserInformation>();
        if (resourceCapacityUtilization != null) {
            Collection<String> userIds = resourceCapacityUtilization.getReservationUserIds();
            cache.fetchUserInformation(securityToken, userIds);
            for (String userId : userIds) {
                UserInformation userInformation = cache.getUserInformation(securityToken, userId);
                users.put(userId, userInformation);
            }
        }
        modelAndView.addObject("users", users);
        modelAndView.addObject("interval", interval);
        modelAndView.addObject("resourceCapacity", resourceCapacity);
        modelAndView.addObject("resourceCapacityUtilization", resourceCapacityUtilization);
        return modelAndView;
    }

    /**
     * TODO:
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleReservationRequestsConfirmationData(
            SecurityToken securityToken,
            Locale locale,
            DateTimeZone timeZone,
            @RequestParam(value = "interval-from", required = false) DateTime intervalFrom,
            @RequestParam(value = "interval-to", required = false) DateTime intervalTo,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "showExisting", required = false) Boolean showExisting,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "false") boolean sortDescending) throws ClassNotFoundException
    {
        ReservationRequestListRequest requestListRequest = new ReservationRequestListRequest(securityToken);
        // Listing is irrelevant when requests and reservations are shown at the same time
        if (!Boolean.TRUE.equals(showExisting)) {
            requestListRequest.setStart(start);
            requestListRequest.setCount(count);
        }
        requestListRequest.setSort(sort);
        requestListRequest.setSortDescending(sortDescending);
        requestListRequest.setAllocationState(AllocationState.CONFIRM_AWAITING);
        if (intervalFrom == null) {
            intervalFrom = Temporal.DATETIME_INFINITY_START;
        }
        if (intervalTo == null) {
            intervalTo = Temporal.DATETIME_INFINITY_END;
        }
        Interval interval = Temporal.roundIntervalToDays(new Interval(intervalFrom, intervalTo));
        requestListRequest.setInterval(interval);
        if (resourceId != null) {
            requestListRequest.setSpecificationResourceIds(new HashSet<>(Collections.singleton(resourceId)));
        } else {
            throw new TodoImplementException("list request for confirmation generaly");
        }
        ListResponse<ReservationRequestSummary> listResponse = reservationService.listOwnedResourcesReservationRequests(requestListRequest);

        List<Map> items = new LinkedList<>();
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        for (ReservationRequestSummary reservationRequestSummary : listResponse.getItems()) {
            // Skip reservation request sets, for now
            if (reservationRequestSummary.getFutureSlotCount() != null) {
                continue;
            }
            Map<String, Object> reservationRequest = new HashMap<>();
            reservationRequest.put("id", reservationRequestSummary.getId());
            reservationRequest.put("description", reservationRequestSummary.getDescription());
            UserInformation user = cache.getUserInformation(securityToken, reservationRequestSummary.getUserId());
            reservationRequest.put("user", user.getFullName());
            reservationRequest.put("userEmail", user.getPrimaryEmail());
            Interval slot = reservationRequestSummary.getEarliestSlot();
            // LIST
            reservationRequest.put("slot", formatter.formatInterval(slot));
            reservationRequest.put("resourceId", reservationRequestSummary.getResourceId());
            // CALENDAR
            reservationRequest.put("start", slot.getStart().toLocalDateTime().toString());
            reservationRequest.put("end", slot.getEnd().toLocalDateTime().toString());
            items.add(reservationRequest);
        }

        if (Boolean.TRUE.equals(showExisting)) {
            ReservationListRequest request = new ReservationListRequest();
            request.setSecurityToken(securityToken);
            request.setStart(start);
            request.setCount(count);
            request.setInterval(interval);
            if (resourceId != null) {
                request.addResourceId(resourceId);
            } else {
                throw new TodoImplementException("list request for confirmation generally");
            }
            ListResponse<ReservationSummary> reservationSummaries = reservationService.listReservations(request);

            for (ReservationSummary reservationSummary : reservationSummaries) {
                Map<String, Object> reservation = new HashMap<String, Object>();
//                reservation.put("id", reservationSummary.getId());
                reservation.put("type", reservationSummary.getType());
                reservation.put("description", reservationSummary.getReservationRequestDescription());
                if (UserInformation.isLocal(reservationSummary.getUserId())) {
                    UserInformation user = cache.getUserInformation(securityToken, reservationSummary.getUserId());
                    reservation.put("user", user.getFullName());
                    reservation.put("userEmail", user.getPrimaryEmail());
                }
                else {
                    Long domainId = UserInformation.parseDomainId(reservationSummary.getUserId());
                    String domainName = resourceService.getDomainName(securityToken, domainId.toString());
                    reservation.put("foreignDomain", domainName);
                }
                Interval slot = reservationSummary.getSlot();
                // LIST - not necessary for existing reservation
                // CALENDAR
                reservation.put("start", slot.getStart().toLocalDateTime().toString());
                reservation.put("end", slot.getEnd().toLocalDateTime().toString());
                reservation.put("reservation", true);

                items.add(reservation);
            }
        }

        Map<String, Object> data = new HashMap<>();
        // Listing is irrelevant when requests and reservations are shown at the same time
        if (!Boolean.TRUE.equals(showExisting)) {
            data.put("start", listResponse.getStart());
            data.put("count", listResponse.getCount());
        }
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }

    /**
     * TODO:Handle single {@link ResourceCapacityUtilization}.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION, method = RequestMethod.GET)
    public Object handleReservationRequestsConfirmation(
            SecurityToken securityToken,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "date", required = false) LocalDate date) throws ClassNotFoundException
    {
        Map<String, String> resources = new LinkedHashMap<>();
        ResourceListRequest listRequest = new ResourceListRequest(securityToken);
        listRequest.setOnlyLocal(true);
        listRequest.setNeedsConfirmation(true);
        listRequest.setPermission(ObjectPermission.CONTROL_RESOURCE);
        for (ResourceSummary resourceSummary : resourceService.listResources(listRequest)) {
            resources.put(resourceSummary.getId(), resourceSummary.getName());
        }
        ModelAndView modelAndView = new ModelAndView("resourceReservationRequestsConfirmationCalendar");
        // Check valid resourceId if present
        if (resourceId != null) {
            if (!resources.containsKey(resourceId)) {
                throw new PageNotAuthorizedException();
            } else {
                modelAndView.addObject("resourceId", resourceId);
            }
        }
        if (date != null) {
            modelAndView.addObject("date", Converter.convertLocalDateToString(date));
        }
        modelAndView.addObject("resources", resources);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_CONFIRM, method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public void handleReservationRequestConfirm(
            SecurityToken securityToken,
            @RequestParam(value = "reservationRequestId") String reservationRequestId)
    {
        reservationService.confirmReservationRequest(securityToken, reservationRequestId, true);
    }

    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DENY, method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public void handleReservationRequestDeny(
            SecurityToken securityToken,
            @RequestParam(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "reason", required = false) String reason)
    {
        reservationService.denyReservationRequest(securityToken, reservationRequestId, reason);
    }
}
