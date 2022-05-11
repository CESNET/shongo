package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.error.LastOwnerRoleNotDeletableException;
import cz.cesnet.shongo.controller.rest.models.roles.UserRoleModel;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for roles endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1/reservation_requests/{id:.+}/roles")
public class UserRoleController {

    private final AuthorizationService authorizationService;
    private final Cache cache;

    public UserRoleController(@Autowired AuthorizationService reservationService, @Autowired Cache cache) {
        this.authorizationService = reservationService;
        this.cache = cache;
    }

    @Operation(summary = "Lists reservation request roles.")
    @GetMapping()
    ListResponse<UserRoleModel> listRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable(value = "id") String objectId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        AclEntryListRequest request = new AclEntryListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addObjectId(objectId);
        ListResponse<AclEntry> aclEntries = authorizationService.listAclEntries(request);

        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        List<UserRoleModel> items = aclEntries.getItems().stream().map(item -> new UserRoleModel(item, cacheProvider)).collect(Collectors.toList());
        return ListResponse.fromRequest(start, count, items);
    }

    @Operation(summary = "Creates new role for reservation request.")
    @PostMapping()
    void createRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable("id") String objectId,
            @RequestBody UserRoleModel userRoleModel)
    {
        String reservationRequestId = cache.getReservationRequestId(securityToken, objectId);
        userRoleModel.setObjectId(reservationRequestId);
        userRoleModel.setDeletable(true);
        authorizationService.createAclEntry(securityToken, userRoleModel.toApi());
    }

    @Operation(summary = "Deletes role for reservation request.")
    @DeleteMapping("/{entityId:.+}")
    void deleteRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable("id") String objectId,
            @PathVariable("entityId") String entityId)
    {
        String reservationRequestId = cache.getReservationRequestId(securityToken, objectId);
        AclEntryListRequest request = new AclEntryListRequest();
        request.setSecurityToken(securityToken);
        request.addObjectId(reservationRequestId);
        request.addRole(ObjectRole.OWNER);
        ListResponse<AclEntry> aclEntries = authorizationService.listAclEntries(request);
        if (aclEntries.getItemCount() == 1 && aclEntries.getItem(0).getId().equals(entityId)) {
            throw new LastOwnerRoleNotDeletableException();
        }
        authorizationService.deleteAclEntry(securityToken, entityId);
    }
}
