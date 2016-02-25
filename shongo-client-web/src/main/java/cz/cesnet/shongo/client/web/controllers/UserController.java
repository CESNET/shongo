package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.UserSession;
import cz.cesnet.shongo.client.web.models.UserSettingsModel;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeZoneEditor;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.request.GroupListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Controller for retrieving {@link UserInformation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"userSettings"})
public class UserController
{
    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTimeZone.class, new DateTimeZoneEditor());
    }

    /**
     * Handle user settings.
     */
    @RequestMapping(value = ClientWebUrl.USER_SETTINGS, method = RequestMethod.GET)
    public String handleUserSettings(
            Locale locale,
            SecurityToken securityToken,
            Model model)
    {
        UserSettings userSettings = authorizationService.getUserSettings(securityToken);
        model.addAttribute("userSettings", new UserSettingsModel(userSettings));
        return "userSettings";
    }

    /**
     * Handle updating of single user settings attribute.
     */
    @RequestMapping(value = ClientWebUrl.USER_SETTINGS_ATTRIBUTE, method = RequestMethod.GET)
    public String handleUserSettingsAttribute(
            SecurityToken securityToken,
            HttpServletRequest request,
            @PathVariable(value = "name") String name,
            @PathVariable(value = "value") String value)
    {
        UserSettingsModel userSettings = new UserSettingsModel(authorizationService.getUserSettings(securityToken));
        if (name.equals("userInterface")) {
            userSettings.setUserInterface(UserSettingsModel.UserInterface.valueOf(value));
            userSettings.setUserInterfaceSelected(true);
        }
        else if (name.equals("localeDefaultWarning")) {
            userSettings.setLocaleDefaultWarning(Boolean.parseBoolean(value));
        }
        else if (name.equals("timeZoneDefaultWarning")) {
            userSettings.setTimeZoneDefaultWarning(Boolean.parseBoolean(value));
        }
        else if (name.equals("administrationMode")) {
            userSettings.setAdministrationMode(Boolean.parseBoolean(value));
        }
        else {
            throw new IllegalArgumentException(name);
        }
        authorizationService.updateUserSettings(securityToken, userSettings.toApi());
        cache.clearUserPermissions(securityToken);
        UserSession userSession = UserSession.getInstance(request);
        userSession.loadUserSettings(userSettings, request, securityToken);
        return "redirect:" + BackUrl.getInstance(request);
    }

    /**
     * Handle saving the user settings.
     */
    @RequestMapping(value = ClientWebUrl.USER_SETTINGS, method = {RequestMethod.POST})
    public String handleUserSettingsSave(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            HttpServletRequest request,
            @ModelAttribute("userSettings") UserSettingsModel userSettings)
    {

        authorizationService.updateUserSettings(securityToken, userSettings.toApi());
        cache.clearUserPermissions(securityToken);
        if (userSettings.isUseWebService()) {
            // Reload user settings (some attributes may be loaded from web service)
            userSettings.fromApi(authorizationService.getUserSettings(securityToken));
        }
        sessionStatus.setComplete();
        UserSession userSession = UserSession.getInstance(request);
        userSession.loadUserSettings(userSettings, request, securityToken);
        return "redirect:" + BackUrl.getInstance(request);
    }

    /**
     * Handle data request for web service user settings.
     *
     * @return map with web service user settings
     */
    @RequestMapping(value = ClientWebUrl.USER_SETTINGS_WEB_SERVICE_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> handleUserList(
            SecurityToken securityToken)
    {
        UserSettings userSettings = authorizationService.getUserSettings(securityToken, true);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("locale", userSettings.getLocale());
        if (userSettings.getHomeTimeZone() != null) {
            result.put("homeTimeZone", userSettings.getHomeTimeZone().getID());
        }
        return result;
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.USER_SETTINGS + ".", exception);
        return "redirect:" + ClientWebUrl.HOME;
    }

    /**
     * Handle data request for list of {@link UserInformation}s which contains given {@code filter} text in any field.
     *
     * @param filter
     * @param userId
     * @return list of {@link UserInformation}s
     */
    @RequestMapping(value = ClientWebUrl.USER_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public List<UserInformation> handleUserList(
            SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "groupId", required = false) String groupId)
    {
        if (userId != null) {
            List<UserInformation> users = new LinkedList<UserInformation>();
            users.add(cache.getUserInformation(securityToken, userId));
            return users;
        }
        else {
            UserListRequest request = new UserListRequest();
            request.setSecurityToken(securityToken);
            request.setSearch(filter);
            if (groupId != null) {
                request.addGroupId(groupId);
            }
            ListResponse<UserInformation> response = authorizationService.listUsers(request);
            return response.getItems();
        }
    }

    @RequestMapping(value = ClientWebUrl.USER_TOKEN, method = RequestMethod.GET)
    @ResponseBody
    public String handleUserToken(SecurityToken securityToken, HttpServletResponse response)
    {
        return securityToken.getAccessToken();
    }

    /**
     * Handle data request for list of {@link Group}s which contains given {@code filter} text in any field.
     *
     * @param filter
     * @param groupId
     * @return list of {@link Group}s
     */
    @RequestMapping(value = ClientWebUrl.GROUP_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public List<Group> handleGroupList(
            SecurityToken securityToken,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "groupId", required = false) String groupId)
    {
        if (groupId != null) {
            List<Group> users = new LinkedList<Group>();
            users.add(cache.getGroup(securityToken, groupId));
            return users;
        }
        else {
            GroupListRequest request = new GroupListRequest();
            request.setSecurityToken(securityToken);
            request.setSearch(filter);
            ListResponse<Group> response = authorizationService.listGroups(request);
            return response.getItems();
        }
    }
}
