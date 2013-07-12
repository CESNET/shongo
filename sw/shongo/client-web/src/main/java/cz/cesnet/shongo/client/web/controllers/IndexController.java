package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Index controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class IndexController
{
    @Resource
    private ExecutableService executableService;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle main (index) view.
     */
    @RequestMapping(value = ClientWebUrl.HOME, method = RequestMethod.GET)
    public String handleIndexView(
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes)
    {
        // Redirect authentication requests until the "redirect_uri" is fixed
        if (request.getParameter("code") != null || request.getParameter("error") != null) {
            redirectAttributes.addAllAttributes(request.getParameterMap());
            return "redirect:" + ClientWebUrl.LOGIN;
        }
        if (authentication != null) {
            return "indexAuthenticated";
        }
        else {
            return "indexAnonymous";
        }
    }

    /**
     * Handle data request for list of rooms.
     */
    @RequestMapping(value = ClientWebUrl.ROOMS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomsData(
            Locale locale,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType specificationType)
    {
        ExecutableListRequest request = new ExecutableListRequest();
        request.setSecurityToken(securityToken);
        request.addExecutableClass(RoomExecutable.class);
        request.setStart(start);
        request.setCount(count);
        request.setSort(ExecutableListRequest.Sort.SLOT);
        request.setSortDescending(Boolean.TRUE);
        ListResponse<ExecutableSummary> response = executableService.listExecutables(request);

        // Build response
        DateTimeFormatter dateTimeFormatter = ReservationRequestModel.DATE_TIME_FORMATTER.withLocale(locale);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ExecutableSummary executableSummary : response.getItems()) {
            RoomExecutableSummary roomExecutableSummary = (RoomExecutableSummary) executableSummary;

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", executableSummary.getId());
            item.put("name", roomExecutableSummary.getName());

            ReservationRequestModel.Technology technology =
                    ReservationRequestModel.Technology.find(roomExecutableSummary.getTechnologies());
            if (technology != null) {
                item.put("technology", technology.getTitle());
            }

            Interval slot = roomExecutableSummary.getSlot();
            item.put("slotStart", dateTimeFormatter.print(slot.getStart()));
            item.put("slotEnd", dateTimeFormatter.print(slot.getEnd()));

            Executable.State roomState = roomExecutableSummary.getState();
            String roomStateMessage = messageSource.getMessage(
                    "views.reservationRequest.executableState." + roomState, null, locale);
            String roomStateHelp = messageSource.getMessage(
                    "views.help.reservationRequest.executableState." + roomState, null, locale);
            item.put("state", roomState);
            item.put("stateAvailable", roomState.isAvailable());
            item.put("stateMessage", roomStateMessage);
            item.put("stateHelp", roomStateHelp);

            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }
}
