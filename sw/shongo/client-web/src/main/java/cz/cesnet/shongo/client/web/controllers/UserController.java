package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for retrieving {@link UserInformation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/user")
public class UserController
{
    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    @RequestMapping(value = {"", "list"}, method = RequestMethod.GET)
    @ResponseBody
    public List<UserInformation> getAcl(
            SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter)
    {
        // List reservation requests
        UserListRequest request = new UserListRequest();
        request.setSecurityToken(securityToken);
        request.setFilter(filter);
        ListResponse<UserInformation> response = authorizationService.listUsers(request);
        return response.getItems();
    }

    @RequestMapping(value = "/user/{userId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public UserInformation getUser(
            SecurityToken securityToken,
            @PathVariable(value = "userId") String userId)
    {
        return cache.getUserInformation(securityToken, userId);
    }
}
