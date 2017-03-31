package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Marek Perichta <mperichta@cesnet.cz>
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

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES, method = RequestMethod.GET)
    public ModelAndView handleResourceCapabilitiesView (
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId
    )
    {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        List<Capability> capabilities = resource.getCapabilities();

        ModelAndView modelAndView = new ModelAndView("capabilities");


        modelAndView.addObject("aliasTypes", AliasType.values());
        modelAndView.addObject("capabilities", capabilities);
        modelAndView.addObject("resourceId", resourceId);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/recording", method = RequestMethod.POST)
    public String handleResourceAddRecordingCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("recordingcapability") RecordingCapability recordingCapability,
            BindingResult bindingResult
    )
    {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        resource.addCapability(recordingCapability);
        resourceService.modifyResource(securityToken, resource);

        return "redirect:/resource/" + resourceId + "/capabilities";
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/terminal", method = RequestMethod.POST)
    public String handleResourceAddTerminalCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("terminalcapability") TerminalCapability terminalCapability,
            BindingResult bindingResult
    )
    {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        resource.addCapability(terminalCapability);
        resourceService.modifyResource(securityToken, resource);

        return "redirect:/resource/" + resourceId + "/capabilities";

    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/streaming", method = RequestMethod.POST)
    public String handleResourceAddStreamingCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("streamingcapability") StreamingCapability streamingCapability,
            BindingResult bindingResult
            )
    {

        if (streamingCapability != null)
            System.out.println(streamingCapability);

        return "redirect:/resource/" + resourceId + "/capabilities";

    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/valueProvider", method = RequestMethod.POST)
    public String handleResourceAddValueProviderCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("valueprovidercapability") ValueProviderCapabilityModel valueProviderCapabilityModel,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    )
    {
        if (valueProviderCapabilityModel != null) {

            ValueProviderCapability valueProviderCapability = new ValueProviderCapability();
            switch (valueProviderCapabilityModel.getValueProviderType()) {
                case "pattern":
                    ValueProvider.Pattern patternValueProvider = new ValueProvider.Pattern();
                    patternValueProvider.setAllowAnyRequestedValue(valueProviderCapabilityModel.getAllowAnyRequestedValue());
                    //add patterns
                    for (String pattern : valueProviderCapabilityModel.getPatterns()) {
                        patternValueProvider.addPattern(pattern);
                    }
                    valueProviderCapability.setValueProvider(patternValueProvider);
                    break;

                case "filtered":
                    ValueProvider.Filtered filteredValueProvider = new ValueProvider.Filtered();
                    filteredValueProvider.setValueProvider(valueProviderCapabilityModel.getFilteredResourceId());
                    filteredValueProvider.setFilterType(FilterType.CONVERT_TO_URL);
                    valueProviderCapability.setValueProvider(filteredValueProvider);
                    //TODO finish the filter type
                    break;

                default:
                    throw new TodoImplementException();

            }

            //TODO decide how add the capability, but create function for that
            Resource resource = resourceService.getResource(securityToken, resourceId);
            resource.addCapability(valueProviderCapability);
            resourceService.modifyResource(securityToken, resource);

        }

        return "redirect:/resource/" + resourceId + "/capabilities";


    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/roomProvider", method = RequestMethod.POST)
    public String handleResourceAddRoomProviderCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("roomprovidercapability") RoomProviderCapabilityModel roomProviderCapabilityModel,
            BindingResult bindingResult
    )
    {


        if (roomProviderCapabilityModel != null) {
            Resource resource = resourceService.getResource(securityToken, resourceId);
            RoomProviderCapability roomProviderCapability = new RoomProviderCapability();
            roomProviderCapability.setLicenseCount(roomProviderCapabilityModel.getLicenseCount());
            for (AliasType aliasType: roomProviderCapabilityModel.getRequiredAliasTypes()) {
                roomProviderCapability.addRequiredAliasType(aliasType);
            }

            resource.addCapability(roomProviderCapability);
            resourceService.modifyResource(securityToken, resource);
        }

        return "redirect:/resource/" + resourceId + "/capabilities";


    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITES + "/aliasProvider", method = RequestMethod.POST)
    public String handleResourceAddAliasProviderCapability (
            SecurityToken securityToken,
            @PathVariable(value="resourceId") String resourceId,
            @ModelAttribute("aliasprovidercapability") AliasProviderCapabilityModel aliasProviderCapabilityModel,
            BindingResult bindingResult
    )
    {

        Resource resource = resourceService.getResource(securityToken, resourceId);
        resource.addCapability(aliasProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource);

        return "redirect:/resource/" + resourceId + "/capabilities";

    }
}
