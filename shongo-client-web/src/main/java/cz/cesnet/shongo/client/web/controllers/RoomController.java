package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.MessageProviderImpl;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for listing rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class RoomController
{
    @Resource
    private ExecutableService executableService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle room list view
     */
    @RequestMapping(value = ClientWebUrl.ROOM_LIST_VIEW, method = RequestMethod.GET)
    public String handleRoomListView()
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
            @RequestParam(value = "room-id", required = false) String roomId,
            @RequestParam(value = "participant-user-id", required = false) String participantUserId)
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
            if (participantUserId != null) {
                request.setRoomLicenseCount(ExecutableListRequest.FILTER_NON_ZERO);
                request.addType(ExecutableSummary.Type.USED_ROOM);
            }
            request.addType(ExecutableSummary.Type.ROOM);
        }
        request.setParticipantUserId(participantUserId);
        ListResponse<ExecutableSummary> response = executableService.listExecutables(request);

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ExecutableSummary executableSummary : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", executableSummary.getId());
            item.put("type", executableSummary.getType());
            item.put("name", executableSummary.getRoomName());
            item.put("description", executableSummary.getRoomDescription());

            TechnologyModel technology =
                    TechnologyModel.find(executableSummary.getRoomTechnologies());
            if (technology != null) {
                item.put("technology", technology);
                item.put("technologyTitle", messageSource.getMessage(technology.getTitleCode(), null, locale));
            }

            RoomState roomState = RoomState.fromRoomState(
                    executableSummary.getState(), executableSummary.getRoomLicenseCount(),
                    executableSummary.getRoomUsageState());
            RoomType roomType = RoomType.fromExecutableSummary(executableSummary);

            item.put("state", roomState);
            item.put("stateAvailable", roomState.isAvailable());
            item.put("stateMessage", roomState.getMessage(messageSource, locale, roomType));
            item.put("stateHelp", roomState.getHelp(messageSource, locale, roomType));

            Interval slot = executableSummary.getRoomUsageSlot();
            if (slot == null) {
                slot = executableSummary.getSlot();
            }
            item.put("slot", formatter.formatInterval(slot));

            boolean isDeprecated;
            switch (roomState) {
                case STARTED:
                case STARTED_AVAILABLE:
                    isDeprecated = false;
                    break;
                default:
                    isDeprecated = slot.getEnd().isBeforeNow();
                    break;
            }
            item.put("isDeprecated", isDeprecated);

            Integer licenseCount = executableSummary.getRoomUsageLicenseCount();
            if (licenseCount == null) {
                licenseCount = executableSummary.getRoomLicenseCount();
            }
            item.put("licenseCount", licenseCount);
            item.put("licenseCountMessage", messageSource.getMessage(
                    "views.room.participants.value", new Object[]{licenseCount}, locale));

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

    /**
     * Handle data request for list of rooms.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomListData(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId)
    {
        String roomId = cache.getExecutableId(securityToken, objectId);
        AbstractRoomExecutable roomExecutable =
                (AbstractRoomExecutable) executableService.getExecutable(securityToken, roomId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("pin", roomExecutable.getPin());
        data.put("adminPin", roomExecutable.getAdminPin());
        data.put("guestPin", roomExecutable.getGuestPin());
        data.put("allowGuests", roomExecutable.getAllowGuests());
        data.put("aliases", RoomModel.formatAliasesDescription(roomExecutable.getAliases(),
                roomExecutable.getState().isAvailable(), new MessageProviderImpl(messageSource, userSession.getLocale())));
        return data;
    }
}
