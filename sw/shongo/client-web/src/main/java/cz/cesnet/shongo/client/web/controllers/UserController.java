package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Controller for retrieving {@link UserInformation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class UserController
{
    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    /**
     * Handle default user action.
     */
    @RequestMapping(value = ClientWebUrl.USER, method = RequestMethod.GET)
    public String handleDefault()
    {
        return "forward:" + ClientWebUrl.USER_LIST;
    }

    /**
     * Handle data request for list of {@link UserInformation} which contains given {@code filter} text in any field.
     *
     * @param filter
     * @return list of {@link UserInformation}
     */
    @RequestMapping(value = ClientWebUrl.USER_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<UserInformation> handleList(
            SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter)
    {
        UserListRequest request = new UserListRequest();
        request.setSecurityToken(securityToken);
        request.setFilter(filter);
        ListResponse<UserInformation> response = authorizationService.listUsers(request);
        return response.getItems();
    }

    /**
     * Handle data request for {@link UserInformation}.
     *
     * @param userId
     * @return {@link UserInformation} with given {@code userId}
     */
    @RequestMapping(value = ClientWebUrl.USER_DETAIL, method = RequestMethod.GET)
    @ResponseBody
    public UserInformation handleDetail(
            SecurityToken securityToken,
            @PathVariable(value = "userId") String userId)
    {
        return cache.getUserInformation(securityToken, userId);
    }
}
