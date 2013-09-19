package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomUser;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for managing rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class RoomController
{
    @Resource
    private ExecutableService executableService;

    @Resource
    private ResourceControlService resourceControlService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle room list view
     */
    @RequestMapping(value = ClientWebUrl.ROOM_LIST, method = RequestMethod.GET)
    public String handleRoomList()
    {
        return "roomList";
    }

    /**
     * Handle data request for list of rooms.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomListData(
            Locale locale,
            DateTimeZone timeZone,
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
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
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
            item.put("slot", formatter.formatInterval(slot));

            Integer licenseCount = executableSummary.getRoomUsageLicenseCount();
            if (licenseCount == null) {
                licenseCount = executableSummary.getRoomLicenseCount();
            }
            item.put("licenseCount", licenseCount);

            item.put("usageCount", executableSummary.getRoomUsageCount());

            item.put("isDeprecated", slot.getEnd().isBeforeNow());

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

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT, method = RequestMethod.GET)
    public String handleRoomManagement(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String executableId, Model model)
    {
        // Room executable
        Executable executable = executableService.getExecutable(securityToken, executableId);
        RoomExecutable roomExecutable;
        if (executable instanceof RoomExecutable) {
            roomExecutable = (RoomExecutable) executable;
        }
        else if (executable instanceof UsedRoomExecutable) {
            UsedRoomExecutable usedRoomExecutable = (UsedRoomExecutable) executable;
            Executable usedExecutable = executableService.getExecutable(
                    securityToken, usedRoomExecutable.getRoomExecutableId());
            if (usedExecutable instanceof RoomExecutable) {
                roomExecutable = (RoomExecutable) usedExecutable;
            }
            else {
                throw new UnsupportedApiException(usedExecutable);
            }
        }
        else {
            throw new UnsupportedApiException(executable);
        }

        // Room model
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProvider(
                messageSource, userSession.getLocale(), userSession.getTimeZone());
        RoomModel roomModel = new RoomModel(
                roomExecutable, cacheProvider, messageProvider, executableService, userSession);
        model.addAttribute("room", roomModel);

        // Runtime room
        if (roomModel.isStarted()) {
            String resourceId = roomExecutable.getResourceId();
            String roomId = roomExecutable.getRoomId();
            Set<Technology> technologies = roomExecutable.getTechnologies();

            try {
                Room room = resourceControlService.getRoom(securityToken, resourceId, roomId);
                model.addAttribute("roomRuntime", room);

                Collection<Recording> recordings = null;
                if (technologies.size() == 1 && technologies.contains(Technology.ADOBE_CONNECT)) {
                    recordings = resourceControlService.listRecordings(securityToken, resourceId, roomId);
                }
                model.addAttribute("roomRecordings", recordings);

                if (roomModel.isAvailable()) {
                    Collection<Map> participants = new LinkedList<Map>();
                    for (RoomUser roomUser : resourceControlService.listParticipants(securityToken, resourceId, roomId)) {
                        UserInformation userInformation = null;
                        String userId = roomUser.getUserId();
                        if (userId != null) {
                            userInformation = cache.getUserInformation(securityToken, userId);
                        }
                        Map<String, Object> participant = new HashMap<String, Object>();
                        participant.put("user", userInformation);
                        participant.put("name",
                                (userInformation != null ? userInformation.getFullName() : roomUser.getDisplayName()));
                        participants.add(participant);
                    }
                    model.addAttribute("roomParticipants", participants);
                }
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                model.addAttribute("roomNotAvailable", true);
            }
        }

        // Reservation request for room
        String reservationRequestId = cache.getReservationRequestIdByExecutable(securityToken, executable);
        model.addAttribute("reservationRequestId", reservationRequestId);

        return "room";
    }
}
