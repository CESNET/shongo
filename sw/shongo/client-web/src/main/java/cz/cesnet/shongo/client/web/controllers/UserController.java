package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.TimeZoneModel;
import cz.cesnet.shongo.client.web.models.UserSession;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeZoneEditor;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.DateTime;
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
import java.util.List;

/**
 * Controller for retrieving {@link UserInformation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"userSettings", "timeZones"})
public class UserController
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestUpdateController.class);

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
     * Handle default user action.
     */
    @RequestMapping(value = ClientWebUrl.USER, method = RequestMethod.GET)
    public String handleDefault()
    {
        return "forward:" + ClientWebUrl.USER_LIST;
    }

    /**
     * Handle user settings.
     */
    @RequestMapping(value = ClientWebUrl.USER_SETTINGS, method = RequestMethod.GET)
    public String handleUserSettings(
            SecurityToken securityToken,
            HttpServletRequest request,
            Model model)
    {
        UserSettings userSettings = authorizationService.getUserSettings(securityToken);
        model.addAttribute("userSettings", userSettings);
        model.addAttribute("timeZones", TimeZoneModel.getTimeZones(DateTime.now()));
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
        UserSettings userSettings = authorizationService.getUserSettings(securityToken);
        if (name.equals("user-interface")) {
            userSettings.setAttribute(UserSession.USER_INTERFACE_SETTINGS_ATTRIBUTE, value);
        }
        else {
            throw new IllegalArgumentException(name);
        }
        authorizationService.updateUserSettings(securityToken, userSettings);
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
            @ModelAttribute("userSettings") UserSettings userSettings)
    {
        authorizationService.updateUserSettings(securityToken, userSettings);
        sessionStatus.setComplete();
        UserSession userSession = UserSession.getInstance(request);
        userSession.loadUserSettings(userSettings, request, securityToken);
        return "redirect:" + BackUrl.getInstance(request);
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
