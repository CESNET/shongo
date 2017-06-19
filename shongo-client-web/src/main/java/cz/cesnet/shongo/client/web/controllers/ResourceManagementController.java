package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWeb;
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

    /**
     * Handle preview of {@link Resource}'s detail.
     *
     * @param securityToken of the user requesting action
     * @param resourceId to show detail of
     * @return view of the resource
     */
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

    /**
     * Handle view of formula for maintenance reservation.
     *
     * @param securityToken of the user requesting action
     * @param resourceId for for the requested maintenance reservation
     * @return view of the maintenance reservation request form
     */
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

    /**
     * Validate and create maintenance reservation request.
     *
     * @param securityToken of the user requesting action
     * @param resourceId for the requested maintenance reservation
     * @param maintenanceReservationModel model of the reservation request
     * @param bindingResult
     * @return redirection to reservation request detail
     */
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

    /**
     * Validate and store {@link Resource}.
     *
     * @param securityToken of the user requesting action
     * @param resourceModel to be validated and stored
     * @param sessionStatus to be completed after persisting
     * @return redirection to the detail of persisted resource
     */
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

    /**
     * Show form for modified resource.
     *
     * @param resourceModel to be modified
     * @return view of the form for resource
     */
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

    /**
     * Handle request for new resource.
     *
     * @return redirection to the form with new resource
     */
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
     * Handle resource modification request.
     * Retrieves resource from service.
     *
     * @param securityToken of the user requesting action
     * @param resourceId to be modified
     * @return redirection to form filled with resource attributes
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

    /**
     * Handle view of confirmation for deletion of resource.
     *
     * @param securityToken of the user requesting action
     * @param resourceId to be asked for deletion
     * @return view questioning deletion
     */
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

    /**
     * Handle deletion of single resource.
     *
     * @param securityToken of the user requesting action
     * @param resourceId to be deleted
     * @return redirection to view with list of resources and error if catched
     */
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
     * Handle capabilities view. Accessible only in administrator's mode.
     *
     * @param securityToken of the user requesting action
     * @param resourceId of the capabilities shown
     * @return view of capabilities management
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES, method = RequestMethod.GET)
    public ModelAndView handleResourceCapabilitiesView (
            SecurityToken securityToken,
            @PathVariable(value = "resourceId") String resourceId,
            UserSession userSession) {
        //Capabilities management accessible only for administrators
        if (!userSession.isAdministrationMode()) {
            throw new PageNotAuthorizedException();
        }
        ResourceModel resource = new ResourceModel(resourceService.getResource(securityToken, resourceId));
        ModelAndView modelAndView = new ModelAndView("capabilities");
        Boolean isDeviceResource = Boolean.valueOf(resource.getType().equals(ResourceType.DEVICE_RESOURCE) );
        modelAndView.addObject("aliasTypes", AliasType.values());
        modelAndView.addObject("resource", resource);
        modelAndView.addObject("isDeviceResource", isDeviceResource);
        return modelAndView;
    }

    /**
     * Removes model from session context and redirects to detail of the resource
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITIES_FINISH, method = RequestMethod.GET)
    public String handleResourceCapabilitiesFinish (
            SessionStatus sessionStatus,
            @ModelAttribute(value = "resource") ResourceModel resource) {
        String resourceId = resource.getId();
        sessionStatus.setComplete();
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_DETAIL, resourceId);
    }

    /**
     * Removes model from session context and redirects to resource management view.
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CANCEL, method = RequestMethod.GET)
    public String handleResourceCancel (
            SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:" + ClientWebUrl.RESOURCE_RESOURCES;
    }

    /**
     * Handle deletion of single capability.
     * @param securityToken of the user requesting action
     * @param capabilityId to be deleted
     * @param resource from the session context
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_CAPABILITY_DELETE, method = RequestMethod.GET)
    public String handleResourceCapabilityDelete (
            SecurityToken securityToken,
            @PathVariable(value = "capabilityId") String capabilityId,
            @ModelAttribute("resource") ResourceModel resource) {
        resource.removeCapabilityById(capabilityId);
        resourceService.modifyResource(securityToken, resource.toApi());
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());
    }

    /**
     * Handle adding of {@link RecordingCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param recordingCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/recording", method = RequestMethod.POST)
    public String handleResourceAddRecordingCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("recordingcapability") RecordingCapabilityModel recordingCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(recordingCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());
    }

    /**
     * Handle adding of {@link TerminalCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param terminalCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/terminal", method = RequestMethod.POST)
    public String handleResourceAddTerminalCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("terminalcapability") TerminalCapabilityModel terminalCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(terminalCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());

    }

    /**
     * Handle adding of {@link StreamingCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param streamingCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/streaming", method = RequestMethod.POST)
    public String handleResourceAddStreamingCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("streamingcapability") StreamingCapabilityModel streamingCapabilityModel,
            BindingResult bindingResult) {

        resource.addCapability(streamingCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());

    }

    /**
     * Handle adding of {@link ValueProviderCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param valueProviderCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/valueProvider", method = RequestMethod.POST)
    public String handleResourceAddValueProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("valueprovidercapability") ValueProviderCapabilityModel valueProviderCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(valueProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());


    }

    /**
     * Handle adding of {@link RoomProviderCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param roomProviderCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/roomProvider", method = RequestMethod.POST)
    public String handleResourceAddRoomProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("roomprovidercapability") RoomProviderCapabilityModel roomProviderCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(roomProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());

    }

    /**
     * Handle adding of {@link AliasProviderCapability} to resource.
     * @param securityToken of the user requesting action
     * @param resource from the session to add capability to
     * @param aliasProviderCapabilityModel to be added to resource
     * @return redirection to capabilities management of the resource in session
     */
    @RequestMapping(value = ClientWebUrl.RESOURCE_ADD_CAPABILITY + "/aliasProvider", method = RequestMethod.POST)
    public String handleResourceAddAliasProviderCapability (
            SecurityToken securityToken,
            @ModelAttribute("resource") ResourceModel resource,
            @ModelAttribute("AliasProviderCapabilityModel") AliasProviderCapabilityModel aliasProviderCapabilityModel,
            BindingResult bindingResult) {
        resource.addCapability(aliasProviderCapabilityModel.toApi());
        resourceService.modifyResource(securityToken, resource.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESOURCE_CAPABILITIES, resource.getId());

    }

}
