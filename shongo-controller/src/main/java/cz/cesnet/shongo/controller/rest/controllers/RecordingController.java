package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.api.RecordingService;
import cz.cesnet.shongo.controller.api.ResourceRecording;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import cz.cesnet.shongo.controller.rest.RestCache;
import cz.cesnet.shongo.controller.rest.models.recording.RecordingModel;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for recording endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping(RestApiPath.RECORDINGS)
@RequiredArgsConstructor
public class RecordingController
{

    private final RestCache cache;
    private final ExecutableService executableService;
    private final ResourceControlService resourceControlService;

    @Operation(summary = "Lists reservation request recordings.")
    @GetMapping
    ListResponse<RecordingModel> listRequestRecordings(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "START")
            ExecutableRecordingListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending)
            throws SchedulerReportSet.RoomExecutableNotExistsException
    {
        String executableId = cache.getExecutableId(securityToken, id);
        if (executableId == null) {
            throw new SchedulerReportSet.RoomExecutableNotExistsException();
        }
        ExecutableRecordingListRequest request = new ExecutableRecordingListRequest();
        request.setSecurityToken(securityToken);
        request.setExecutableId(executableId);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);

        ListResponse<ResourceRecording> response = executableService.listExecutableRecordings(request);

        List<RecordingModel> items = response.getItems().stream().map(RecordingModel::new).collect(Collectors.toList());
        return ListResponse.fromRequest(response.getStart(), response.getCount(), items);
    }

    @Operation(summary = "Deletes recording from reservation request.")
    @DeleteMapping(RestApiPath.RECORDINGS_ID_SUFFIX)
    void deleteRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String recordingId)
    {
        String executableId = cache.getExecutableId(securityToken, id);
        ExecutableServiceListRequest request = new ExecutableServiceListRequest(securityToken, executableId, RecordingService.class);
        List<cz.cesnet.shongo.controller.api.ExecutableService> executableServices = executableService.listExecutableServices(request).getItems();
        if (executableServices.isEmpty()) {
            throw new IllegalArgumentException("No recording service found for executable " + executableId);
        }
        String resId = ((RecordingService) executableServices.get(0)).getResourceId();
        resourceControlService.deleteRecording(securityToken, resId, recordingId);
    }
}
