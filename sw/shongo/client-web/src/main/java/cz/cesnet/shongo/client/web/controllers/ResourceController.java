package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * Handle resource reservations view
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_RESERVATIONS_VIEW, method = RequestMethod.GET)
    public ModelAndView handleRoomListView(SecurityToken securityToken)
    {
        Map<String, String> resources = new LinkedHashMap<String, String>();
        for (ResourceSummary resourceSummary : resourceService.listResources(securityToken, null)) {
            String resourceId = resourceSummary.getId();
            StringBuilder resourceTitle = new StringBuilder();
            resourceTitle.append("<b>");
            resourceTitle.append(resourceSummary.getName());
            resourceTitle.append("</b>");
            String resourceTechnologies = resourceSummary.getTechnologies();
            if (resourceTechnologies != null && !resourceTechnologies.isEmpty()) {
                resourceTitle.append(" (");
                String[] technologies = resourceTechnologies.split(",");
                for (int index = 0; index < technologies.length; index++) {
                    if (index > 0) {
                        resourceTitle.append(", ");
                    }
                    resourceTitle.append(Technology.valueOf(technologies[index]).getName());
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
    @RequestMapping(value = ClientWebUrl.RESOURCE_RESERVATIONS_TABLE, method = RequestMethod.GET)
    public String handleRoomListTable()
    {
        return "resourceReservationsTable";
    }
}
