package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.RoomCache;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.MessageProviderImpl;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Controller for runtime management of room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailRuntimeManagementController extends AbstractDetailController
{
    private static Logger logger = LoggerFactory.getLogger(DetailRuntimeManagementController.class);

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private RoomCache roomCache;

    /**
     * Handle detail runtime management tab.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_TAB, method = RequestMethod.GET)
    public ModelAndView handleDetailRuntimeManagementTab(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId)
    {
        ModelAndView modelAndView = new ModelAndView("detailRuntimeManagement");

        // Get target room executable
        String executableId = getExecutableId(securityToken, objectId);
        Executable executable = getExecutable(securityToken, executableId);
        RoomExecutable roomExecutable = getTargetRoomExecutableFromExecutable(securityToken, executable);
        executableId = roomExecutable.getId();

        // Room model
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        MessageProvider messageProvider = new MessageProviderImpl(
                messageSource, userSession.getLocale(), userSession.getTimeZone());
        RoomModel roomModel = new RoomModel(
                roomExecutable, cacheProvider, messageProvider, executableService, userSession, true);
        modelAndView.addObject("room", roomModel);

        // Runtime room
        if (roomModel.isAvailable()) {
            try {
                Room room = roomCache.getRoom(securityToken, executableId);
                modelAndView.addObject("roomRuntime", room);
                H323RoomSetting h323RoomSetting = room.getRoomSetting(H323RoomSetting.class);
                if (h323RoomSetting != null) {
                    modelAndView.addObject("contentImportant", h323RoomSetting.getContentImportant());
                }
            }
            catch (ControllerReportSet.DeviceCommandFailedException exception) {
                logger.warn("Room " + executableId +" isn't available", exception);
            }
        }

        // Reservation request for room
        String reservationRequestId = cache.getReservationRequestIdByExecutable(securityToken, executable);
        modelAndView.addObject("reservationRequestId", reservationRequestId);
        modelAndView.addObject("isProvidable", executable instanceof RoomExecutable);

        return modelAndView;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_MODIFY, method = RequestMethod.GET)
    @ResponseBody
    public String handleModify(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "layout", required = false) RoomLayout layout,
            @RequestParam(value = "contentImportant", required = false) Boolean contentImportant)
    {
        String executableId = getExecutableId(securityToken, objectId);
        Room room = roomCache.getRoom(securityToken, executableId);
        if (layout != null) {
            if (room.getLayout() == null) {
                throw new IllegalStateException("Layout is not available.");
            }
            room.setLayout(layout);
        }
        H323RoomSetting h323RoomSetting = room.getRoomSetting(H323RoomSetting.class);
        if (contentImportant != null && h323RoomSetting != null) {
            h323RoomSetting.setContentImportant(contentImportant);
        }
        roomCache.modifyRoom(securityToken, executableId, room);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_MODIFY, method = RequestMethod.POST)
    @ResponseBody
    public void handleModifyPost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "layout", required = false) RoomLayout layout,
            @RequestParam(value = "contentImportant", required = false) boolean contentImportant)
    {
        handleModify(securityToken, objectId, layout, contentImportant);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANTS_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleRoomManagementParticipants(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "DATETIME") String sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        String executableId = getExecutableId(securityToken, objectId);
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        List<RoomParticipant> roomParticipants = Collections.emptyList();
        try {
            roomParticipants = roomCache.getRoomParticipants(securityToken, executableId);
        }
        catch (Exception exception) {
            logger.warn("Failed to load participants", exception);
        }
        ListResponse<RoomParticipant> response = ListResponse.fromRequest(start, count, roomParticipants);
        List<Map> items = new LinkedList<Map>();
        for (RoomParticipant roomParticipant : response.getItems()) {
            UserInformation user = null;
            String userId = roomParticipant.getUserId();
            if (userId != null) {
                user = cacheProvider.getUserInformation(userId);
            }
            Alias alias = roomParticipant.getAlias();
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", roomParticipant.getId());
            item.put("name", (user != null ? user.getFullName() : roomParticipant.getDisplayName()));
            item.put("alias", (alias != null ? alias.getValue() : null));
            ParticipantRole roomParticipantRole = roomParticipant.getRole();
            if (roomParticipantRole != null) {
                item.put("role",
                        messageSource.getMessage("views.participant.role." + roomParticipantRole, null, locale));
            }
            item.put("email", (user != null ? user.getPrimaryEmail() : null));
            item.put("layout", roomParticipant.getLayout());
            item.put("microphoneEnabled", roomParticipant.getMicrophoneEnabled());
            item.put("microphoneLevel", roomParticipant.getMicrophoneLevel());
            item.put("videoEnabled", roomParticipant.getVideoEnabled());
            item.put("videoSnapshot", roomParticipant.isVideoSnapshot());
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

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_VIDEO_SNAPSHOT)
    @IgnoreDateTimeZone
    public ResponseEntity<byte[]> handleRoomParticipantVideoSnapshot(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "participantId") String participantId)
    {
        String executableId = getExecutableId(securityToken, objectId);
        try {
            MediaData participantSnapshot = roomCache.getRoomParticipantSnapshot(
                    securityToken, executableId, participantId);
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

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    @ResponseBody
    public String handleRoomParticipantModify(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "participantId") String participantId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "microphoneEnabled", required = false) Boolean microphoneEnabled,
            @RequestParam(value = "microphoneLevel", required = false) Integer microphoneLevel,
            @RequestParam(value = "videoEnabled", required = false) Boolean videoEnabled)
    {
        String executableId = getExecutableId(securityToken, objectId);
        RoomParticipant oldRoomParticipant = null;
        if (!participantId.equals("*")) {
            oldRoomParticipant = roomCache.getRoomParticipant(securityToken, executableId, participantId);
        }
        RoomParticipant roomParticipant = new RoomParticipant(participantId);
        if (name != null) {
            roomParticipant.setDisplayName(name);
        }
        if (microphoneLevel != null) {
            roomParticipant.setMicrophoneLevel(microphoneLevel);
        }
        if (microphoneEnabled != null) {
            if (oldRoomParticipant != null && oldRoomParticipant.getMicrophoneEnabled() == null) {
                throw new IllegalStateException("Mute microphone is not available.");
            }
            roomParticipant.setMicrophoneEnabled(microphoneEnabled);
        }
        if (videoEnabled != null) {
            if (oldRoomParticipant != null && oldRoomParticipant.getVideoEnabled() == null) {
                throw new IllegalStateException("Disable video is not available.");
            }
            roomParticipant.setVideoEnabled(videoEnabled);
        }
        if (participantId.equals("*")) {
            roomParticipant.setId((String) null);
            roomCache.modifyRoomParticipants(securityToken, executableId, roomParticipant);
        }
        else {
            roomCache.modifyRoomParticipant(securityToken, executableId, roomParticipant);
        }
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    @ResponseBody
    public void handleRoomParticipantModifyPost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "participantId") String participantId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "microphoneEnabled", required = false) Boolean microphoneEnabled,
            @RequestParam(value = "microphoneLevel", required = false) Integer microphoneLevel,
            @RequestParam(value = "videoEnabled", required = false) Boolean videoEnabled)
    {
        handleRoomParticipantModify(
                securityToken, objectId, participantId, name, microphoneEnabled, microphoneLevel, videoEnabled);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_DISCONNECT, method = RequestMethod.GET)
    public String handleRoomParticipantDisconnect(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "participantId") String participantId)
    {
        String executableId = getExecutableId(securityToken, objectId);
        roomCache.disconnectRoomParticipant(securityToken, executableId, participantId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_DISCONNECT, method = RequestMethod.POST)
    @ResponseBody
    public void handleRoomParticipantDisconnectPost(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @PathVariable(value = "participantId") String participantId)
    {
        handleRoomParticipantDisconnect(securityToken, objectId, participantId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_START, method = RequestMethod.GET)
    public String handleRecordingStart(
            UserSession userSession,
            SecurityToken securityToken,
            Model model,
            @PathVariable(value = "objectId") String objectId,
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
        if (Boolean.TRUE.equals(result) || Boolean.FALSE.equals(result)) {
            cache.clearExecutable(executableId);
        }
        else {
            Locale locale = userSession.getLocale();
            String errorCode = "views.room.recording.error.startingFailed";
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
                logger.warn("Start recording failed: {}", executionReport.toString(locale, userSession.getTimeZone()));

                // Detect further error
                ExecutionReport.UserError userError = executionReport.toUserError();
                if (userError instanceof ExecutionReport.RecordingUnavailable) {
                    errorCode = "views.room.recording.error.unavailable";
                }
            }
            model.addAttribute("error", messageSource.getMessage(errorCode, null, locale));
        }
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_START, method = RequestMethod.POST)
    @ResponseBody
    public Object handleRecordingStartPost(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Model model = new ExtendedModelMap();
        handleRecordingStart(userSession, securityToken, model, objectId, executableId, executableServiceId);
        if (model.containsAttribute("error")) {
            return new HashMap<String, Object>(model.asMap());
        }
        return null;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_STOP, method = RequestMethod.GET)
    public String handleRecordingStop(
            UserSession userSession,
            SecurityToken securityToken,
            Model model,
            @PathVariable(value = "objectId") String objectId,
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
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW, objectId);
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_STOP, method = RequestMethod.POST)
    @ResponseBody
    public Map handleRecordingStopPost(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Model model = new ExtendedModelMap();
        handleRecordingStop(userSession, securityToken, model, objectId, executableId, executableServiceId);
        if (model.containsAttribute("error")) {
            return new HashMap<String, Object>(model.asMap());
        }
        return null;
    }

    @RequestMapping(value = ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_ENTER, method = RequestMethod.GET)
    public String handleRoomEnter(
            SecurityToken securityToken,
            @PathVariable(value = "objectId") String objectId)
    {
        Executable executable = getExecutable(securityToken, objectId);
        RoomExecutable roomExecutable = getTargetRoomExecutableFromExecutable(securityToken, executable);
        Alias adobeConnectUrl = roomExecutable.getAliasByType(AliasType.ADOBE_CONNECT_URI);
        if (adobeConnectUrl == null) {
            throw new UnsupportedApiException(roomExecutable.getId());
        }
        return "redirect:" + adobeConnectUrl.getValue();
    }

    /**
     * Handle device command failed.
     */
    @ExceptionHandler(ControllerReportSet.DeviceCommandFailedException.class)
    public Object handleExceptions(Exception exception, HttpServletResponse response)
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return "errorRoomNotAvailable";
    }

    public RoomExecutable getTargetRoomExecutableFromExecutable(SecurityToken securityToken, Executable executable)
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
}
