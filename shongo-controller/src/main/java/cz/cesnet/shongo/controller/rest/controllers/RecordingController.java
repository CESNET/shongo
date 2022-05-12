package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.api.ResourceRecording;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.ClientWebUrl;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;
import io.swagger.v3.oas.annotations.Operation;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for recording endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping(ClientWebUrl.RECORDINGS)
public class RecordingController {

    private final Cache cache;
    private final ExecutableService executableService;
    private final ResourceControlService resourceControlService;

    public RecordingController(
            @Autowired Cache cache,
            @Autowired ExecutableService executableService,
            @Autowired ResourceControlService resourceControlService)
    {
        this.cache = cache;
        this.executableService = executableService;
        this.resourceControlService = resourceControlService;
    }

    @Operation(summary = "Lists reservation request recordings.")
    @GetMapping
    Map<String, Object> listRequestRecordings(
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
        List<Map> items = new LinkedList<>();
        for (ResourceRecording recording : response.getItems()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", recording.getId());
            item.put("resourceId", recording.getResourceId());
            item.put("name", recording.getName());
            item.put("description", recording.getDescription());
            item.put("beginDate", recording.getBeginDate());
            Duration duration = recording.getDuration();
            if (duration == null || duration.isShorterThan(Duration.standardMinutes(1))) {
                item.put("duration", null);
            }
            else {
                item.put("duration", duration.toPeriod());
            }
            item.put("isPublic",recording.isPublic());
            item.put("downloadUrl", recording.getDownloadUrl());
            item.put("viewUrl", recording.getViewUrl());
            item.put("editUrl", recording.getEditUrl());
            item.put("filename",recording.getFileName());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);

        return data;
    }

    @Operation(summary = "Deletes recording from reservation request.")
    @DeleteMapping(ClientWebUrl.RECORDINGS_ID_SUFFIX)
    void deleteRequestRecording(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String recordingId)
    {
        resourceControlService.deleteRecording(securityToken, id, recordingId);
    }
}
