package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.PeriodEditor;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
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

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ResourceController
{
    @Resource
    protected ResourceService resourceService;

    @Resource
    protected ReservationService reservationService;

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
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
            resourceListRequest.setCapabilityClass(capabilityType);
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
     * Handle resource reservations table
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_RESERVATIONS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Object handleReservationsData(
            SecurityToken securityToken,
            @RequestParam(value = "resource-id", required = false) String resourceId,
            @RequestParam(value = "interval-from") DateTime intervalFrom,
            @RequestParam(value = "interval-to") DateTime intervalTo)
    {
        ReservationListRequest request = new ReservationListRequest(securityToken);
        request.setSort(ReservationListRequest.Sort.SLOT);
        request.setResourceId(resourceId);
        request.setInterval(new Interval(intervalFrom, intervalTo));
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
}
