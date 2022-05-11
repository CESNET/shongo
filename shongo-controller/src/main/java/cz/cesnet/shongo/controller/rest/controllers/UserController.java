package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.request.GroupListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.models.users.SettingsModel;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for user endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final AuthorizationService authorizationService;
    private final Cache cache;

    public UserController(@Autowired AuthorizationService reservationService, @Autowired Cache cache) {
        this.authorizationService = reservationService;
        this.cache = cache;
    }

    /**
     * Handle request for list of {@link UserInformation}s which contains given {@code filter} text in any field.
     *
     * @param filter
     * @return list of {@link UserInformation}s
     */
    @Operation(summary = "Lists users.")
    @GetMapping("/users")
    public ListResponse<UserInformation> getUsers(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "groupId", required = false) String groupId)
    {
        UserListRequest request = new UserListRequest();
        request.setSecurityToken(securityToken);
        request.setSearch(filter);
        if (groupId != null) {
            request.addGroupId(groupId);
        }

        return authorizationService.listUsers(request);
    }

    /**
     * Handle request for {@link UserInformation} by given {@code userId}.
     *
     * @param userId
     * @return {@link UserInformation}
     */
    @Operation(summary = "Returns information about user.")
    @GetMapping("/users/{userId:.+}")
    public UserInformation getUser(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String userId)
    {
        return cache.getUserInformation(securityToken, userId);
    }

    /**
     * Returns user settings with user's system permissions by given {@link SecurityToken}.
     *
     * @return {@link SettingsModel}
     */
    @Operation(summary = "Returns user's settings.")
    @GetMapping("/settings")
    public SettingsModel getUserSettings(@RequestAttribute(TOKEN) SecurityToken securityToken)
    {
        UserSettings settings = authorizationService.getUserSettings(securityToken);
        List<SystemPermission> permissions = cache.getSystemPermissions(securityToken);
        return new SettingsModel(settings, permissions);
    }

    /**
     * Handle request for updating user's settings.
     *
     * @param newSettings new settings of user
     */
    @Operation(summary = "Updates user's settings.")
    @PutMapping("/settings")
    public SettingsModel updateUserSettings(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestBody UserSettings newSettings)
    {
        UserSettings userSettings = authorizationService.getUserSettings(securityToken);

        boolean useWebService = newSettings.isUseWebService();
        userSettings.setUseWebService(newSettings.isUseWebService());
        if (!useWebService) {
            userSettings.setLocale(newSettings.getLocale());
            userSettings.setHomeTimeZone(newSettings.getHomeTimeZone());
        }
        userSettings.setCurrentTimeZone(newSettings.getCurrentTimeZone());

        authorizationService.updateUserSettings(securityToken, userSettings);
        cache.clearUserPermissions(securityToken);

        return getUserSettings(securityToken);
    }

    /**
     * Handle request for list of {@link Group}s which contains given {@code filter} text in any field.
     *
     * @param filter
     * @return list of {@link Group}s
     */
    @Operation(summary = "Lists groups.")
    @GetMapping("/groups")
    public ListResponse<Group> getGroups(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter)
    {
        GroupListRequest request = new GroupListRequest();
        request.setSecurityToken(securityToken);
        request.setSearch(filter);

        return authorizationService.listGroups(request);
    }

    /**
     * Handle request for {@link Group} by given {@code groupId}.
     *
     * @param groupId
     * @return {@link Group}
     */
    @Operation(summary = "Returns information about group.")
    @GetMapping("/groups/{groupId:.+}")
    public Group getGroup(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String groupId)
    {
        return cache.getGroup(securityToken, groupId);
    }
}
