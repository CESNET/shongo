package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for managing rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({RoomController.PARTICIPANT_ATTRIBUTE})
public class RoomController
{
    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    @Resource
    private ReservationService reservationService;

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
            item.put("isDeprecated", slot.getEnd().isBeforeNow());

            Integer licenseCount = executableSummary.getRoomUsageLicenseCount();
            if (licenseCount == null) {
                licenseCount = executableSummary.getRoomLicenseCount();
            }
            item.put("licenseCount", licenseCount);
            item.put("licenseCountMessage", messageSource.getMessage(
                    "views.roomList.room.usage.participant", new Object[]{licenseCount}, locale));

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

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT, method = RequestMethod.GET)
    public String handleRoomManagement(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String executableId,
            Model model)
    {
        // Get room executable
        Executable executable = getExecutable(securityToken, executableId);
        RoomExecutable roomExecutable = getPrimaryRoomFromExecutable(securityToken, executable);

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
                    for (RoomParticipant roomParticipant : resourceControlService.listRoomParticipants(securityToken,
                            resourceId, roomId)) {
                        UserInformation userInformation = null;
                        String userId = roomParticipant.getUserId();
                        if (userId != null) {
                            userInformation = cache.getUserInformation(securityToken, userId);
                        }
                        Map<String, Object> participant = new HashMap<String, Object>();
                        participant.put("user", userInformation);
                        participant.put("name",
                                (userInformation != null ? userInformation.getFullName() : roomParticipant.getDisplayName()));
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

    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANTS, method = RequestMethod.GET)
    public String handleRoomParticipants(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String executableId,
            Model model)
    {
        // Get room executable
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, executableId);

        // Room model
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProvider(
                messageSource, userSession.getLocale(), userSession.getTimeZone());
        RoomModel roomModel = new RoomModel(
                roomExecutable, cacheProvider, messageProvider, executableService, userSession);
        roomModel.disableDependentParticipants();
        model.addAttribute("room", roomModel);
        return "roomParticipants";
    }

        /**
         * Show form for adding new participant for ad-hoc/permanent room.
         */
    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @PathVariable("roomId") String roomId)
    {
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, new ParticipantModel(new CacheProvider(cache, securityToken)));
        return modelAndView;
    }

    /**
     * Store new {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public String handleParticipantCreateProcess(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable("roomId") String roomId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            return "participant";
        }
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        participantConfiguration.addParticipant(participant.toApi());
        executableService.modifyExecutableConfiguration(securityToken, roomId, participantConfiguration);
        return "redirect:" + BackUrl.getInstance(request).getUrl(
                ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId));
    }

    /**
     * Show form for modifying existing participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            SecurityToken securityToken,
            @PathVariable("roomId") String roomId,
            @PathVariable("participantId") String participantId)
    {
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        ParticipantModel participant = getParticipant(participantConfiguration, participantId, securityToken);
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        return modelAndView;
    }

    /**
     * Store changes for existing {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public String handleParticipantModifyProcess(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable("roomId") String roomId,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            return "participant";
        }
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        ParticipantModel oldParticipant = getParticipant(participantConfiguration, participantId, securityToken);
        participantConfiguration.removeParticipantById(oldParticipant.getId());
        participantConfiguration.addParticipant(participant.toApi());
        executableService.modifyExecutableConfiguration(securityToken, roomId, participantConfiguration);
        return "redirect:" + BackUrl.getInstance(request).getUrl(
                ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId));
    }

    /**
     * Delete existing {@code participant} from reservation request.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public String handleParticipantDelete(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable("roomId") String roomId,
            @PathVariable("participantId") String participantId)
    {
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();
        ParticipantModel oldParticipant = getParticipant(participantConfiguration, participantId, securityToken);
        participantConfiguration.removeParticipantById(oldParticipant.getId());
        executableService.modifyExecutableConfiguration(securityToken, roomId, participantConfiguration);
        return "redirect:" + BackUrl.getInstance(request).getUrl(
                ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId));
    }

    @RequestMapping(value = ClientWebUrl.ROOM_ENTER, method = RequestMethod.GET)
    public String handleRoomEnter(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId)
    {
        Executable executable = getExecutable(securityToken, roomId);
        RoomExecutable roomExecutable = getPrimaryRoomFromExecutable(securityToken, executable);
        Alias adobeConnectUrl = roomExecutable.getAliasByType(AliasType.ADOBE_CONNECT_URI);
        if (adobeConnectUrl == null) {
            throw new UnsupportedApiException(roomExecutable.getId());
        }
        return "redirect:" + adobeConnectUrl.getValue();
    }

    private Executable getExecutable(SecurityToken securityToken, String executableId)
    {
        Executable executable;
        if (executableId.contains(":rsv:")) {
            Reservation reservation = reservationService.getReservation(securityToken, executableId);
            executable = reservation.getExecutable();
            if (executable == null) {
                throw new UnsupportedApiException("Reservation " + executableId + " doesn't have executable.");
            }
        }
        else {
            executable = executableService.getExecutable(securityToken, executableId);
        }

        return executable;
    }

    private RoomExecutable getPrimaryRoomFromExecutable(SecurityToken securityToken, Executable executable)
    {
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
        return roomExecutable;
    }

    private AbstractRoomExecutable getRoomExecutable(SecurityToken securityToken, String executableId)
    {
        Executable executable = executableService.getExecutable(securityToken, executableId);
        if (executable instanceof AbstractRoomExecutable) {
            return (AbstractRoomExecutable) executable;
        }
        else {
            throw new UnsupportedApiException(executable);
        }
    }

    private ParticipantModel getParticipant(RoomExecutableParticipantConfiguration participantConfiguration,
            String participantId, SecurityToken securityToken)
    {
        AbstractParticipant participant = participantConfiguration.getParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant " + participantId + " doesn't exist.");
        }
        return new ParticipantModel(participant, new CacheProvider(cache, securityToken));
    }
}
