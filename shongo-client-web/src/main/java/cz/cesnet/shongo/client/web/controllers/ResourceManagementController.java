package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.PageNotAuthorizedException;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.editors.*;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Set;


/**
 *
 * Controller for managing resources and its capabilities.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
@Controller
@SessionAttributes({"resource"})
public class ResourceManagementController {

    private static Logger logger = LoggerFactory.getLogger(ResourceManagementController.class);

    @javax.annotation.Resource
    protected ResourceService resourceService;

    @javax.annotation.Resource
    protected AuthorizationService authorizationService;

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
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
        binder.registerCustomEditor(DateTimeZone.class, new DateTimeZoneEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(LocalTime.class, new LocalTimeEditor());
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_DETAIL, method = RequestMethod.GET)
    public ModelAndView handleResourceDetailPreview(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId) {
        Resource resource = resourceService.getResource(securityToken, resourceId);
        ResourceModel resourceModel = new ResourceModel(resource);
        ModelAndView modelAndView = new ModelAndView("resourceDetail");
        modelAndView.addObject("resource", resourceModel);
        return modelAndView;
    }


    @RequestMapping(value = ClientWebUrl.RESOURCE_MAINTENANCE_RESERVATION, method = RequestMethod.GET)
    public ModelAndView handleResourceMaintenanceReservation(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId) {
        AclEntryListRequest listRequest = new AclEntryListRequest(securityToken);
        listRequest.addObjectId(resourceId);
        listRequest.addRole(ObjectRole.OWNER);
        if (authorizationService.listAclEntries(listRequest).getCount() == 0) {
            throw new PageNotAuthorizedException();
        }
        ResourceSummary resource = cache.getResourceSummary(securityToken, resourceId);

        MaintenanceReservationModel maintenanceReservation = new MaintenanceReservationModel();
        maintenanceReservation.setResourceId(resource.getId());
        maintenanceReservation.setPriority(1);
        maintenanceReservation.setStartDate(LocalDate.now());
        maintenanceReservation.setEndDate(LocalDate.now());
        ModelAndView modelAndView = new ModelAndView("maintenanceReservation");
        modelAndView.addObject("resourceName", resource.getName());
        modelAndView.addObject("maintenanceReservation", maintenanceReservation);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_MAINTENANCE_RESERVATION, method = RequestMethod.POST)
    public Object handleResourceMaintenanceReservationPost(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            @ModelAttribute("maintenanceReservation") MaintenanceReservationModel maintenanceReservationModel,
            BindingResult bindingResult) {
        AclEntryListRequest listRequest = new AclEntryListRequest(securityToken);
        listRequest.addObjectId(resourceId);
        listRequest.addRole(ObjectRole.OWNER);
        MaintenanceReservationValidator validator = new MaintenanceReservationValidator();
        validator.validate(maintenanceReservationModel, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            ModelAndView modelAndView = new ModelAndView("maintenanceReservation");
            modelAndView.addObject("errors", bindingResult);
            modelAndView.addObject("maintenanceReservation", maintenanceReservationModel);
            return modelAndView;
        }
        String reservationRequestId = reservationService.createReservationRequest(securityToken, maintenanceReservationModel.toApi());
        //redirect to reservationDetail
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId);
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_ATTRIBUTES, method = RequestMethod.POST)
    public Object handleResourceAttributesPost (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resourceModel,
            SessionStatus sessionStatus,
            BindingResult bindingResult)  {

        ResourceValidator validator = new ResourceValidator();
        validator.validate(resourceModel, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            ModelAndView modelAndView = new ModelAndView("resourceAttributes");
            modelAndView.addObject("errors", bindingResult);
            modelAndView.addObject("resource", resourceModel);
            modelAndView.addObject("resourceTypes", ResourceType.values());
            return modelAndView;
        }
        String resourceId = resourceModel.getId();
        if (resourceId != null) {
            resourceService.modifyResource(securityToken, resourceModel.toApi());
        } else {
            resourceId = resourceService.createResource(securityToken, resourceModel.toApi());
        }
        sessionStatus.setComplete();

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_DETAIL, resourceId);
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleResourceAttributes (
            UserSession userSession,
            @ModelAttribute("resource") ResourceModel resourceModel) {
        ModelAndView modelAndView = new ModelAndView("resourceAttributes");
        modelAndView.addObject("resourceTypes", ResourceType.values());
        modelAndView.addObject("resource", resourceModel);
        modelAndView.addObject("administrationMode", userSession.isAdministrationMode());
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_NEW, method = RequestMethod.GET)
    public String handleCreateNewResource (
            RedirectAttributes redirectAttributes,
            UserSession userSession) {
        if (!userSession.isAdministrationMode()) {
            throw new PageNotAuthorizedException();
        }
        ResourceModel resource = new ResourceModel();
        resource.setType(ResourceType.RESOURCE);
        redirectAttributes.addFlashAttribute("resource", resource);
        return "redirect:" + ClientWebUrl.RESOURCE_ATTRIBUTES;
    }

    /**
     * Handle resource modification request
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_MODIFY, method = RequestMethod.GET)
    public String handleResourceModify (
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            RedirectAttributes redirectAttributes,
            UserSession userSession) {
        Set<ObjectPermission> permissions = cache.getSingleResourcePermissions(securityToken, resourceId);
        //if user does not have administration mode on and does not have the permission to write, throw exception
        if (!userSession.isAdministrationMode()) {
            if (!permissions.contains(ObjectPermission.WRITE)) {
                throw new PageNotAuthorizedException();
            }
        }

        Resource resource = resourceService.getResource(securityToken, resourceId);
        ResourceModel resourceModel = new ResourceModel(resource);

        redirectAttributes.addFlashAttribute("resource", resourceModel);

        return "redirect:" + ClientWebUrl.RESOURCE_ATTRIBUTES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_SINGLE_DELETE, method = RequestMethod.GET)
    public ModelAndView handleResourceDelete(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId) {
        ResourceSummary resource =  cache.getResourceSummary(securityToken, resourceId);
        ModelAndView modelAndView = new ModelAndView("resourceDelete");
        modelAndView.addObject("resourceName", resource.getName());
        modelAndView.addObject("resourceDescription", resource.getDescription());
        modelAndView.addObject("resourceId", resourceId);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_SINGLE_DELETE, method = RequestMethod.POST)
    public String handleResourceDelete(
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            RedirectAttributes redirectAttributes) {
        try {
            resourceService.deleteResource(securityToken, resourceId);
        } catch (ControllerReportSet.SecurityNotAuthorizedException ex) {
            throw new PageNotAuthorizedException();
        } catch (CommonReportSet.ObjectNotDeletableReferencedException ex) {
             redirectAttributes.addFlashAttribute("error", "views.resourceManagement.resourceStillReferenced");
        }

        return "redirect:" + ClientWebUrl.RESOURCE_RESOURCES;
    }

    /**
     *  Handles capabilities view. Accessible only in administrator's mode.
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES, method = RequestMethod.GET)
    public ModelAndView handleResourceCapabilitiesView (
            SecurityToken securityToken,
            @ModelAttribute(value = "resource") ResourceModel resource,
            UserSession userSession) {
        //Capabilities management accessible only for administrators
        if (!userSession.isAdministrationMode()) {
            throw new PageNotAuthorizedException();
        }
        resource = new ResourceModel(resourceService.getResource(securityToken, resource.getId()));
        ModelAndView modelAndView = new ModelAndView("capabilities");
        Boolean isDeviceResource = Boolean.valueOf(resource.getType().equals(ResourceType.DEVICE_RESOURCE) );
        modelAndView.addObject("aliasTypes", AliasType.values());
        modelAndView.addObject("resource", resource);
        modelAndView.addObject("isDeviceResource", isDeviceResource);
        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES_FINISH, method = RequestMethod.GET)
    public String handleResourceCapabilitiesFinish (
            SessionStatus sessionStatus,
            @ModelAttribute(value = "resource") ResourceModel resource
    ) {
        String resourceId = resource.getId();
        sessionStatus.setComplete();
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_DETAIL, resourceId);
    }


            @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITY_DELETE, method = RequestMethod.GET)
    public String handleResourceCapabilityDelete (
            SecurityToken securityToken,
            @PathVariable(value = "capabilityId") String capabilityId,
            @ModelAttribute("resource") ResourceModel resource) {
        resource.removeCapabilityById(capabilityId);
        resourceService.modifyResource(securityToken, resource.toApi());
        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;
    }

    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES + "/recording", method = RequestMethod.POST)
    public String handleResourceAddRecordingCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("recordingcapability") RecordingCapabilityModel recordingCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(recordingCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.RESOURCE_CAPABILITIES;
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
