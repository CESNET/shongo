package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.client.web.models.UserRoleModel;
import cz.cesnet.shongo.client.web.models.UserRoleValidator;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for managing ACL for reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"userRole"})
public class UserRoleController
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle list of user roles view.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_LIST, method = RequestMethod.GET)
    public ModelAndView handleListView(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId)
    {
        ModelAndView modelAndView = new ModelAndView("userRoleList");
        modelAndView.addObject("objectId", objectId);
        if (objectId.contains(":req:")) {
            ReservationRequestSummary reservationRequest = cache.getReservationRequestSummary(securityToken, objectId);
            SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
            modelAndView.addObject("headingFor",
                    messageSource.getMessage("views.reservationRequest.for." + specificationType,
                            new Object[]{reservationRequest.getRoomName()}, locale));
        }
        else {
            throw new UnsupportedApiException(objectId);
        }
        return modelAndView;
    }

    /**
     * Handle data request for reservation request {@link UserRoleModel}s.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleListData(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        // List ACL entries
        AclEntryListRequest request = new AclEntryListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addObjectId(objectId);
        ListResponse<AclEntry> response = authorizationService.listAclEntries(request);

        // Build response
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (AclEntry aclEntry : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", aclEntry.getId());
            AclIdentityType identityType = aclEntry.getIdentityType();
            item.put("identityType", identityType);
            String identityPrincipalId = aclEntry.getIdentityPrincipalId();
            item.put("identityPrincipalId", identityPrincipalId);
            switch (identityType) {
                case USER:
                    UserInformation user = cache.getUserInformation(securityToken, identityPrincipalId);
                    item.put("identityName", user.getFullName());
                    item.put("identityDescription", user.getOrganization());
                    item.put("email", user.getPrimaryEmail());
                    break;
                case GROUP:
                    Group group = cache.getGroup(securityToken, identityPrincipalId);
                    item.put("identityName", group.getName());
                    item.put("identityDescription", group.getDescription());
                    break;
                default:
                    throw new UnsupportedApiException(aclEntry.getIdentityType());
            }
            String objectRole = aclEntry.getRole().toString();
            item.put("role", messageSource.getMessage("views.userRole.objectRole." + objectRole, null, locale));
            item.put("deletable", aclEntry.isDeletable());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }

    /**
     * Handle creation of {@link UserRoleModel} for reservation request.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleRoleCreate(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId)
    {
        UserRoleModel userRole = new UserRoleModel(new CacheProvider(cache, securityToken));
        userRole.setObjectId(objectId);
        return handleRoleCreate(userRole);
    }

    /**
     * Handle confirmation of creation of {@link UserRoleModel} for reservation request.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_CREATE, method = RequestMethod.POST)
    public Object handleRoleCreateProcess(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @ModelAttribute("userRole") UserRoleModel userRole,
            BindingResult result)
    {
        if (!userRole.getObjectId().equals(objectId)) {
            throw new IllegalStateException("Acl entry object id doesn't match the reservation request id.");
        }
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, result);
        if (result.hasErrors()) {
            return handleRoleCreate(userRole);
        }
        authorizationService.createAclEntry(securityToken, userRole.toApi());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.USER_ROLE_LIST, objectId);
    }

    /**
     * Handle deletion of {@link UserRoleModel} for reservation request.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_DELETE,
            method = RequestMethod.GET)
    public String handleRoleDelete(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "roleId") String userRoleId,
            Model model)
    {
        AclEntryListRequest request = new AclEntryListRequest();
        request.setSecurityToken(securityToken);
        request.addObjectId(objectId);
        request.addRole(ObjectRole.OWNER);
        ListResponse<AclEntry> aclEntries = authorizationService.listAclEntries(request);
        if (aclEntries.getItemCount() == 1 && aclEntries.getItem(0).getId().equals(userRoleId)) {
            model.addAttribute("title", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.title");
            model.addAttribute("message", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.message");
            return "message";
        }
        authorizationService.deleteAclEntry(securityToken, userRoleId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.USER_ROLE_LIST, objectId);
    }

    /**
     * Handle view for creation of {@link UserRoleModel} for reservation request.
     */
    private ModelAndView handleRoleCreate(UserRoleModel userRole)
    {
        ModelAndView modelAndView = new ModelAndView("userRole");
        modelAndView.addObject("userRole", userRole);
        return modelAndView;
    }
}
