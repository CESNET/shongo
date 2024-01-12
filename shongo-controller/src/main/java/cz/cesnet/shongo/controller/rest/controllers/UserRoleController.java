package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import cz.cesnet.shongo.controller.rest.error.LastOwnerRoleNotDeletableException;
import cz.cesnet.shongo.controller.rest.models.roles.UserRoleModel;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for roles endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping(RestApiPath.ROLES)
@RequiredArgsConstructor
public class UserRoleController
{

    private final AuthorizationService authorizationService;
    private final Cache cache;

    @Operation(summary = "Lists reservation request roles.")
    @GetMapping
    ListResponse<UserRoleModel> listRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        AclEntryListRequest request = new AclEntryListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addObjectId(id);
        ListResponse<AclEntry> aclEntries = authorizationService.listAclEntries(request);

        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        List<UserRoleModel> items = aclEntries.getItems()
                .stream()
                .map(item -> new UserRoleModel(item, cacheProvider))
                .collect(Collectors.toList());
        return ListResponse.fromRequest(start, count, items);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates new role for reservation request.")
    @PostMapping
    void createRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestBody UserRoleModel userRoleModel)
    {
        String reservationRequestId = cache.getReservationRequestId(securityToken, id);
        userRoleModel.setObjectId(reservationRequestId);
        userRoleModel.setDeletable(true);
        authorizationService.createAclEntry(securityToken, userRoleModel.toApi());
    }

    @Operation(summary = "Deletes role for reservation request.")
    @DeleteMapping(RestApiPath.ENTITY_SUFFIX)
    void deleteRequestRoles(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @PathVariable String entityId)
    {
        String reservationRequestId = cache.getReservationRequestId(securityToken, id);
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
