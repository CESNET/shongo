package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.ParticipantRole;
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
import cz.cesnet.shongo.controller.EntityPermission;
import cz.cesnet.shongo.controller.ExecutionReportMessages;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
            item.put("name", executableSummary.getRoomName());
            item.put("description", executableSummary.getRoomDescription());

            TechnologyModel technology =
                    TechnologyModel.find(executableSummary.getRoomTechnologies());
            if (technology != null) {
                item.put("technology", technology.getTitle());
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

    /**
     * Handle data request for list of rooms.
     */
    @RequestMapping(value = ClientWebUrl.ROOM_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomListData(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId)
    {
        AbstractRoomExecutable roomExecutable =
                (AbstractRoomExecutable) executableService.getExecutable(securityToken, roomId);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("aliases", RoomModel.formatAliasesDescription(roomExecutable.getAliases(),
                roomExecutable.getState().isAvailable(), new MessageProvider(messageSource, userSession.getLocale())));
        return data;
    }


    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT, method = RequestMethod.GET)
    public ModelAndView handleRoomManagement(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId)
    {
        ModelAndView modelAndView = new ModelAndView("room");

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
        modelAndView.addObject("room", roomModel);

        // Runtime room
        if (roomModel.isStarted()) {
            try {
                Room room = roomCache.getRoom(securityToken, roomId);
                modelAndView.addObject("roomRuntime", room);
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                modelAndView.addObject("roomNotAvailable", true);
            }
        }

        // Reservation request for room
        String reservationRequestId = cache.getReservationRequestIdByExecutable(securityToken, executable);
        Set<EntityPermission> reservationRequestPermissions = cache.getEntityPermissions(securityToken,
                reservationRequestId);
        modelAndView.addObject("reservationRequestId", reservationRequestId);
        modelAndView.addObject("reservationRequestProvidable",
                reservationRequestPermissions.contains(EntityPermission.PROVIDE_RESERVATION_REQUEST));

        // Add use roles
        // Add user roles
        AclRecordListRequest userRoleRequest = new AclRecordListRequest();
        userRoleRequest.setSecurityToken(securityToken);
        userRoleRequest.addEntityId(reservationRequestId);
        List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();
        for (AclRecord aclRecord : authorizationService.listAclRecords(userRoleRequest)) {
            userRoles.add(new UserRoleModel(aclRecord, cacheProvider));
        }
        modelAndView.addObject("userRoles", userRoles);

        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_MODIFY, method = RequestMethod.GET)
    @ResponseBody
    public String handleRoomModify(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "layout", required = false) RoomLayout layout)
    {
        Room room = roomCache.getRoom(securityToken, roomId);
        if (layout != null) {
            if (room.getLayout() == null) {
                throw new IllegalStateException("Layout is not available.");
            }
            room.setLayout(layout);
        }
        roomCache.modifyRoom(securityToken, roomId, room);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_MODIFY, method = RequestMethod.POST)
    @ResponseBody
    public void handleRoomModifyPost(
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "layout", required = false) RoomLayout layout)
    {
        handleRoomModify(securityToken, roomId, layout);
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
                item.put("role",
                        messageSource.getMessage("views.participant.role." + roomParticipantRole, null, locale));
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

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_RECORDING_START, method = RequestMethod.GET)
    public String handleRoomManagementRecordingStart(
            UserSession userSession,
            SecurityToken securityToken,
            Model model,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Object result = null;
        try {
            result = executableService.activateExecutableService(securityToken, executableId, executableServiceId);
        }
        catch (Exception exception) {
            logger.warn("Start recording failed", exception);
        }
        if (Boolean.TRUE.equals(result)) {
            cache.clearExecutable(executableId);
        }
        else {
            Locale locale = userSession.getLocale();
            String errorCode = "views.room.recording.error.startingFailed";
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
                logger.warn("Stop recording failed: {}", executionReport.toString(locale, userSession.getTimeZone()));

                // Detect further error
                Map<String, Object> report = executionReport.getLastReport();
                if (report != null && ExecutionReportMessages.COMMAND_FAILED.equals(report.get("id"))) {
                    Map jadeReport = (Map) report.get("jadeReport");
                    if (jadeReport != null && JadeReportSet.COMMAND_FAILED.equals(jadeReport.get("id"))) {
                        String code = (String) jadeReport.get("code");
                        if (code != null && code.equals("recording-unavailable")) {
                            errorCode = "views.room.recording.error.unavailable";
                        }
                    }
                }
            }
            model.addAttribute("error", messageSource.getMessage(errorCode, null, locale));
        }
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_RECORDING_START, method = RequestMethod.POST)
    @ResponseBody
    public Object handleRoomManagementRecordingStartPost(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Model model = new ExtendedModelMap();
        handleRoomManagementRecordingStart(userSession, securityToken, model, roomId, executableId, executableServiceId);
        if (model.containsAttribute("error")) {
            return new HashMap<String, Object>(model.asMap());
        }
        return null;
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_RECORDING_STOP, method = RequestMethod.GET)
    public String handleRoomManagementRecordingStop(
            UserSession userSession,
            SecurityToken securityToken,
            Model model,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Object result = null;
        try {
            result = executableService.deactivateExecutableService(securityToken, executableId, executableServiceId);
        }
        catch (Exception exception) {
            logger.warn("Stop recording failed", exception);
        }
        if (Boolean.TRUE.equals(result)) {
            cache.clearExecutable(executableId);
        }
        else {
            Locale locale = userSession.getLocale();
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
                logger.warn("Stop recording failed: {}", executionReport.toString(locale, userSession.getTimeZone()));
            }
            model.addAttribute("error", messageSource.getMessage(
                    "views.room.recording.error.stoppingFailed", null, locale));
        }
        cache.clearExecutable(executableId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.ROOM_MANAGEMENT, roomId);
    }

    @RequestMapping(value = ClientWebUrl.ROOM_MANAGEMENT_RECORDING_STOP, method = RequestMethod.POST)
    @ResponseBody
    public Map handleRoomManagementRecordingStopPost(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "roomId") String roomId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Model model = new ExtendedModelMap();
        handleRoomManagementRecordingStop(userSession, securityToken, model, roomId, executableId, executableServiceId);
        if (model.containsAttribute("error")) {
            return new HashMap<String, Object>(model.asMap());
        }
        return null;
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
            @RequestParam(value = "sort", required = false,
                    defaultValue = "START") ExecutableRecordingListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(
                DateTimeFormatter.Type.SHORT, locale, timeZone);

        ExecutableRecordingListRequest request = new ExecutableRecordingListRequest();
        request.setSecurityToken(securityToken);
        request.setExecutableId(roomId);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        ListResponse<Recording> response = executableService.listExecutableRecordings(request);
        List<Map> items = new LinkedList<Map>();
        for (Recording recording : response.getItems()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("name", recording.getName());
            item.put("description", recording.getDescription());
            item.put("beginDate", dateTimeFormatter.formatDateTime(recording.getBeginDate()));
            item.put("duration", dateTimeFormatter.formatRoundedDuration(recording.getDuration()));
            item.put("url", recording.getUrl());
            item.put("editableUrl", recording.getEditableUrl());
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
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();

        // Initialize model from API
        ParticipantConfigurationModel participantConfigurationModel = new ParticipantConfigurationModel();
        for (AbstractParticipant existingParticipant : participantConfiguration.getParticipants()) {
            participantConfigurationModel.addParticipant(new ParticipantModel(existingParticipant, cacheProvider));
        }
        // Modify model
        participantConfigurationModel.addParticipant(participant);
        // Initialize API from model
        participantConfiguration.clearParticipants();
        for (ParticipantModel participantModel : participantConfigurationModel.getParticipants()) {
            participantConfiguration.addParticipant(participantModel.toApi());
        }

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
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        AbstractRoomExecutable roomExecutable = getRoomExecutable(securityToken, roomId);
        RoomExecutableParticipantConfiguration participantConfiguration = roomExecutable.getParticipantConfiguration();

        // Initialize model from API
        ParticipantConfigurationModel participantConfigurationModel = new ParticipantConfigurationModel();
        for (AbstractParticipant existingParticipant : participantConfiguration.getParticipants()) {
            participantConfigurationModel.addParticipant(new ParticipantModel(existingParticipant, cacheProvider));
        }
        // Modify model
        ParticipantModel oldParticipant = getParticipant(participantConfiguration, participantId, securityToken);
        participantConfigurationModel.removeParticipant(oldParticipant);
        participantConfigurationModel.addParticipant(participant);
        // Initialize API from model
        participantConfiguration.clearParticipants();
        for (ParticipantModel participantModel : participantConfigurationModel.getParticipants()) {
            participantConfiguration.addParticipant(participantModel.toApi());
        }


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

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(ControllerReportSet.DeviceCommandFailedException.class)
    public Object handleExceptions(Exception exception, HttpServletResponse response)
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return "roomNotAvailable";
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
                    securityToken, usedRoomExecutable.getReusedRoomExecutableId());
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
