package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.MediaData;
import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.ExecutionReport;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.RoomCache;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for runtime endpoints.
 *
 * @author Filip Karnis
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reservation_requests/{id:.+}/runtime_management")
public class RuntimeController {

    private final Cache cache;
    private final RoomCache roomCache;
    private final ExecutableService executableService;

    public RuntimeController(
            @Autowired Cache cache,
            @Autowired RoomCache roomCache,
            @Autowired ExecutableService executableService)
    {
        this.cache = cache;
        this.executableService = executableService;
        this.roomCache = roomCache;
    }

    @Operation(summary = "Lists reservation request runtime participants.")
    @GetMapping("/participants")
    Map<String, Object> listRuntimeParticipants(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "DATETIME") String sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        List<RoomParticipant> roomParticipants = Collections.emptyList();
        try {
            roomParticipants = roomCache.getRoomParticipants(securityToken, executableId);
        }
        catch (Exception exception) {
            log.warn("Failed to load participants", exception);
        }
        ListResponse<RoomParticipant> response = ListResponse.fromRequest(start, count, roomParticipants);
        List<Map> items = new LinkedList<>();
        for (RoomParticipant roomParticipant : response.getItems()) {
            UserInformation user = null;
            String userId = roomParticipant.getUserId();
            if (userId != null) {
                user = cacheProvider.getUserInformation(userId);
            }
            Alias alias = roomParticipant.getAlias();
            Map<String, Object> item = new HashMap<>();
            item.put("id", roomParticipant.getId());
            item.put("name", (user != null ? user.getFullName() : roomParticipant.getDisplayName()));
            item.put("alias", (alias != null ? alias.getValue() : null));
            item.put("role", roomParticipant.getRole());
            item.put("email", (user != null ? user.getPrimaryEmail() : null));
            item.put("layout", roomParticipant.getLayout());
            item.put("microphoneEnabled", roomParticipant.getMicrophoneEnabled());
            item.put("microphoneLevel", roomParticipant.getMicrophoneLevel());
            item.put("videoEnabled", roomParticipant.getVideoEnabled());
            item.put("videoSnapshot", roomParticipant.isVideoSnapshot());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }

    @Operation(summary = "Takes snapshot of reservation request runtime participant.")
    @PostMapping("/participants/{participantId:.+}/snapshot")
    ResponseEntity<byte[]> snapshotRuntimeParticipant(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String participantId)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        try {
            MediaData participantSnapshot = roomCache.getRoomParticipantSnapshot(
                    securityToken, executableId, participantId);
            if (participantSnapshot != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(participantSnapshot.getType().toString()));
                headers.setCacheControl("no-cache, no-store, must-revalidate");
                headers.setPragma("no-cache");
                return new ResponseEntity<>(participantSnapshot.getData(), headers, HttpStatus.OK);
            }
        }
        catch (Exception exception) {
            log.warn("Failed to get participant snapshot", exception);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Modifies reservation request runtime participant.")
    @PatchMapping("/participants/{participantId:.+}")
    void modifyRuntimeParticipant(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String participantId,
            @RequestBody Map<String, Object> body)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        RoomParticipant oldRoomParticipant = null;
        if (!participantId.equals("*")) {
            oldRoomParticipant = roomCache.getRoomParticipant(securityToken, executableId, participantId);
        }

        // Parse body
        String name = (String) body.get("name");
        Boolean microphoneEnabled = (Boolean) body.get("microphoneEnabled");
        Integer microphoneLevel = (Integer) body.get("microphoneLevel");
        Boolean videoEnabled = (Boolean) body.get("videoEnabled");

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
    }

    @Operation(summary = "Disconnects reservation request runtime participant.")
    @PostMapping("/participants/{participantId:.+}/disconnect")
    void disconnectRuntimeParticipant(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String participantId)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        roomCache.disconnectRoomParticipant(securityToken, executableId, participantId);
    }

    @Operation(summary = "Starts recording of reservation request runtime.")
    @PostMapping("/recording/start")
    void startRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Object result = null;
        try {
            result = executableService.activateExecutableService(securityToken, executableId, executableServiceId);
        }
        catch (Exception exception) {
            log.warn("Start recording failed", exception);
        }
        if (Boolean.TRUE.equals(result) || Boolean.FALSE.equals(result)) {
            cache.clearExecutable(executableId);
        }
        else {
//            Locale locale = userSession.getLocale();
            String errorCode = "startingFailed";
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
//                log.warn("Start recording failed: {}", executionReport.toString(locale, userSession.getTimeZone()));
                log.warn("Start recording failed: {}", executionReport);

                // Detect further error
                ExecutionReport.UserError userError = executionReport.toUserError();
                if (userError instanceof ExecutionReport.RecordingUnavailable) {
                    errorCode = "unavailable";
                }
            }
            throw new RuntimeException("Starting recording failed with error: " + errorCode);
        }
    }

    @Operation(summary = "Stops recording of reservation request runtime.")
    @PostMapping("/recording/stop")
    void stopRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(value = "executableId") String executableId,
            @RequestParam(value = "executableServiceId") String executableServiceId)
    {
        Object result = null;
        try {
            result = executableService.deactivateExecutableService(securityToken, executableId, executableServiceId);
        }
        catch (Exception exception) {
            log.warn("Stop recording failed", exception);
        }
        if (Boolean.TRUE.equals(result)) {
            cache.clearExecutable(executableId);
        }
        else {
//            Locale locale = userSession.getLocale();
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
//                log.warn("Stop recording failed: {}", executionReport.toString(locale, userSession.getTimeZone()));
            }
        }
        cache.clearExecutable(executableId);
    }
}
