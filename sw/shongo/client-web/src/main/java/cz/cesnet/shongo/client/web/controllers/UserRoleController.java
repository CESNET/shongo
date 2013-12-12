package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.client.web.models.UserRoleModel;
import cz.cesnet.shongo.client.web.models.UserRoleValidator;
import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
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
            @PathVariable(value = "entityId") String entityId)
    {
        ModelAndView modelAndView = new ModelAndView("userRoleList");
        modelAndView.addObject("entityId", entityId);
        if (entityId.contains(":req:")) {
            ReservationRequestSummary reservationRequest = cache.getReservationRequestSummary(securityToken, entityId);
            SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
            modelAndView.addObject("headingFor",
                    messageSource.getMessage("views.reservationRequest.for." + specificationType,
                            new Object[]{reservationRequest.getRoomName()}, locale));
        }
        else {
            throw new UnsupportedApiException(entityId);
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
            @PathVariable(value = "entityId") String entityId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        // List ACL records
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addEntityId(entityId);
        ListResponse<AclRecord> response = authorizationService.listAclRecords(request);

        // Build response
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (AclRecord aclRecord : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", aclRecord.getId());
            item.put("user", cache.getUserInformation(securityToken, aclRecord.getUserId()));
            String entityRole = aclRecord.getEntityRole().toString();
            item.put("entityRole", messageSource.getMessage("views.userRole.entityRole." + entityRole, null, locale));
            item.put("deletable", aclRecord.isDeletable());
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
            @PathVariable(value = "entityId") String entityId)
    {
        UserRoleModel userRole = new UserRoleModel(new CacheProvider(cache, securityToken));
        userRole.setEntityId(entityId);
        return handleRoleCreate(userRole);
    }

    /**
     * Handle confirmation of creation of {@link UserRoleModel} for reservation request.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_CREATE, method = RequestMethod.POST)
    public Object handleRoleCreateProcess(
            SecurityToken securityToken,
            @PathVariable(value = "entityId") String entityId,
            @ModelAttribute("userRole") UserRoleModel userRole,
            BindingResult result)
    {
        if (!userRole.getEntityId().equals(entityId)) {
            throw new IllegalStateException("Acl record entity id doesn't match the reservation request id.");
        }
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, result);
        if (result.hasErrors()) {
            return handleRoleCreate(userRole);
        }
        authorizationService.createAclRecord(securityToken,
                userRole.getUserId(), userRole.getEntityId(), userRole.getEntityRole());

        return "redirect:" + ClientWebUrl.format(ClientWebUrl.USER_ROLE_LIST, entityId);
    }

    /**
     * Handle deletion of {@link UserRoleModel} for reservation request.
     */
    @RequestMapping(value = ClientWebUrl.USER_ROLE_DELETE,
            method = RequestMethod.GET)
    public String handleRoleDelete(
            SecurityToken securityToken,
            @PathVariable(value = "entityId") String entityId,
            @PathVariable(value = "roleId") String userRoleId,
            Model model)
    {
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.addEntityId(entityId);
        request.addEntityRole(EntityRole.OWNER);
        ListResponse<AclRecord> aclRecords = authorizationService.listAclRecords(request);
        if (aclRecords.getItemCount() == 1 && aclRecords.getItem(0).getId().equals(userRoleId)) {
            model.addAttribute("title", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.title");
            model.addAttribute("message", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.message");
            return "message";
        }
        authorizationService.deleteAclRecord(securityToken, userRoleId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.USER_ROLE_LIST, entityId);
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
