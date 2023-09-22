package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.api.MediaData;
import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.controller.api.ExecutionReport;
import cz.cesnet.shongo.controller.api.RecordingService;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import cz.cesnet.shongo.controller.rest.RoomCache;
import cz.cesnet.shongo.controller.rest.error.UnsupportedApiException;
import cz.cesnet.shongo.controller.rest.models.runtimemanagement.RuntimeParticipantModel;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for runtime endpoints.
 *
 * @author Filip Karnis
 */
@Slf4j
@RestController
@RequestMapping(RestApiPath.RUNTIME_MANAGEMENT)
@RequiredArgsConstructor
public class RuntimeController
{

    private final Cache cache;
    private final RoomCache roomCache;
    private final ExecutableService executableService;

    @Operation(summary = "Lists reservation request runtime participants.")
    @GetMapping(RestApiPath.RUNTIME_MANAGEMENT_PARTICIPANTS)
    ListResponse<RuntimeParticipantModel> listRuntimeParticipants(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer count)
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

        List<RuntimeParticipantModel> items = roomParticipants
                .stream()
                .map(roomParticipant -> new RuntimeParticipantModel(roomParticipant, cacheProvider))
                .collect(Collectors.toList());
        return ListResponse.fromRequest(start, count, items);
    }

    @Operation(summary = "Takes snapshot of reservation request runtime participant.")
    @PostMapping(RestApiPath.RUNTIME_MANAGEMENT_PARTICIPANTS_SNAPSHOT)
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
    @PatchMapping(RestApiPath.RUNTIME_MANAGEMENT_PARTICIPANTS_MODIFY)
    void modifyRuntimeParticipant(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String participantId,
            @RequestBody RuntimeParticipantModel body)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        RoomParticipant oldRoomParticipant = null;
        if (!participantId.equals("*")) {
            oldRoomParticipant = roomCache.getRoomParticipant(securityToken, executableId, participantId);
        }

        // Parse body
        String name = body.getName();
        Boolean microphoneEnabled = body.getMicrophoneEnabled();
        Integer microphoneLevel = body.getMicrophoneLevel();
        Boolean videoEnabled = body.getVideoEnabled();

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
    @PostMapping(RestApiPath.RUNTIME_MANAGEMENT_PARTICIPANTS_DISCONNECT)
    void disconnectRuntimeParticipant(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String participantId)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        roomCache.disconnectRoomParticipant(securityToken, executableId, participantId);
    }

    @Operation(summary = "Starts recording of reservation request runtime.")
    @PostMapping(RestApiPath.RUNTIME_MANAGEMENT_RECORDING_START)
    void startRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        String executableServiceId = getExecutableServiceId(securityToken, executableId);

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
            String errorCode = "startingFailed";
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
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
    @PostMapping(RestApiPath.RUNTIME_MANAGEMENT_RECORDING_STOP)
    void stopRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        String executableServiceId = getExecutableServiceId(securityToken, executableId);

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
            if (result instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) result;
                log.warn("Stop recording failed: {}", executionReport);
            }
        }
        cache.clearExecutable(executableId);
    }

    private String getExecutableServiceId(SecurityToken securityToken, String executableId)
    {
        ExecutableServiceListRequest request = new ExecutableServiceListRequest(securityToken, executableId, RecordingService.class);
        List<cz.cesnet.shongo.controller.api.ExecutableService> services = executableService.listExecutableServices(request).getItems();
        log.debug("Found recording services: {}", services);
        if (services.size() > 1) {
            throw new UnsupportedApiException("Room " + executableId + " has multiple recording services.");
        }
        if (!services.isEmpty()) {
            return services.get(0).getId();
        }
        return null;
    }
}
