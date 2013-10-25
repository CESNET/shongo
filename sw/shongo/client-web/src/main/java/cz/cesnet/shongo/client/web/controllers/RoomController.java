package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.RoomCache;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private static Logger logger = LoggerFactory.getLogger(RoomController.class);

    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    @Resource
    private RoomCache roomCache;

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
            @PathVariable(value = "roomId") String roomId,
            Model model)
    {
        // Get target room executable
        Executable executable = getExecutable(securityToken, roomId);
        RoomExecutable roomExecutable = getTargetRoomFromExecutable(securityToken, executable);
        roomId = roomExecutable.getId();

        // Room model
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProvider(
                messageSource, userSession.getLocale(), userSession.getTimeZone());
        RoomModel roomModel = new RoomModel(
                roomExecutable, cacheProvider, messageProvider, executableService, userSession);
        model.addAttribute("room", roomModel);

        // Runtime room
        if (roomModel.isStarted()) {
            try {
                Room room = roomCache.getRoom(securityToken, roomId);
                model.addAttribute("roomRuntime", room);
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                model.addAttribute("roomNotAvailable", true);
            }
        }

        // Reservation request for room
        String reservationRequestId = cache.getReservationRequestIdByExecutable(securityToken, executable);
        Set<Permission> reservationRequestPermissions = cache.getPermissions(securityToken, reservationRequestId);
        model.addAttribute("reservationRequestId", reservationRequestId);
        model.addAttribute("reservationRequestProvidable",
                reservationRequestPermissions.contains(Permission.PROVIDE_RESERVATION_REQUEST));

        // Add use roles
        // Add user roles
        AclRecordListRequest userRoleRequest = new AclRecordListRequest();
        userRoleRequest.setSecurityToken(securityToken);
        userRoleRequest.addEntityId(reservationRequestId);
        List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();
        for (AclRecord aclRecord : authorizationService.listAclRecords(userRoleRequest)) {
            userRoles.add(new UserRoleModel(aclRecord, cacheProvider));
        }
        model.addAttribute("userRoles", userRoles);

        return "room";
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANTS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomManagementParticipants(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "DATETIME") String sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        List<RoomParticipant> roomParticipants = roomCache.getRoomParticipants(securityToken, roomId);
        int maxIndex = Math.max(0, roomParticipants.size() - 1);
        if (start > maxIndex) {
            start = maxIndex;
        }
        int end = start + count;
        if (end > roomParticipants.size()) {
            end = roomParticipants.size();
        }
        List<Map> items = new LinkedList<Map>();
        for (RoomParticipant roomParticipant : roomParticipants.subList(start, end)) {
            UserInformation user = null;
            String userId = roomParticipant.getUserId();
            if (userId != null) {
                user = cacheProvider.getUserInformation(userId);
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", roomParticipant.getId());
            item.put("name", (user != null ? user.getFullName() : roomParticipant.getDisplayName()));
            ParticipantRole roomParticipantRole = roomParticipant.getRole();
            if (roomParticipantRole != null) {
                item.put("role", messageSource.getMessage("views.participant.role." + roomParticipantRole, null, locale));
            }
            item.put("email", (user != null ? user.getPrimaryEmail() : null));
            item.put("layout", roomParticipant.getLayout());
            item.put("microphoneLevel", roomParticipant.getMicrophoneLevel());
            item.put("audioMuted", roomParticipant.getAudioMuted());
            item.put("videoMuted", roomParticipant.getVideoMuted());
            item.put("videoSnapshot", roomParticipant.isVideoSnapshot());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", start);
        data.put("count", roomParticipants.size());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_RECORDINGS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomManagementRecordings(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "DATETIME") String sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(
                DateTimeFormatter.Type.SHORT, locale, timeZone);
        List<Recording> recordings = roomCache.getRoomRecordings(securityToken, roomId);
        int maxIndex = Math.max(0, recordings.size() - 1);
        if (start > maxIndex) {
            start = maxIndex;
        }
        int end = start + count;
        if (end > recordings.size()) {
            end = maxIndex;
        }
        List<Map> items = new LinkedList<Map>();
        for (Recording recording : recordings.subList(start, end)) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("name", recording.getName());
            item.put("description", recording.getDescription());
            item.put("beginDate", dateTimeFormatter.formatDateTime(recording.getBeginDate()));
            item.put("duration", dateTimeFormatter.formatDuration(recording.getDuration()));
            item.put("url", recording.getUrl());
            item.put("editableUrl", recording.getEditableUrl());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", start);
        data.put("count", recordings.size());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_VIDEO_SNAPSHOT)
    @IgnoreDateTimeZone
    public ResponseEntity<byte[]> handleRoomParticipantVideoSnapshot(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @PathVariable(value = "participantId") String participantId)
    {
        try {
            MediaData participantSnapshot = roomCache.getRoomParticipantSnapshot(securityToken, roomId, participantId);
            if (participantSnapshot != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(participantSnapshot.getType().toString()));
                headers.setCacheControl("no-cache, no-store, must-revalidate");
                headers.setPragma("no-cache");
                return new ResponseEntity<byte[]>(participantSnapshot.getData(), headers, HttpStatus.OK);
            }
        }
        catch (Exception exception) {
            logger.warn("Failed to get participant snapshot", exception);
        }
        return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    @ResponseBody
    public String handleRoomParticipantModify(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @PathVariable(value = "participantId") String participantId,
            @RequestParam(value = "layout", required = false) RoomLayout layout,
            @RequestParam(value = "microphoneLevel", required = false) Integer microphoneLevel,
            @RequestParam(value = "audioMuted", required = false) Boolean audioMuted,
            @RequestParam(value = "videoMuted", required = false) Boolean videoMuted)
    {
        RoomParticipant oldRoomParticipant = roomCache.getRoomParticipant(securityToken, roomId, participantId);
        RoomParticipant roomParticipant = new RoomParticipant(participantId);
        if (layout != null) {
            if (oldRoomParticipant.getLayout() == null) {
                throw new IllegalStateException("Layout is not available.");
            }
            roomParticipant.setLayout(layout);
        }
        if (microphoneLevel != null) {
            roomParticipant.setMicrophoneLevel(microphoneLevel);
        }
        if (audioMuted != null) {
            if (oldRoomParticipant.getAudioMuted() == null) {
                throw new IllegalStateException("Audio muting is not available.");
            }
            roomParticipant.setAudioMuted(audioMuted);
        }
        if (videoMuted != null) {
            if (oldRoomParticipant.getVideoMuted() == null) {
                throw new IllegalStateException("Video muting is not available.");
            }
            roomParticipant.setVideoMuted(videoMuted);
        }
        roomCache.modifyRoomParticipant(securityToken, roomId, roomParticipant);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    @ResponseBody
    public void handleRoomParticipantModifyPost(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @PathVariable(value = "participantId") String participantId,
            @RequestParam(value = "layout", required = false) RoomLayout layout,
            @RequestParam(value = "microphoneLevel", required = false) Integer microphoneLevel,
            @RequestParam(value = "audioMuted", required = false) Boolean audioMuted,
            @RequestParam(value = "videoMuted", required = false) Boolean videoMuted)
    {
        handleRoomParticipantModify(securityToken, roomId, participantId,
                layout, microphoneLevel, audioMuted, videoMuted);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_DISCONNECT, method = RequestMethod.GET)
    public String handleRoomParticipantDisconnect(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @PathVariable(value = "participantId") String participantId)
    {
        roomCache.disconnectRoomParticipant(securityToken, roomId, participantId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_DISCONNECT, method = RequestMethod.POST)
    @ResponseBody
    public void handleRoomParticipantDisconnectPost(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @PathVariable(value = "participantId") String participantId)
    {
        handleRoomParticipantDisconnect(securityToken, roomId, participantId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_PARTICIPANTS, method = RequestMethod.GET)
    public String handleRoomParticipantList(
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
        return "roomParticipantList";
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
        RoomExecutable roomExecutable = getTargetRoomFromExecutable(securityToken, executable);
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

    private RoomExecutable getTargetRoomFromExecutable(SecurityToken securityToken, Executable executable)
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
