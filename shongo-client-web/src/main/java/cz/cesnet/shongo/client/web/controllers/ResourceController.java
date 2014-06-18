package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.resource.ResourceCapacity;
import cz.cesnet.shongo.client.web.resource.ResourceCapacityUtilization;
import cz.cesnet.shongo.client.web.resource.ResourcesUtilization;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.IntervalEditor;
import cz.cesnet.shongo.client.web.support.editors.PeriodEditor;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
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
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    /**
     * Handle resource reservations view
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> handleResourceListData(
            SecurityToken securityToken,
            @RequestParam(value = "capabilityClass", required = false) String capabilityClassName,
            @RequestParam(value = "technology", required = false) TechnologyModel technology)
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
        List<Map<String, Object>> resources = new LinkedList<Map<String, Object>>();
        for (ResourceSummary resourceSummary : resourceService.listResources(resourceListRequest)) {
            Map<String, Object> resource = new HashMap<String, Object>();
            resource.put("id", resourceSummary.getId());
            resource.put("name", resourceSummary.getName());
            resource.put("technology", TechnologyModel.find(resourceSummary.getTechnologies()));
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
}
