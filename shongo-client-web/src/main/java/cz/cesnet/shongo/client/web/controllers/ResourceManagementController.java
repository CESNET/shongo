package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ResourceModel;
import cz.cesnet.shongo.client.web.models.ResourceType;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Created by Marek Perichta.
 */
@Controller
public class ResourceManagementController {

    @javax.annotation.Resource
    protected ResourceService resourceService;

    @javax.annotation.Resource
    protected AuthorizationService authorizationService;

    @javax.annotation.Resource
    protected Cache cache;

    @RequestMapping(value = ClientWebUrl.RESOURCE_ATTRIBUTES, method = RequestMethod.POST)
    public String handleResourceAttributesPost (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resourceModel)
    {
        if (resourceModel.getId() != null) {
            resourceService.modifyResource(securityToken, resourceModel.toApi());
        } else {
            resourceService.createResource(securityToken, resourceModel.toApi());
        }

        return "redirect:" + ClientWebUrl.RESOURCE_RESOURCES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleResourceAttributes (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resourceModel)
    {
        if (resourceModel.getType() == null) {
            resourceModel.setType(ResourceType.RESOURCE);
        }

        ModelAndView modelAndView = new ModelAndView("resourceAttributes");
        modelAndView.addObject("resourceTypes", ResourceType.values());
        modelAndView.addObject("resource", resourceModel);

        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_MODIFY, method = RequestMethod.GET)
    public String handleResourceModify (
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            RedirectAttributes redirectAttributes)
    {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        ResourceModel resourceModel = new ResourceModel(resource);
        redirectAttributes.addFlashAttribute("resource", resourceModel);

        return "redirect:" + ClientWebUrl.RESOURCE_ATTRIBUTES;
    }
}
