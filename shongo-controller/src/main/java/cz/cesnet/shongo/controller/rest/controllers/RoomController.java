package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.ClientWebUrl;
import cz.cesnet.shongo.controller.rest.models.room.RoomAuthorizedData;
import cz.cesnet.shongo.controller.rest.models.room.RoomModel;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for room endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping(ClientWebUrl.ROOMS)
public class RoomController {

    private final Cache cache;
    private final ExecutableService executableService;

    public RoomController(@Autowired Cache cache, @Autowired ExecutableService executableService) {
        this.cache = cache;
        this.executableService = executableService;
    }

    @Operation(summary = "Lists rooms (executables).")
    @GetMapping
    public ListResponse<RoomModel> listRooms(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
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
        ListResponse<RoomModel> listResponse = new ListResponse<>();
        listResponse.addAll(response.getItems().stream().map(RoomModel::new).collect(Collectors.toList()));
        listResponse.setStart(response.getStart());
        listResponse.setCount(response.getCount());
        return listResponse;
    }

    @Operation(summary = "Gets room's (executable's) authorized data.")
    @GetMapping(ClientWebUrl.ID_SUFFIX)
    public RoomAuthorizedData getRoom(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        String roomId = cache.getExecutableId(securityToken, id);
        AbstractRoomExecutable roomExecutable =
                (AbstractRoomExecutable) executableService.getExecutable(securityToken, roomId);

        return new RoomAuthorizedData(roomExecutable);
    }
}
