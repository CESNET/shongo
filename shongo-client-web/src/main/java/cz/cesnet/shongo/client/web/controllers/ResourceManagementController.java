package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.PageNotAuthorizedException;
import cz.cesnet.shongo.client.web.auth.UserPermission;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


/**
 *
 * Controller for managing resource and its capabilities.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
@Controller
@SessionAttributes({"resource"})
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
            @ModelAttribute("resource") ResourceModel resourceModel,
            SessionStatus sessionStatus)  {

        if (resourceModel.getId() != null) {
            resourceService.modifyResource(securityToken, resourceModel.toApi());
        } else {
            resourceService.createResource(securityToken, resourceModel.toApi());
        }
        sessionStatus.setComplete();

        return "redirect:" + ClientWebUrl.RESOURCE_RESOURCES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleResourceAttributes (
            SecurityToken securityToken,
            UserSession userSession,
            @ModelAttribute("resource") ResourceModel resourceModel) {
        ModelAndView modelAndView = new ModelAndView("resourceAttributes");
        modelAndView.addObject("resourceTypes", ResourceType.values());
        modelAndView.addObject("resource", resourceModel);
        modelAndView.addObject("administrationMode", userSession.getUserSettings().isAdministrationMode());
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_NEW, method = RequestMethod.GET)
    public String handleCreateNewResource (
            SecurityToken securityToken,
            RedirectAttributes redirectAttributes) {
        ResourceModel resource = new ResourceModel();
        resource.setType(ResourceType.RESOURCE);
        redirectAttributes.addFlashAttribute("resource", resource);
        return "redirect:" + ClientWebUrl.RESOURCE_ATTRIBUTES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_MODIFY, method = RequestMethod.GET)
    public String handleResourceModify (
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            RedirectAttributes redirectAttributes) {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        ResourceModel resourceModel = new ResourceModel(resource);

        redirectAttributes.addFlashAttribute("resource", resourceModel);

        return "redirect:" + ClientWebUrl.RESOURCE_ATTRIBUTES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_SINGLE_DELETE, method = RequestMethod.GET)
    public String handleResourceDelete(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId
    ) {
        resourceService.deleteResource(securityToken, resourceId);
        return "redirect:" + ClientWebUrl.RESOURCE_RESOURCES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES, method = RequestMethod.GET)
    public ModelAndView handleResourceCapabilitiesView (
            SecurityToken securityToken,
            @ModelAttribute(value = "resource") ResourceModel resource) {
        //Capabilities management accessible only for administrators
        if (!cache.hasUserPermission(securityToken, UserPermission.ADMINISTRATION)) {
            throw new PageNotAuthorizedException();
        }
        List<Capability> capabilities = resource.getCapabilities();
        ModelAndView modelAndView = new ModelAndView("capabilities");
        Boolean isDeviceResource = Boolean.valueOf(resource.getType().equals(ResourceType.DEVICE_RESOURCE) );

        modelAndView.addObject("aliasTypes", AliasType.values());
        modelAndView.addObject("capabilities", capabilities);
        modelAndView.addObject("isDeviceResource", isDeviceResource);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/recording", method = RequestMethod.POST)
    public String handleResourceAddRecordingCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("recordingcapability") RecordingCapabilityModel recordingCapabilityModel,
            BindingResult bindingResult) {
        //Resource resource = resourceService.getResource(securityToken, resourceId);
        resource.addCapability(recordingCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:/resource" + "/capabilities";
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/terminal", method = RequestMethod.POST)
    public String handleResourceAddTerminalCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("terminalcapability") TerminalCapabilityModel terminalCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(terminalCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;

    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/streaming", method = RequestMethod.POST)
    public String handleResourceAddStreamingCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("streamingcapability") StreamingCapabilityModel streamingCapabilityModel,
            BindingResult bindingResult) {

        resource.addCapability(streamingCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;

    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/valueProvider", method = RequestMethod.POST)
    public String handleResourceAddValueProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("valueprovidercapability") ValueProviderCapabilityModel valueProviderCapabilityModel,
            BindingResult bindingResult) {
            //TODO decide how add the capability, but create function for that

            resource.addCapability(valueProviderCapabilityModel.toApi());
            resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;


    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/roomProvider", method = RequestMethod.POST)
    public String handleResourceAddRoomProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("roomprovidercapability") RoomProviderCapabilityModel roomProviderCapabilityModel,
            BindingResult bindingResult) {


        resource.addCapability(roomProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;

    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/aliasProvider", method = RequestMethod.POST)
    public String handleResourceAddAliasProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("aliasprovidercapability") AliasProviderCapabilityModel aliasProviderCapabilityModel,
            BindingResult bindingResult) {

        resource.addCapability(aliasProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;

    }
}
