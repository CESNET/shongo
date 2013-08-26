package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.CommonModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.RoomState;
import cz.cesnet.shongo.client.web.models.TechnologyModel;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
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
            @RequestParam(value = "sort", required = false, defaultValue = "SLOT") ExecutableListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "room-id", required = false) String roomId)
    {
        ExecutableListRequest request = new ExecutableListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        if (roomId != null) {
            request.addType(ExecutableSummary.Type.USED_ROOM);
            request.setRoomId(roomId);
        }
        else {
            request.addType(ExecutableSummary.Type.ROOM);
        }
        ListResponse<ExecutableSummary> response = executableService.listExecutables(request);

        // Build response
        DateTimeFormatter dateTimeFormatter = CommonModel.DATE_TIME_FORMATTER.withLocale(locale);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ExecutableSummary executableSummary : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", executableSummary.getId());
            item.put("name", executableSummary.getRoomName());

            TechnologyModel technology =
                    TechnologyModel.find(executableSummary.getRoomTechnologies());
            if (technology != null) {
                item.put("technology", technology.getTitle());
            }

            RoomState roomState = RoomState.fromRoomState(
                    executableSummary.getState(), executableSummary.getRoomLicenseCount(), executableSummary.getRoomUsageState());
            String roomStateMessage  = messageSource.getMessage(
                    "views.executable.roomState." + roomState, null, locale);
            String roomStateHelp;
            switch (executableSummary.getType()) {
                case ROOM:
                    roomStateHelp = messageSource.getMessage(
                            "help.executable.roomState." + roomState, null, locale);
                    break;
                case USED_ROOM:
                    roomStateHelp = messageSource.getMessage(
                            "help.executable.roomState.USED_ROOM." + roomState, null, locale);
                    break;
                default:
                    throw new TodoImplementException(executableSummary.getType().toString());
            }

            item.put("state", roomState);
            item.put("stateAvailable", roomState.isAvailable());
            item.put("stateMessage", roomStateMessage);
            item.put("stateHelp", roomStateHelp);

            Interval slot = executableSummary.getRoomUsageSlot();
            if (slot == null) {
                slot = executableSummary.getSlot();
            }
            item.put("slotStart", dateTimeFormatter.print(slot.getStart()));
            item.put("slotEnd", dateTimeFormatter.print(slot.getEnd()));

            Integer licenseCount = executableSummary.getRoomUsageLicenseCount();
            if (licenseCount == null) {
                licenseCount = executableSummary.getRoomLicenseCount();
            }
            item.put("licenseCount", licenseCount);

            item.put("usageCount", executableSummary.getRoomUsageCount());

            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }
}
